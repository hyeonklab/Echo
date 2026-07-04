/**
 * 채팅방 목록 빈 화면.
 */
export default function ChatPage() {
  return (
    <div className="flex h-full flex-col items-center justify-center bg-zinc-50 px-6 dark:bg-zinc-950">
      <p className="text-sm font-medium text-zinc-500">채팅방을 선택하세요</p>
      <p className="mt-2 text-xs text-zinc-400">좌측 목록에서 대화를 시작할 수 있습니다.</p>
    </div>
  );
}
