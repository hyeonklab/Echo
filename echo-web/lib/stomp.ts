import { Client, type StompSubscription } from "@stomp/stompjs";

import { getWsUrl } from "@/lib/api";
import { ensureAccessToken } from "@/lib/auth";
import type { Message, MessageDeletedEvent } from "@/lib/messages";
import type { RoomReadEvent } from "@/lib/room-live";
import type { PresenceUpdate } from "@/lib/presence";
import type { Room } from "@/lib/rooms";

export type RoomMessageHandler = (message: Message) => void;
export type RoomMessageDeletedHandler = (deleted: MessageDeletedEvent) => void;
export type RoomReadHandler = (read: RoomReadEvent) => void;
export type RoomMetaHandler = (update: RoomMetaUpdate) => void;
export type PresenceHandler = (update: PresenceUpdate) => void;
export type UserRoomMembershipHandler = (room: Room) => void;

export type RoomMetaUpdate = {
  roomId: number;
  name: string;
};

type DestinationHandler = (body: string) => void;

let sharedClient: Client | null = null;
let activationPromise: Promise<void> | null = null;
const handlersByDestination = new Map<string, Set<DestinationHandler>>();
const stompSubscriptions = new Map<string, StompSubscription>();

/**
 * 공유 STOMP 클라이언트를 활성화한다.
 */
async function activateSharedClient(): Promise<Client> {
  if (sharedClient?.connected) {
    return sharedClient;
  }

  if (!activationPromise) {
    activationPromise = new Promise<void>((resolve, reject) => {
      void (async () => {
        const token = await ensureAccessToken();

        if (!token) {
          activationPromise = null;
          reject(new Error("Authentication required"));
          return;
        }

        if (!sharedClient) {
          sharedClient = new Client({
            brokerURL: getWsUrl(),
            reconnectDelay: 5000,
            heartbeatIncoming: 10000,
            heartbeatOutgoing: 10000,
            beforeConnect: async () => {
              const freshToken = await ensureAccessToken();

              if (!freshToken) {
                throw new Error("Authentication required");
              }

              sharedClient!.connectHeaders = {
                Authorization: `Bearer ${freshToken}`,
              };
            },
            onConnect: () => {
              stompSubscriptions.clear();
              resubscribeAllDestinations();
              resolve();
            },
            onStompError: () => {
              activationPromise = null;
            },
            onWebSocketClose: () => {
              stompSubscriptions.clear();
              activationPromise = null;
            },
          });
        }

        sharedClient.connectHeaders = {
          Authorization: `Bearer ${token}`,
        };
        sharedClient.activate();
      })().catch((error: unknown) => {
        activationPromise = null;
        reject(error instanceof Error ? error : new Error("STOMP connection failed"));
      });
    });
  }

  await activationPromise;
  activationPromise = null;

  if (!sharedClient?.connected) {
    throw new Error("STOMP connection failed");
  }

  return sharedClient;
}

/**
 * 목적지별 STOMP 구독을 복원한다.
 */
function resubscribeAllDestinations(): void {
  if (!sharedClient?.connected) {
    return;
  }

  for (const destination of handlersByDestination.keys()) {
    resubscribeDestination(destination);
  }
}

/**
 * 단일 목적지 STOMP 구독을 연결한다.
 */
function resubscribeDestination(destination: string): void {
  if (!sharedClient?.connected || stompSubscriptions.has(destination)) {
    return;
  }

  const subscription = sharedClient.subscribe(destination, (frame) => {
    const handlers = handlersByDestination.get(destination);

    if (!handlers) {
      return;
    }

    for (const handler of handlers) {
      handler(frame.body);
    }
  });

  stompSubscriptions.set(destination, subscription);
}

/**
 * 단일 목적지 STOMP 구독을 해제한다.
 */
function unsubscribeDestination(destination: string): void {
  const subscription = stompSubscriptions.get(destination);

  if (subscription) {
    subscription.unsubscribe();
    stompSubscriptions.delete(destination);
  }
}

/**
 * STOMP 목적지 구독을 등록한다.
 */
function subscribeDestination(destination: string, handler: DestinationHandler): () => void {
  let handlers = handlersByDestination.get(destination);

  if (!handlers) {
    handlers = new Set();
    handlersByDestination.set(destination, handlers);
  }

  handlers.add(handler);

  void activateSharedClient()
    .then(() => {
      resubscribeDestination(destination);
    })
    .catch(() => {
      // 인증 만료 등 연결 실패는 상위 구독 해제 시 정리된다.
    });

  return () => {
    const currentHandlers = handlersByDestination.get(destination);

    if (!currentHandlers) {
      return;
    }

    currentHandlers.delete(handler);

    if (currentHandlers.size > 0) {
      return;
    }

    handlersByDestination.delete(destination);
    unsubscribeDestination(destination);
  };
}

