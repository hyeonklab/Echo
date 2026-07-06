import HomeAuth from "@/components/HomeAuth";
import AppLogo from "@/components/AppLogo";

/**
 * 앱 셸 우측 홈 패널.
 */
export default function ShellHomePage() {
  return (
    <div className="flex h-full flex-col overflow-hidden">
      <header className="shrink-0 border-b border-zinc-200 px-6 py-4 dark:border-zinc-800">
        <AppLogo size={32} showName href={null} />
        <h1 className="mt-2 text-lg font-semibold text-zinc-900 dark:text-zinc-50">홈</h1>
      </header>

      <div className="min-h-0 flex-1 overflow-y-auto px-6 py-6">
        <p className="text-sm leading-6 text-zinc-600 dark:text-zinc-400">
          Google 계정으로 로그인하고 실시간 채팅을 시작하세요.
        </p>
        <HomeAuth />
      </div>
    </div>
  );
}
