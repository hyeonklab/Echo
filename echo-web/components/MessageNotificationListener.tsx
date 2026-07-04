"use client";

import { useCallback, useEffect, useRef, useState } from "react";
import { usePathname, useRouter } from "next/navigation";

import { AuthUser, fetchSessionUser, getAccessToken } from "@/lib/auth";
import type { Message } from "@/lib/messages";
import {
  formatNotificationBody,
  formatNotificationPreview,
  requestNotificationPermission,
  shouldNotifyMessage,
  showMessageNotification,
} from "@/lib/notifications";
import { Room, fetchRooms, getRoomDisplayName } from "@/lib/rooms";
import { publishRoomMessageEvent } from "@/lib/room-live";
import { subscribeRoomsMessages } from "@/lib/stomp";

/**
 * 로그인 사용자의 모든 채팅방 메시지를 구독하고 브라우저 알림을 표시한다.
 */
export default function MessageNotificationListener() {
  const router = useRouter();
  const pathname = usePathname();
  const pathnameRef = useRef(pathname);
  const roomsRef = useRef<Room[]>([]);
  const currentUserRef = useRef<AuthUser | null>(null);
  const [rooms, setRooms] = useState<Room[]>([]);
  const [currentUser, setCurrentUser] = useState<AuthUser | null>(null);
  const [enabled, setEnabled] = useState(false);

  pathnameRef.current = pathname;
  roomsRef.current = rooms;
  currentUserRef.current = currentUser;

  const loadNotificationContext = useCallback(async () => {
    const token = getAccessToken();

    if (!token) {
      setCurrentUser(null);
      setRooms([]);
      setEnabled(false);
      return;
    }

    const user = await fetchSessionUser();

    if (!user) {
      setCurrentUser(null);
      setRooms([]);
      setEnabled(false);
      return;
    }

    const roomList = await fetchRooms();

    setCurrentUser(user);
    setRooms(roomList);
    setEnabled(true);
  }, []);

  useEffect(() => {
    void loadNotificationContext();
    void requestNotificationPermission();

    function handleVisibilityChange() {
      if (globalThis.document.visibilityState === "visible") {
        void loadNotificationContext();
      }
    }

    globalThis.window.addEventListener("focus", loadNotificationContext);
    globalThis.document.addEventListener("visibilitychange", handleVisibilityChange);

    return () => {
      globalThis.window.removeEventListener("focus", loadNotificationContext);
      globalThis.document.removeEventListener("visibilitychange", handleVisibilityChange);
    };
  }, [loadNotificationContext, pathname]);

  useEffect(() => {
    if (!enabled || !currentUser || rooms.length === 0) {
      return;
    }

    function handleIncomingMessage(message: Message) {
      publishRoomMessageEvent(message);

      const user = currentUserRef.current;

      if (!user) {
        return;
      }

      if (!shouldNotifyMessage(message.roomId, message.senderId, user.id, pathnameRef.current)) {
        return;
      }

      const room = roomsRef.current.find((item) => item.id === message.roomId);
      const roomTitle = room ? getRoomDisplayName(room, user.id) : "새 메시지";
      const senderLabel = message.senderDisplayName;
      const body =
        room?.type === "GROUP"
          ? formatNotificationBody(senderLabel, message.content)
          : formatNotificationPreview(message.content);

      showMessageNotification({
        title: roomTitle,
        body,
        roomId: message.roomId,
        onClick: () => {
          router.push(`/chat/${message.roomId}`);
        },
      });
    }

    const roomIds = rooms.map((room) => room.id);

    return subscribeRoomsMessages(roomIds, handleIncomingMessage);
  }, [currentUser, enabled, rooms, router]);

  return null;
}
