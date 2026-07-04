/**
 * 브라우저 알림 지원 여부를 반환한다.
 */
export function isNotificationSupported(): boolean {
  return globalThis.window !== undefined && "Notification" in globalThis;
}

/**
 * 알림 권한을 요청한다.
 */
export async function requestNotificationPermission(): Promise<NotificationPermission | "unsupported"> {
  if (!isNotificationSupported()) {
    return "unsupported";
  }

  if (Notification.permission === "default") {
    return Notification.requestPermission();
  }

  return Notification.permission;
}

/**
 * 메시지 내용을 알림 본문용으로 정리한다.
 */
export function formatNotificationPreview(content: string): string {
  const normalized = content.replace(/\s+/g, " ").trim();

  if (normalized.length > 80) {
    return `${normalized.slice(0, 80)}...`;
  }

  return normalized;
}

/**
 * 메시지 미리보기 문구를 알림용으로 정리한다.
 */
export function formatNotificationBody(senderLabel: string, content: string): string {
  return `${senderLabel}: ${formatNotificationPreview(content)}`;
}

type ShowMessageNotificationOptions = {
  title: string;
  body: string;
  roomId: number;
  onClick: () => void;
};

/**
 * 새 메시지 브라우저 알림을 표시한다.
 */
export function showMessageNotification(options: ShowMessageNotificationOptions): void {
  if (!isNotificationSupported() || Notification.permission !== "granted") {
    return;
  }

  const notification = new Notification(options.title, {
    body: options.body,
    tag: `echo-room-${options.roomId}`,
  });

  notification.onclick = () => {
    globalThis.window.focus();
    options.onClick();
    notification.close();
  };
}

/**
 * 현재 화면에서 해당 채팅방을 보고 있으면 true를 반환한다.
 */
export function isViewingRoom(roomId: number, pathname: string): boolean {
  return pathname === `/chat/${roomId}`;
}

/**
 * 알림을 표시해야 하면 true를 반환한다.
 */
export function shouldNotifyMessage(
  roomId: number,
  senderId: number,
  currentUserId: number,
  pathname: string,
): boolean {
  if (senderId === currentUserId) {
    return false;
  }

  if (!isViewingRoom(roomId, pathname)) {
    return true;
  }

  if (globalThis.document.visibilityState !== "visible") {
    return true;
  }

  return !globalThis.document.hasFocus();
}
