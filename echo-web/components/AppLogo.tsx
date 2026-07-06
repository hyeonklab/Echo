import Link from "next/link";

type AppLogoProps = {
  size?: number;
  showName?: boolean;
  href?: string | null;
  className?: string;
};

/**
 * Echo 앱 로고를 표시한다.
 */
export default function AppLogo({
  size = 36,
  showName = false,
  href = "/home",
  className = "",
}: Readonly<AppLogoProps>) {
  const src = size <= 72 ? "/echo-logo-140.png" : "/echo-logo.png";

  const content = (
    <span className={`inline-flex items-center gap-2 ${className}`}>
      <img
        src={src}
        alt="Echo"
        width={size}
        height={size}
        className="shrink-0 rounded-xl"
        draggable={false}
      />
      {showName ? (
        <span className="text-sm font-semibold text-zinc-900 dark:text-zinc-50">Echo</span>
      ) : null}
    </span>
  );

  if (href) {
    return (
      <Link href={href} aria-label="Echo 홈" className="inline-flex">
        {content}
      </Link>
    );
  }

  return content;
}