/**
 * 여러 채팅방 메시지 STOMP 구독을 시작한다.
 */
export function subscribeRoomsMessages(roomIds: number[], onMessage: RoomMessageHandler): () => void {
  const uniqueRoomIds = [...new Set(roomIds)];

  if (uniqueRoomIds.length === 0) {
    return () => undefined;
  }

  const unsubscribes = uniqueRoomIds.map((roomId) =>
    subscribeDestination(`/topic/rooms/${roomId}/messages`, (body) => {
      onMessage(JSON.parse(body) as Message);
    }),
  );

  return () => {
    for (const unsubscribe of unsubscribes) {
      unsubscribe();
    }
  };
}

/**
 * 채팅방 메시지 STOMP 구독을 시작한다.
 */
export function subscribeRoomMessages(roomId: number, onMessage: RoomMessageHandler): () => void {
  return subscribeRoomsMessages([roomId], onMessage);
}

/**
 * 여러 채팅방 메시지 삭제 STOMP 구독을 시작한다.
 */
export function subscribeRoomsMessageDeletes(
  roomIds: number[],
  onDeleted: RoomMessageDeletedHandler,
): () => void {
  const uniqueRoomIds = [...new Set(roomIds)];

  if (uniqueRoomIds.length === 0) {
    return () => undefined;
  }

  const unsubscribes = uniqueRoomIds.map((roomId) =>
    subscribeDestination(`/topic/rooms/${roomId}/messages/deleted`, (body) => {
      onDeleted(JSON.parse(body) as MessageDeletedEvent);
    }),
  );

  return () => {
    for (const unsubscribe of unsubscribes) {
      unsubscribe();
    }
  };
}

/**
 * 채팅방 메시지 삭제 STOMP 구독을 시작한다.
 */
export function subscribeRoomMessageDeletes(
  roomId: number,
  onDeleted: RoomMessageDeletedHandler,
): () => void {
  return subscribeRoomsMessageDeletes([roomId], onDeleted);
}

/**
 * 여러 채팅방 읽음 상태 STOMP 구독을 시작한다.
 */
export function subscribeRoomsReads(roomIds: number[], onRead: RoomReadHandler): () => void {
  const uniqueRoomIds = [...new Set(roomIds)];

  if (uniqueRoomIds.length === 0) {
    return () => undefined;
  }

  const unsubscribes = uniqueRoomIds.map((roomId) =>
    subscribeDestination(`/topic/rooms/${roomId}/read`, (body) => {
      onRead(JSON.parse(body) as RoomReadEvent);
    }),
  );

  return () => {
    for (const unsubscribe of unsubscribes) {
      unsubscribe();
    }
  };
}

/**
 * 채팅방 읽음 상태 STOMP 구독을 시작한다.
 */
export function subscribeRoomRead(roomId: number, onRead: RoomReadHandler): () => void {
  return subscribeRoomsReads([roomId], onRead);
}

/**
 * 여러 채팅방 메타 정보 STOMP 구독을 시작한다.
 */
export function subscribeRoomsMeta(roomIds: number[], onMeta: RoomMetaHandler): () => void {
  const uniqueRoomIds = [...new Set(roomIds)];

  if (uniqueRoomIds.length === 0) {
    return () => undefined;
  }

  const unsubscribes = uniqueRoomIds.map((roomId) =>
    subscribeDestination(`/topic/rooms/${roomId}/meta`, (body) => {
      onMeta(JSON.parse(body) as RoomMetaUpdate);
    }),
  );

  return () => {
    for (const unsubscribe of unsubscribes) {
      unsubscribe();
    }
  };
}

/**
 * 사용자 채팅방 참여 변경 STOMP 구독을 시작한다.
 */
export function subscribeUserRoomMembership(
  userId: number,
  onRoomMembership: UserRoomMembershipHandler,
): () => void {
  return subscribeDestination(`/topic/users/${userId}/rooms`, (body) => {
    onRoomMembership(JSON.parse(body) as Room);
  });
}

/**
 * 사용자 온라인 상태 STOMP 구독을 시작한다.
 */
export function subscribePresenceUpdates(
  onPresence: PresenceHandler,
  onConnected?: () => void,
): () => void {
  const unsubscribe = subscribeDestination("/topic/presence", (body) => {
    onPresence(JSON.parse(body) as PresenceUpdate);
  });

  void activateSharedClient()
    .then(() => {
      onConnected?.();
    })
    .catch(() => {
      // 연결 실패 시 상위에서 재시도한다.
    });

  return unsubscribe;
}
