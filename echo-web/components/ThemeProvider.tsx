"use client";

import {
  createContext,
  type ReactNode,
  useContext,
  useEffect,
  useMemo,
  useState,
} from "react";

import {
  applyTheme,
  readStoredTheme,
  resolveTheme,
  type ResolvedTheme,
  type Theme,
  writeStoredTheme,
} from "@/lib/theme";

type ThemeContextValue = {
  theme: Theme;
  resolvedTheme: ResolvedTheme;
  setTheme: (theme: Theme) => void;
};

const ThemeContext = createContext<ThemeContextValue | null>(null);

type ThemeProviderProps = {
  children: ReactNode;
};

/**
 * 다크/라이트/시스템 테마 Provider.
 */
export default function ThemeProvider({ children }: Readonly<ThemeProviderProps>) {
  const [theme, setThemeState] = useState<Theme>(() => readStoredTheme());
  const [resolvedTheme, setResolvedTheme] = useState<ResolvedTheme>(() => resolveTheme(readStoredTheme()));

  useEffect(() => {
    const resolved = applyTheme(theme);

    setResolvedTheme(resolved);
  }, [theme]);

  useEffect(() => {
    if (theme !== "system") {
      return;
    }

    const mediaQuery = window.matchMedia("(prefers-color-scheme: dark)");

    function handleSystemThemeChange() {
      const resolved = applyTheme("system");

      setResolvedTheme(resolved);
    }

    mediaQuery.addEventListener("change", handleSystemThemeChange);

    return () => {
      mediaQuery.removeEventListener("change", handleSystemThemeChange);
    };
  }, [theme]);

  function setTheme(nextTheme: Theme) {
    const resolved = applyTheme(nextTheme);

    setThemeState(nextTheme);
    setResolvedTheme(resolved);
    writeStoredTheme(nextTheme);
  }

  const value = useMemo(
    () => ({
      theme,
      resolvedTheme,
      setTheme,
    }),
    [theme, resolvedTheme],
  );

  return <ThemeContext.Provider value={value}>{children}</ThemeContext.Provider>;
}

/**
 * 현재 테마 컨텍스트를 반환한다.
 */
export function useTheme(): ThemeContextValue {
  const context = useContext(ThemeContext);

  if (!context) {
    throw new Error("useTheme은 ThemeProvider 내부에서만 사용할 수 있습니다.");
  }

  return context;
}
