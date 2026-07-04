import FriendList from "@/components/FriendList";

/**
 * 친구 목록 페이지.
 */
export default function FriendsPage() {
  return (
    <div className="flex h-full flex-col overflow-hidden">
      <FriendList />
    </div>
  );
}
