export type Theme = "dark" | "light" | "system";

export type ResolvedTheme = "dark" | "light";

export const THEME_STORAGE_KEY = "echo-theme";

/**
 * 저장된 테마를 실제 적용 테마로 변환한다.
 */
export function resolveTheme(theme: Theme): ResolvedTheme {
  if (theme === "system") {
    return window.matchMedia("(prefers-color-scheme: dark)").matches ? "dark" : "light";
  }

  return theme;
}

/**
 * 문서 루트에 테마를 적용한다.
 */
export function applyTheme(theme: Theme): ResolvedTheme {
  const resolved = resolveTheme(theme);
  const root = document.documentElement;

  root.classList.toggle("dark", resolved === "dark");
  root.style.colorScheme = resolved;
  root.dataset.theme = theme;

  return resolved;
}

/**
 * localStorage에 저장된 테마를 읽는다.
 */
export function readStoredTheme(): Theme {
  try {
    const stored = localStorage.getItem(THEME_STORAGE_KEY);

    if (stored === "dark" || stored === "light" || stored === "system") {
      return stored;
    }
  } catch {
    return "dark";
  }

  return "dark";
}

/**
 * 테마를 localStorage에 저장한다.
 */
export function writeStoredTheme(theme: Theme): void {
  try {
    localStorage.setItem(THEME_STORAGE_KEY, theme);
  } catch {
    return;
  }
}
