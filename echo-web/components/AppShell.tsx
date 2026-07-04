"use client";

import { usePathname } from "next/navigation";

import AppSidebar from "@/components/AppSidebar";
import ChatRoomList from "@/components/ChatRoomList";

type AppShellProps = {
  children: React.ReactNode;
};

/**
 * 현재 경로에서 활성 채팅방 ID를 반환한다.
 */
function resolveActiveRoomId(pathname: string): number | null {
  const match = pathname.match(/^\/chat\/(\d+)$/);

  if (!match) {
    return null;
  }

  const roomId = Number(match[1]);

  if (!Number.isInteger(roomId) || roomId <= 0) {
    return null;
  }

  return roomId;
}

/**
 * 카카오톡 스타일 앱 셸 레이아웃.
 */
export default function AppShell({ children }: AppShellProps) {
  const pathname = usePathname();
  const isChatSection = pathname === "/chat" || pathname.startsWith("/chat/");
  const activeRoomId = resolveActiveRoomId(pathname);

  return (
    <div className="flex h-screen overflow-hidden bg-zinc-100 dark:bg-zinc-950">
      <AppSidebar />

      {isChatSection ? (
        <aside className="flex w-80 shrink-0 flex-col overflow-hidden border-r border-zinc-200 bg-white dark:border-zinc-800 dark:bg-zinc-900">
          <ChatRoomList mode="panel" activeRoomId={activeRoomId} />
        </aside>
      ) : null}

      <main className="min-w-0 flex-1 overflow-hidden bg-white dark:bg-zinc-900">{children}</main>
    </div>
  );
}
