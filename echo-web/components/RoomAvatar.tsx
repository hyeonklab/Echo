import UserAvatar from "@/components/UserAvatar";
import { getRoomAvatarMembers, type Room } from "@/lib/rooms";

type RoomAvatarProps = {
  room: Room;
  currentUserId: number;
  className?: string;
  tileClassName?: string;
  textClassName?: string;
};

/**
 * 채팅방 유형에 맞는 프로필 썸네일을 표시한다.
 */
export default function RoomAvatar({
  room,
  currentUserId,
  className = "h-12 w-12",
  tileClassName = "h-full w-full",
  textClassName = "text-[10px] font-semibold",
}: Readonly<RoomAvatarProps>) {
  const members = getRoomAvatarMembers(room, currentUserId);

  if (members.length === 0) {
    return (
      <UserAvatar
        displayName={room.name}
        className={className}
        textClassName={textClassName}
      />
    );
  }

  if (members.length === 1) {
    const member = members[0];

    return (
      <UserAvatar
        displayName={member.displayName}
        avatarFileId={member.avatarFileId}
        className={className}
        textClassName={textClassName}
      />
    );
  }

  const gridClassName = members.length === 2 ? "grid-cols-2 grid-rows-1" : "grid-cols-2 grid-rows-2";

  return (
    <div className={`${className} overflow-hidden rounded-full bg-zinc-200 dark:bg-zinc-700`}>
      <div className={`grid h-full w-full gap-px ${gridClassName}`}>
        {members.map((member, index) => (
          <div
            key={member.userId}
            className={`overflow-hidden ${members.length === 3 && index === 2 ? "col-span-2" : ""}`}
          >
            <UserAvatar
              displayName={member.displayName}
              avatarFileId={member.avatarFileId}
              className={`${tileClassName} rounded-none`}
              textClassName={textClassName}
            />
          </div>
        ))}
      </div>
    </div>
  );
}
