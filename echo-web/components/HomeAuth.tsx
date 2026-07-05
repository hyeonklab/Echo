"use client";

import Link from "next/link";
import { type ChangeEvent, useEffect, useRef, useState } from "react";

import LoginPanel from "@/components/LoginPanel";
import UserAvatar from "@/components/UserAvatar";
import {
  AuthUser,
  fetchSessionUser,
  logout,
} from "@/lib/auth";
import { uploadAvatar } from "@/lib/files";

/**
 * 홈 화면 인증 상태 표시.
 */
export default function HomeAuth() {
  const fileInputRef = useRef<HTMLInputElement>(null);
  const [user, setUser] = useState<AuthUser | null>(null);
  const [loading, setLoading] = useState(true);
  const [uploadingAvatar, setUploadingAvatar] = useState(false);
  const [avatarErrorMessage, setAvatarErrorMessage] = useState<string | null>(null);

  useEffect(() => {
    async function loadUser() {
      try {
        const currentUser = await fetchSessionUser();

        if (!currentUser) {
          setUser(null);
          return;
        }

        setUser(currentUser);
      } catch {
        setUser(null);
      } finally {
        setLoading(false);
      }
    }

    void loadUser();
  }, []);

  async function handleLogout() {
    await logout();
    setUser(null);
  }

  async function handleAvatarChange(event: ChangeEvent<HTMLInputElement>) {
    const file = event.target.files?.[0];

    event.target.value = "";

    if (!file) {
      return;
    }

    setUploadingAvatar(true);
    setAvatarErrorMessage(null);

    const updatedUser = await uploadAvatar(file);

    setUploadingAvatar(false);

    if (!updatedUser) {
      setAvatarErrorMessage("프로필 사진 업로드에 실패했습니다.");
      return;
    }

    setUser(updatedUser);
  }

  if (loading) {
    return (
      <p className="mt-6 text-sm text-zinc-500">인증 상태 확인 중...</p>
    );
  }

  if (!user) {
    return <LoginPanel />;
  }

  return (
    <div className="mt-6 max-w-md space-y-4 rounded-xl border border-zinc-200 bg-zinc-50 p-5 dark:border-zinc-700 dark:bg-zinc-800/50">
      <p className="text-sm font-medium text-emerald-600 dark:text-emerald-400">
        로그인됨
      </p>

      <div className="flex items-center gap-4">
        <UserAvatar
          displayName={user.displayName}
          avatarFileId={user.avatarFileId}
          className="h-16 w-16"
          textClassName="text-lg font-semibold"
        />
        <div>
          <p className="text-sm font-medium text-zinc-900 dark:text-zinc-100">{user.displayName}</p>
          <button
            type="button"
            onClick={() => fileInputRef.current?.click()}
            disabled={uploadingAvatar}
            className="mt-2 rounded-lg border border-zinc-300 px-3 py-1.5 text-xs font-medium text-zinc-700 transition hover:bg-zinc-100 disabled:opacity-50 dark:border-zinc-600 dark:text-zinc-200 dark:hover:bg-zinc-800"
          >
            {uploadingAvatar ? "업로드 중..." : "프로필 사진 변경"}
          </button>
          <input
            ref={fileInputRef}
            type="file"
            accept="image/jpeg,image/png,image/gif,image/webp"
            className="hidden"
            onChange={(event) => void handleAvatarChange(event)}
          />
        </div>
      </div>

      {avatarErrorMessage ? (
        <p className="text-sm text-red-600 dark:text-red-400">{avatarErrorMessage}</p>
      ) : null}

      <dl className="space-y-2.5 text-sm">
        <div className="flex items-center gap-4">
          <dt className="w-14 shrink-0 text-zinc-500">이메일</dt>
          <dd className="min-w-0 break-all font-medium text-zinc-900 dark:text-zinc-100">{user.email ?? "-"}</dd>
        </div>
        <div className="flex items-center gap-4">
          <dt className="w-14 shrink-0 text-zinc-500">제공자</dt>
          <dd className="font-medium text-zinc-900 dark:text-zinc-100">{user.provider}</dd>
        </div>
      </dl>
      <div className="flex flex-wrap gap-3 pt-2">
        <Link
          href="/friends"
          className="inline-flex items-center justify-center rounded-lg bg-zinc-900 px-4 py-2 text-sm font-medium text-white transition hover:bg-zinc-700 dark:bg-zinc-100 dark:text-zinc-900 dark:hover:bg-zinc-300"
        >
          친구 목록으로 이동
        </Link>
        <Link
          href="/chat"
          className="inline-flex items-center justify-center rounded-lg border border-zinc-300 px-4 py-2 text-sm font-medium text-zinc-700 transition hover:bg-zinc-100 dark:border-zinc-600 dark:text-zinc-200 dark:hover:bg-zinc-800"
        >
          채팅으로 이동
        </Link>
        <button
          type="button"
          onClick={handleLogout}
          className="inline-flex items-center justify-center rounded-lg border border-zinc-300 px-4 py-2 text-sm font-medium text-zinc-700 transition hover:bg-zinc-100 dark:border-zinc-600 dark:text-zinc-200 dark:hover:bg-zinc-800"
        >
          로그아웃
        </button>
      </div>
    </div>
  );
}
