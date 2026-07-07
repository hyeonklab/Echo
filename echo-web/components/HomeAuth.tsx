"use client";

import Link from "next/link";
import { type ChangeEvent, type SubmitEvent, useEffect, useRef, useState } from "react";

import LoginPanel from "@/components/LoginPanel";
import ThemeToggle from "@/components/ThemeToggle";
import UserAvatar from "@/components/UserAvatar";
import {
  AuthUser,
  fetchSessionUser,
  logout,
} from "@/lib/auth";
import { resetAvatar, uploadAvatar } from "@/lib/files";
import { updateDisplayName } from "@/lib/users";

/**
 * 홈 화면 인증 상태 표시.
 */
export default function HomeAuth() {
  const fileInputRef = useRef<HTMLInputElement>(null);
  const [user, setUser] = useState<AuthUser | null>(null);
  const [loading, setLoading] = useState(true);
  const [uploadingAvatar, setUploadingAvatar] = useState(false);
  const [resettingAvatar, setResettingAvatar] = useState(false);
  const [avatarErrorMessage, setAvatarErrorMessage] = useState<string | null>(null);
  const [isRenaming, setIsRenaming] = useState(false);
  const [renameInput, setRenameInput] = useState("");
  const [renamingSubmitting, setRenamingSubmitting] = useState(false);
  const [renameErrorMessage, setRenameErrorMessage] = useState<string | null>(null);

  useEffect(() => {
    async function loadUser() {
      try {
        const currentUser = await fetchSessionUser();

        if (!currentUser) {
          setUser(null);
          return;
        }

        setUser(currentUser);
        setRenameInput(currentUser.displayName);
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

  async function handleResetAvatar() {
    if (!user?.avatarFileId || resettingAvatar || uploadingAvatar) {
      return;
    }

    setResettingAvatar(true);
    setAvatarErrorMessage(null);

    const updatedUser = await resetAvatar();

    setResettingAvatar(false);

    if (!updatedUser) {
      setAvatarErrorMessage("기본 프로필로 되돌리지 못했습니다.");
      return;
    }

    setUser(updatedUser);
  }

  function handleStartRename() {
    if (!user) {
      return;
    }

    setRenameInput(user.displayName);
    setRenameErrorMessage(null);
    setIsRenaming(true);
  }

  function handleCancelRename() {
    setRenameInput(user?.displayName ?? "");
    setRenameErrorMessage(null);
    setIsRenaming(false);
  }

  async function handleSubmitRename(event: SubmitEvent<HTMLFormElement>) {
    event.preventDefault();

    if (!user) {
      return;
    }

    const trimmedName = renameInput.trim();

    if (!trimmedName) {
      setRenameErrorMessage("이름을 입력해 주세요.");
      return;
    }

    if (trimmedName === user.displayName) {
      setIsRenaming(false);
      setRenameErrorMessage(null);
      return;
    }

    setRenamingSubmitting(true);
    setRenameErrorMessage(null);

    const updatedUser = await updateDisplayName(trimmedName);

    setRenamingSubmitting(false);

    if (!updatedUser) {
      setRenameErrorMessage("이름 변경에 실패했습니다.");
      return;
    }

    setUser(updatedUser);
    setRenameInput(updatedUser.displayName);
    setIsRenaming(false);
  }

  if (loading) {
    return (
      <p className="mt-6 text-sm text-zinc-500">인증 상태 확인 중...</p>
    );
  }

  if (!user) {
    return <LoginPanel />;
  }

  const avatarBusy = uploadingAvatar || resettingAvatar;

  return (
    <div className="mt-6 max-w-md space-y-4 rounded-xl border border-zinc-200 bg-zinc-50 p-5 dark:border-zinc-700 dark:bg-zinc-800/50">
      <p className="text-sm font-medium text-emerald-600 dark:text-emerald-400">
        로그인됨
      </p>

      <div className="flex items-start gap-4">
        <UserAvatar
          displayName={user.displayName}
          avatarFileId={user.avatarFileId}
          className="h-16 w-16"
          textClassName="text-lg font-semibold"
        />
        <div className="min-w-0 flex-1 space-y-2">
          {isRenaming ? (
            <form className="space-y-2" onSubmit={handleSubmitRename}>
              <input
                type="text"
                value={renameInput}
                onChange={(event) => setRenameInput(event.target.value)}
                maxLength={255}
                className="w-full rounded-lg border border-zinc-300 bg-white px-3 py-2 text-sm dark:border-zinc-600 dark:bg-zinc-900"
                autoFocus
                disabled={renamingSubmitting}
              />
              <div className="flex flex-wrap gap-2">
                <button
                  type="submit"
                  disabled={renamingSubmitting}
                  className="rounded-lg bg-zinc-900 px-3 py-1.5 text-xs font-medium text-white disabled:opacity-50 dark:bg-zinc-100 dark:text-zinc-900"
                >
                  {renamingSubmitting ? "저장 중..." : "저장"}
                </button>
                <button
                  type="button"
                  onClick={handleCancelRename}
                  disabled={renamingSubmitting}
                  className="rounded-lg border border-zinc-300 px-3 py-1.5 text-xs font-medium text-zinc-700 disabled:opacity-50 dark:border-zinc-600 dark:text-zinc-200"
                >
                  취소
                </button>
              </div>
            </form>
          ) : (
            <div className="flex flex-wrap items-center gap-2">
              <p className="text-sm font-medium text-zinc-900 dark:text-zinc-100">{user.displayName}</p>
              <button
                type="button"
                onClick={handleStartRename}
                className="rounded-lg border border-zinc-300 px-2.5 py-1 text-xs font-medium text-zinc-700 transition hover:bg-zinc-100 dark:border-zinc-600 dark:text-zinc-200 dark:hover:bg-zinc-800"
              >
                이름 변경
              </button>
            </div>
          )}

          {renameErrorMessage ? (
            <p className="text-xs text-red-600 dark:text-red-400">{renameErrorMessage}</p>
          ) : null}

          <div className="flex flex-wrap gap-2">
            <button
              type="button"
              onClick={() => fileInputRef.current?.click()}
              disabled={avatarBusy}
              className="rounded-lg border border-zinc-300 px-3 py-1.5 text-xs font-medium text-zinc-700 transition hover:bg-zinc-100 disabled:opacity-50 dark:border-zinc-600 dark:text-zinc-200 dark:hover:bg-zinc-800"
            >
              {uploadingAvatar ? "업로드 중..." : "프로필 사진 변경"}
            </button>
            {user.avatarFileId ? (
              <button
                type="button"
                onClick={() => void handleResetAvatar()}
                disabled={avatarBusy}
                className="rounded-lg border border-zinc-300 px-3 py-1.5 text-xs font-medium text-zinc-700 transition hover:bg-zinc-100 disabled:opacity-50 dark:border-zinc-600 dark:text-zinc-200 dark:hover:bg-zinc-800"
              >
                {resettingAvatar ? "처리 중..." : "기본 프로필로"}
              </button>
            ) : null}
          </div>
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
        <div className="flex items-center gap-4">
          <dt className="w-14 shrink-0 text-zinc-500">테마</dt>
          <dd>
            <ThemeToggle />
          </dd>
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
