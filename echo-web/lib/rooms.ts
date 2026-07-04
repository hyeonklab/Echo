import { ensureAccessToken } from "@/lib/auth";

const API_URL = process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080";

export type RoomType = "GROUP" | "DM" | "SELF";

export type RoomMember = {
  userId: number;
  displayName: string;
  email: string | null;
  provider: "LOCAL" | "GOOGLE" | "NAVER";
};

export type Room = {
  id: number;
  name: string;
  type: RoomType;
  createdByUserId: number;
  createdAt: string;
  members: RoomMember[];
};

/**
 * 채팅방 유형 라벨을 반환한다.
 */
export function getRoomTypeLabel(type: RoomType): string {
  if (type === "SELF") {
    return "나와의 대화";
  }

  if (type === "DM") {
    return "1:1 DM";
  }

  return "그룹";
}

/**
 * 인증 헤더를 구성한다.
 */
function authHeaders(token: string): HeadersInit {
  return {
    Authorization: `Bearer ${token}`,
    "Content-Type": "application/json",
  };
}

/**
 * API 호출용 access token을 확보한다.
 */
async function resolveAccessToken(): Promise<string | null> {
  return ensureAccessToken();
}

/**
 * 내 채팅방 목록을 조회한다.
 */
export async function fetchRooms(): Promise<Room[]> {
  const token = await resolveAccessToken();

  if (!token) {
    return [];
  }

  const response = await fetch(`${API_URL}/api/rooms`, {
    headers: authHeaders(token),
    cache: "no-store",
  });

  if (!response.ok) {
    return [];
  }

  return response.json() as Promise<Room[]>;
}

/**
 * 채팅방 상세 정보를 조회한다.
 */
export async function fetchRoom(roomId: number): Promise<Room | null> {
  const token = await resolveAccessToken();

  if (!token) {
    return null;
  }

  const response = await fetch(`${API_URL}/api/rooms/${roomId}`, {
    headers: authHeaders(token),
    cache: "no-store",
  });

  if (!response.ok) {
    return null;
  }

  return response.json() as Promise<Room>;
}

/**
 * 그룹 채팅방을 생성한다.
 */
export async function createGroupRoom(
  name: string,
  memberUserIds: number[] = [],
): Promise<Room | null> {
  const token = await resolveAccessToken();

  if (!token) {
    return null;
  }

  const response = await fetch(`${API_URL}/api/rooms`, {
    method: "POST",
    headers: authHeaders(token),
    body: JSON.stringify({ name, memberUserIds }),
    cache: "no-store",
  });

  if (!response.ok) {
    return null;
  }

  return response.json() as Promise<Room>;
}

/**
 * API 오류 메시지를 읽는다.
 */
async function readApiErrorMessage(response: Response): Promise<string | null> {
  try {
    const body = (await response.json()) as { message?: string };

    if (body.message) {
      return body.message;
    }
  } catch {
    // ignore parse errors
  }

  return null;
}

/**
 * 1:1 DM 채팅방을 조회하거나 생성한다.
 */
export async function createDmRoom(
  targetUserId: number,
): Promise<{ room: Room | null; errorMessage: string | null }> {
  const token = await resolveAccessToken();

  if (!token) {
    return { room: null, errorMessage: "Authentication required" };
  }

  const response = await fetch(`${API_URL}/api/rooms/dm`, {
    method: "POST",
    headers: authHeaders(token),
    body: JSON.stringify({ targetUserId }),
    cache: "no-store",
  });

  if (!response.ok) {
    return { room: null, errorMessage: await readApiErrorMessage(response) };
  }

  return {
    room: (await response.json()) as Room,
    errorMessage: null,
  };
}

/**
 * 채팅방에 멤버를 초대한다.
 */
export async function inviteRoomMember(roomId: number, userId: number): Promise<Room | null> {
  const token = await resolveAccessToken();

  if (!token) {
    return null;
  }

  const response = await fetch(`${API_URL}/api/rooms/${roomId}/members`, {
    method: "POST",
    headers: authHeaders(token),
    body: JSON.stringify({ userId }),
    cache: "no-store",
  });

  if (!response.ok) {
    return null;
  }

  return response.json() as Promise<Room>;
}

/**
 * 채팅방을 삭제하거나 참여를 종료한다.
 */
export async function deleteRoom(roomId: number): Promise<boolean> {
  const token = await resolveAccessToken();

  if (!token) {
    return false;
  }

  const response = await fetch(`${API_URL}/api/rooms/${roomId}`, {
    method: "DELETE",
    headers: authHeaders(token),
    cache: "no-store",
  });

  return response.ok;
}
