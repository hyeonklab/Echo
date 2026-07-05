import AuthenticatedImage from "@/components/AuthenticatedImage";

type UserAvatarProps = {
  displayName: string;
  avatarFileId?: number | null;
  className?: string;
  textClassName?: string;
};

/**
 * 사용자 아바타를 표시한다.
 */
export default function UserAvatar({
  displayName,
  avatarFileId,
  className = "h-10 w-10",
  textClassName = "text-sm font-semibold",
}: Readonly<UserAvatarProps>) {
  const initial = displayName.trim().slice(0, 1) || "?";

  if (avatarFileId) {
    return (
      <AuthenticatedImage
        fileId={avatarFileId}
        alt={`${displayName} 프로필`}
        className={`${className} rounded-full object-cover`}
      />
    );
  }

  return (
    <div
      className={`${className} flex items-center justify-center rounded-full bg-zinc-200 text-zinc-700 dark:bg-zinc-700 dark:text-zinc-100`}
    >
      <span className={textClassName}>{initial}</span>
    </div>
  );
}
