"use client";

import { useEffect } from "react";
import { useParams, useRouter } from "next/navigation";

import ChatRoomView from "@/components/ChatRoomView";

/**
 * 채팅방 상세 페이지.
 */
export default function ChatRoomPage() {
  const router = useRouter();
  const params = useParams();
  const roomId = Number(params.roomId);
  const isValidRoomId = Number.isInteger(roomId) && roomId > 0;

  useEffect(() => {
    if (!isValidRoomId) {
      router.replace("/chat");
    }
  }, [isValidRoomId, router]);

  if (!isValidRoomId) {
    return null;
  }

  return (
    <div className="flex h-full flex-col overflow-hidden">
      <ChatRoomView roomId={roomId} />
    </div>
  );
}
