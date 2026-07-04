import AppShell from "@/components/AppShell";

/**
 * 친구·채팅 공통 앱 셸 레이아웃.
 */
export default function ShellLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return <AppShell>{children}</AppShell>;
}
