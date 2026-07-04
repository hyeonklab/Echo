"use client";

import Link from "next/link";
import { type SubmitEvent, useEffect, useRef, useState } from "react";
import { useRouter } from "next/navigation";

import { AuthUser, fetchSessionUser } from "@/lib/auth";
import { Message, fetchMessages, sendMessage } from "@/lib/messages";
import { Room, fetchRoom, getRoomTypeLabel } from "@/lib/rooms";

type ChatRoomViewProps = {
  roomId: number;
};

/**
 * 메시지 시간을 표시 형식으로 변환한다.
 */
function formatMessageTime(value: string): string {
  return new Date(value).toLocaleString("ko-KR", {
    month: "short",
    day: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  });
}

/**
 * 채팅방 메시지 목록 및 입력 UI.
 */
export default function ChatRoomView({ roomId }: Readonly<ChatRoomViewProps>) {
  const router = useRouter();
  const messagesEndRef = useRef<HTMLDivElement>(null);
  const messageInputRef = useRef<HTMLInputElement>(null);
  const [room, setRoom] = useState<Room | null>(null);
  const [currentUser, setCurrentUser] = useState<AuthUser | null>(null);
  const [messages, setMessages] = useState<Message[]>([]);
  const [hasMore, setHasMore] = useState(false);
  const [loading, setLoading] = useState(true);
  const [loadingMore, setLoadingMore] = useState(false);
  const [messageInput, setMessageInput] = useState("");
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  useEffect(() => {
    async function loadChatRoom() {
      const user = await fetchSessionUser();

      if (!user) {
        router.replace("/login");
        return;
      }

      const [roomData, history] = await Promise.all([
        fetchRoom(roomId),
        fetchMessages(roomId),
      ]);

      if (!roomData || !history) {
        setErrorMessage("채팅방을 불러오지 못했습니다.");
        setLoading(false);
        return;
      }

      setRoom(roomData);
      setCurrentUser(user);
      setMessages(history.messages);
      setHasMore(history.hasMore);
      setLoading(false);
    }

    loadChatRoom();
  }, [roomId, router]);

  useEffect(() => {
    if (loading) {
      return;
    }

    async function pollMessages() {
      const history = await fetchMessages(roomId);

      if (!history || history.messages.length === 0) {
        return;
      }

      setMessages((prev) => {
        const existingIds = new Set(prev.map((item) => item.id));
        const merged = [...prev];

        for (const message of history.messages) {
          if (!existingIds.has(message.id)) {
            merged.push(message);
          }
        }

        merged.sort((a, b) => a.id - b.id);

        return merged;
      });
    }

    const intervalId = globalThis.setInterval(() => {
      void pollMessages();
    }, 4000);

    return () => {
      globalThis.clearInterval(intervalId);
    };
  }, [roomId, loading]);

  function focusMessageInput() {
    messageInputRef.current?.focus({ preventScroll: true });
  }

  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: "auto", block: "end" });
  }, [messages]);

  async function handleLoadMore() {
    if (loadingMore || !hasMore || messages.length === 0) {
      return;
    }

    setLoadingMore(true);
    setErrorMessage(null);

    const history = await fetchMessages(roomId, { before: messages[0].id });

    if (!history) {
      setErrorMessage("이전 메시지를 불러오지 못했습니다.");
      setLoadingMore(false);
      return;
    }

    setMessages((prev) => [...history.messages, ...prev]);
    setHasMore(history.hasMore);
    setLoadingMore(false);
  }

  function handleSendMessage(event: SubmitEvent<HTMLFormElement>) {
    event.preventDefault();

    const trimmed = messageInput.trim();

    if (!trimmed) {
      return;
    }

    const contentToSend = trimmed;

    setErrorMessage(null);
    setMessageInput("");
    focusMessageInput();

    void sendMessage(roomId, contentToSend).then((message) => {
      if (!message) {
        setErrorMessage("메시지 전송에 실패했습니다.");
        focusMessageInput();
        return;
      }

      setMessages((prev) => [...prev, message]);
      focusMessageInput();
    });
  }

  if (loading) {
    return <p className="text-sm text-zinc-500">채팅방 불러오는 중...</p>;
  }

  if (!room || !currentUser) {
    return (
      <div className="space-y-4">
        <p className="text-sm text-red-600 dark:text-red-400">
          {errorMessage ?? "채팅방을 찾을 수 없습니다."}
        </p>
        <Link
          href="/chat"
          className="inline-flex items-center justify-center rounded-lg border border-zinc-300 px-4 py-2 text-sm font-medium text-zinc-700 transition hover:bg-zinc-100 dark:border-zinc-600 dark:text-zinc-200 dark:hover:bg-zinc-800"
        >
          채팅방 목록으로
        </Link>
      </div>
    );
  }

  return (
    <div className="flex h-[70vh] min-h-[32rem] flex-col">
      <div className="flex items-start justify-between gap-4 border-b border-zinc-200 pb-4 dark:border-zinc-700">
        <div>
          <Link
            href="/chat"
            className="text-xs font-medium text-zinc-500 transition hover:text-zinc-700 dark:hover:text-zinc-300"
          >
            ← 채팅방 목록
          </Link>
          <h2 className="mt-2 text-xl font-semibold text-zinc-900 dark:text-zinc-50">{room.name}</h2>
          <p className="mt-1 text-xs text-zinc-500">
            {getRoomTypeLabel(room.type)} · {room.members.length}명
          </p>
        </div>
      </div>

      {errorMessage ? (
        <p className="mt-3 rounded-lg border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700 dark:border-red-900 dark:bg-red-950 dark:text-red-300">
          {errorMessage}
        </p>
      ) : null}

      <div className="mt-4 flex min-h-0 flex-1 flex-col rounded-xl border border-zinc-200 bg-zinc-50 dark:border-zinc-700 dark:bg-zinc-800/50">
        <div className="flex-1 space-y-3 overflow-y-auto p-4">
          {hasMore ? (
            <div className="flex justify-center">
              <button
                type="button"
                onClick={handleLoadMore}
                disabled={loadingMore}
                className="rounded-lg border border-zinc-300 px-3 py-1 text-xs font-medium text-zinc-600 transition hover:bg-zinc-100 disabled:opacity-50 dark:border-zinc-600 dark:text-zinc-300 dark:hover:bg-zinc-800"
              >
                {loadingMore ? "불러오는 중..." : "이전 메시지 불러오기"}
              </button>
            </div>
          ) : null}

          {messages.length === 0 ? (
            <p className="text-center text-sm text-zinc-500">아직 메시지가 없습니다. 첫 메시지를 보내 보세요.</p>
          ) : (
            messages.map((message) => {
              const isMine = message.senderId === currentUser.id;

              return (
                <div key={message.id} className={`flex ${isMine ? "justify-end" : "justify-start"}`}>
                  <div
                    className={`max-w-[80%] rounded-2xl px-3 py-2 ${
                      isMine
                        ? "bg-zinc-900 text-white dark:bg-zinc-100 dark:text-zinc-900"
                        : "border border-zinc-200 bg-white text-zinc-900 dark:border-zinc-600 dark:bg-zinc-900 dark:text-zinc-100"
                    }`}
                  >
                    {!isMine ? (
                      <p className="mb-1 text-xs font-medium text-zinc-500 dark:text-zinc-400">
                        {message.senderDisplayName}
                      </p>
                    ) : null}
                    <p className="whitespace-pre-wrap break-words text-sm">{message.content}</p>
                    <p
                      className={`mt-1 text-[11px] ${
                        isMine ? "text-zinc-300 dark:text-zinc-500" : "text-zinc-400"
                      }`}
                    >
                      {formatMessageTime(message.createdAt)}
                    </p>
                  </div>
                </div>
              );
            })
          )}
          <div ref={messagesEndRef} />
        </div>

        <form
          className="flex gap-2 border-t border-zinc-200 p-3 dark:border-zinc-700"
          onSubmit={handleSendMessage}
        >
          <input
            ref={messageInputRef}
            type="text"
            value={messageInput}
            onChange={(event) => setMessageInput(event.target.value)}
            placeholder="메시지를 입력하세요"
            className="flex-1 rounded-lg border border-zinc-300 bg-white px-3 py-2 text-sm dark:border-zinc-600 dark:bg-zinc-900"
            autoFocus
          />
          <button
            type="submit"
            disabled={!messageInput.trim()}
            className="rounded-lg bg-zinc-900 px-4 py-2 text-sm font-medium text-white disabled:opacity-50 dark:bg-zinc-100 dark:text-zinc-900"
          >
            전송
          </button>
        </form>
      </div>
    </div>
  );
}
