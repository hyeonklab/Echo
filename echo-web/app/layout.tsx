import type { Metadata } from "next";
import { Geist, Geist_Mono } from "next/font/google";

import MessageNotificationListener from "@/components/MessageNotificationListener";
import PresenceListener from "@/components/PresenceListener";
import ThemeProvider from "@/components/ThemeProvider";

import { THEME_STORAGE_KEY } from "@/lib/theme";

import "./globals.css";

const geistSans = Geist({
  variable: "--font-geist-sans",
  subsets: ["latin"],
});

const geistMono = Geist_Mono({
  variable: "--font-geist-mono",
  subsets: ["latin"],
});

export const metadata: Metadata = {
  title: "Echo | 실시간 메신저",
  description: "Echo realtime messenger",
  icons: {
    icon: "/echo-logo-140.png",
    apple: "/echo-logo-140.png",
  },
};

const themeInitScript = `
(function () {
  try {
    var theme = localStorage.getItem("${THEME_STORAGE_KEY}") || "dark";
    var resolved = theme;

    if (theme === "system") {
      resolved = window.matchMedia("(prefers-color-scheme: dark)").matches ? "dark" : "light";
    }

    if (resolved === "dark") {
      document.documentElement.classList.add("dark");
      document.documentElement.style.colorScheme = "dark";
      document.documentElement.dataset.theme = theme;
      return;
    }

    document.documentElement.classList.remove("dark");
    document.documentElement.style.colorScheme = "light";
    document.documentElement.dataset.theme = theme;
  } catch (_) {}
})();
`;

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html
      lang="ko"
      className={`${geistSans.variable} ${geistMono.variable} h-full antialiased`}
      suppressHydrationWarning
    >
      <head>
        <script dangerouslySetInnerHTML={{ __html: themeInitScript }} />
      </head>
      <body className="h-full min-h-full" suppressHydrationWarning>
        <ThemeProvider>
          <MessageNotificationListener />
          <PresenceListener />
          {children}
        </ThemeProvider>
      </body>
    </html>
  );
}
