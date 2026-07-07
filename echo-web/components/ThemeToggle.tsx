"use client";

import { useEffect, useState } from "react";

import { useTheme } from "@/components/ThemeProvider";
import type { Theme } from "@/lib/theme";

type ThemeToggleProps = {
  variant?: "segmented" | "icon";
};

/**
 * 테마 토글 UI.
 */
export default function ThemeToggle({ variant = "segmented" }: Readonly<ThemeToggleProps>) {
  const { theme, setTheme } = useTheme();
  const [mounted, setMounted] = useState(false);

  useEffect(() => {
    setMounted(true);
  }, []);

  if (!mounted) {
    return (
      <div
        className={
          variant === "icon"
            ? "h-12 w-12 rounded-xl bg-zinc-200/60 dark:bg-zinc-800/60"
            : "h-9 w-[198px] rounded-lg bg-zinc-200/60 dark:bg-zinc-800/60"
        }
        aria-hidden="true"
      />
    );
  }

  function handleSelectTheme(nextTheme: Theme) {
    setTheme(nextTheme);
  }

  function handleCycleTheme() {
    if (theme === "dark") {
      setTheme("light");
      return;
    }

    if (theme === "light") {
      setTheme("system");
      return;
    }

    setTheme("dark");
  }

  function getIconLabel(): string {
    if (theme === "system") {
      return "시스템 설정 따름";
    }

    if (theme === "light") {
      return "라이트 모드";
    }

    return "다크 모드";
  }

  if (variant === "icon") {
    return (
      <button
        type="button"
        onClick={handleCycleTheme}
        aria-label={`테마: ${getIconLabel()}. 클릭하여 변경`}
        title={getIconLabel()}
        className="flex h-12 w-12 items-center justify-center rounded-xl text-zinc-500 transition hover:bg-white/70 hover:text-zinc-800 dark:hover:bg-zinc-800/70 dark:hover:text-zinc-200"
      >
        {theme === "system" ? (
          <svg viewBox="0 0 24 24" className="h-6 w-6" fill="currentColor" aria-hidden="true">
            <path d="M20 3H4c-1.1 0-2 .9-2 2v11c0 1.1.9 2 2 2h3v2h.5c.28 0 .5.22.5.5s-.22.5-.5.5h-3c-.83 0-1.5-.67-1.5-1.5v-2H4c-1.65 0-3-1.35-3-3V5c0-1.65 1.35-3 3-3h16c1.65 0 3 1.35 3 3v11c0 1.65-1.35 3-3 3h-3v2h.5c.28 0 .5.22.5.5s-.22.5-.5.5H9.5c-.28 0-.5-.22-.5-.5s.22-.5.5-.5H13v-2H4V5h16v11z" />
          </svg>
        ) : theme === "dark" ? (
          <svg viewBox="0 0 24 24" className="h-6 w-6" fill="currentColor" aria-hidden="true">
            <path d="M12 7c-2.76 0-5 2.24-5 5s2.24 5 5 5 5-2.24 5-5-2.24-5-5-5zM2 13h2c.55 0 1-.45 1-1s-.45-1-1-1H2c-.55 0-1 .45-1 1s.45 1 1 1zm18 0h2c.55 0 1-.45 1-1s-.45-1-1-1h-2c-.55 0-1 .45-1 1s.45 1 1 1zM11 2v2c0 .55.45 1 1 1s1-.45 1-1V2c0-.55-.45-1-1-1s-1 .45-1 1zm0 18v2c0 .55.45 1 1 1s1-.45 1-1v-2c0-.55-.45-1-1-1s-1 .45-1 1zM5.99 4.58a.996.996 0 0 0-1.41 0 .996.996 0 0 0 0 1.41l1.06 1.06c.39.39 1.03.39 1.41 0s.39-1.03 0-1.41L5.99 4.58zm12.37 12.37a.996.996 0 0 0-1.41 0 .996.996 0 0 0 0 1.41l1.06 1.06c.39.39 1.03.39 1.41 0a.996.996 0 0 0 0-1.41l-1.06-1.06zm1.06-10.96a.996.996 0 0 0 0-1.41.996.996 0 0 0-1.41 0l-1.06 1.06c-.39.39-.39 1.03 0 1.41s1.03.39 1.41 0l1.06-1.06zM7.05 18.36a.996.996 0 0 0 0-1.41.996.996 0 0 0-1.41 0l-1.06 1.06c-.39.39-.39 1.03 0 1.41s1.03.39 1.41 0l1.06-1.06z" />
          </svg>
        ) : (
          <svg viewBox="0 0 24 24" className="h-6 w-6" fill="currentColor" aria-hidden="true">
            <path d="M12 3a9 9 0 1 0 9 9c0-.46-.04-.92-.1-1.36a5.389 5.389 0 0 1-4.4 2.26 5.403 5.403 0 0 1-3.14-9.8c-.44-.06-.9-.1-1.36-.1z" />
          </svg>
        )}
      </button>
    );
  }

  return (
    <div
      role="group"
      aria-label="테마 선택"
      className="inline-flex rounded-lg border border-zinc-300 bg-white p-1 dark:border-zinc-600 dark:bg-zinc-900"
    >
      <button
        type="button"
        onClick={() => handleSelectTheme("dark")}
        aria-pressed={theme === "dark"}
        className={`rounded-md px-3 py-1.5 text-xs font-medium transition ${
          theme === "dark"
            ? "bg-zinc-900 text-white dark:bg-zinc-100 dark:text-zinc-900"
            : "text-zinc-600 hover:text-zinc-900 dark:text-zinc-400 dark:hover:text-zinc-100"
        }`}
      >
        다크
      </button>
      <button
        type="button"
        onClick={() => handleSelectTheme("light")}
        aria-pressed={theme === "light"}
        className={`rounded-md px-3 py-1.5 text-xs font-medium transition ${
          theme === "light"
            ? "bg-zinc-900 text-white dark:bg-zinc-100 dark:text-zinc-900"
            : "text-zinc-600 hover:text-zinc-900 dark:text-zinc-400 dark:hover:text-zinc-100"
        }`}
      >
        라이트
      </button>
      <button
        type="button"
        onClick={() => handleSelectTheme("system")}
        aria-pressed={theme === "system"}
        className={`rounded-md px-3 py-1.5 text-xs font-medium transition ${
          theme === "system"
            ? "bg-zinc-900 text-white dark:bg-zinc-100 dark:text-zinc-900"
            : "text-zinc-600 hover:text-zinc-900 dark:text-zinc-400 dark:hover:text-zinc-100"
        }`}
      >
        시스템
      </button>
    </div>
  );
}
