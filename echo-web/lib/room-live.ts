import type { Message } from "@/lib/messages";
import type { LastMessagePreview, Room } from "@/lib/rooms";

export const ROOM_MESSAGE_EVENT = "echo:room-message";

/**
 * 채팅방 활동 시각을 반환한다.
 */
function getRoomActivityTime(room: Room): string {
  return room.lastMessage?.createdAt ?? room.createdAt;
}

/**
 * 최근 메시지 기준으로 채팅방 목록을 정렬한다.
 */
export function sortRoomsByLastActivity(rooms: Room[]): Room[] {
  return [...rooms].sort((left, right) =>
    getRoomActivityTime(right).localeCompare(getRoomActivityTime(left)),
  );
}

/**
 * 수신 메시지로 채팅방 목록의 미리보기를 갱신한다.
 */
export function applyIncomingMessageToRooms(rooms: Room[], message: Message): Room[] {
  const targetRoom = rooms.find((room) => room.id === message.roomId);

  if (!targetRoom) {
    return rooms;
  }

  const lastMessage: LastMessagePreview = {
    senderId: message.senderId,
    senderDisplayName: message.senderDisplayName,
    content: message.content,
    createdAt: message.createdAt,
  };

  if (
    targetRoom.lastMessage?.createdAt === lastMessage.createdAt
    && targetRoom.lastMessage.content === lastMessage.content
    && targetRoom.lastMessage.senderId === lastMessage.senderId
  ) {
    return rooms;
  }

  const updated = rooms.map((room) => {
    if (room.id !== message.roomId) {
      return room;
    }

    return {
      ...room,
      lastMessage,
    };
  });

  return sortRoomsByLastActivity(updated);
}

/**
 * 채팅방 메시지 수신 이벤트를 발행한다.
 */
export function publishRoomMessageEvent(message: Message): void {
  if (globalThis.window === undefined) {
    return;
  }

  globalThis.window.dispatchEvent(
    new CustomEvent<Message>(ROOM_MESSAGE_EVENT, { detail: message }),
  );
}

/**
 * 채팅방 메시지 수신 이벤트를 구독한다.
 */
export function subscribeRoomMessageEvents(handler: (message: Message) => void): () => void {
  function onRoomMessage(event: Event) {
    handler((event as CustomEvent<Message>).detail);
  }

  globalThis.window.addEventListener(ROOM_MESSAGE_EVENT, onRoomMessage);

  return () => {
    globalThis.window.removeEventListener(ROOM_MESSAGE_EVENT, onRoomMessage);
  };
}
