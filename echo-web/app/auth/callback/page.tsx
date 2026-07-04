"use client";

import Link from "next/link";
import { useRouter, useSearchParams } from "next/navigation";
import { Suspense, useEffect, useState } from "react";

import { exchangeAuthCode, setTokens } from "@/lib/auth";

type AuthCallbackErrorProps = {
  message: string;
};

/**
 * OAuth 콜백 오류 UI.
 */
function AuthCallbackError({ message }: Readonly<AuthCallbackErrorProps>) {
  return (
    <div className="space-y-4 text-center">
      <p className="text-sm text-red-600 dark:text-red-400">{message}</p>
      <Link
        href="/login"
        className="inline-flex items-center justify-center rounded-lg bg-zinc-900 px-4 py-2 text-sm font-medium text-white dark:bg-zinc-100 dark:text-zinc-900"
      >
        로그인으로 돌아가기
      </Link>
    </div>
  );
}

/**
 * OAuth 콜백에서 일회용 교환 코드를 JWT로 교환해 저장한다.
 */
function AuthCallbackContent() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const code = searchParams.get("code");
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!code) {
      return;
    }

    let cancelled = false;

    exchangeAuthCode(code).then((tokens) => {
      if (cancelled) {
        return;
      }

      if (!tokens) {
        setError("로그인 처리에 실패했습니다. 다시 로그인해 주세요.");
        return;
      }

      setTokens(tokens.accessToken, tokens.refreshToken);
      router.replace("/friends");
    });

    return () => {
      cancelled = true;
    };
  }, [router, code]);

  if (!code) {
    return <AuthCallbackError message="인증 코드가 없습니다. 다시 로그인해 주세요." />;
  }

  if (error) {
    return <AuthCallbackError message={error} />;
  }

  return <p className="text-sm text-zinc-500">로그인 처리 중...</p>;
}

/**
 * OAuth 인증 콜백 페이지.
 */
export default function AuthCallbackPage() {
  return (
    <main className="flex min-h-screen flex-col items-center justify-center bg-zinc-50 px-6 py-16 font-sans dark:bg-zinc-950">
      <div className="w-full max-w-md rounded-2xl border border-zinc-200 bg-white p-10 text-center shadow-sm dark:border-zinc-800 dark:bg-zinc-900">
        <Suspense fallback={<p className="text-sm text-zinc-500">로그인 처리 중...</p>}>
          <AuthCallbackContent />
        </Suspense>
      </div>
    </main>
  );
}
