"use client";

import { useEffect } from "react";

import { getAccessToken } from "@/lib/auth";
import {
  fetchOnlineUserIds,
  publishPresenceSnapshot,
  publishPresenceUpdate,
} from "@/lib/presence";
import { subscribePresenceUpdates } from "@/lib/stomp";

/**
 * 로그인 사용자의 온라인 상태를 구독하고 앱 전역 이벤트로 전파한다.
 */
export default function PresenceListener() {
  useEffect(() => {
    let active = true;
    let unsubscribePresence: () => void = () => undefined;

    async function refreshPresenceSnapshot() {
      const token = getAccessToken();

      if (!token || !active) {
        return;
      }

      const onlineUserIds = await fetchOnlineUserIds();

      if (!active) {
        return;
      }

      publishPresenceSnapshot(onlineUserIds);
    }

    async function setupPresence() {
      await refreshPresenceSnapshot();

      if (!active || !getAccessToken()) {
        return;
      }

      unsubscribePresence();
      unsubscribePresence = subscribePresenceUpdates(
        (update) => {
          publishPresenceUpdate(update);
        },
        () => {
          void refreshPresenceSnapshot();
        },
      );
    }

    const timerId = globalThis.setTimeout(() => {
      void setupPresence();
    }, 0);

    function handleVisibilityChange() {
      if (globalThis.document.visibilityState === "visible") {
        void refreshPresenceSnapshot();
      }
    }

    function handleFocus() {
      void refreshPresenceSnapshot();
    }

    globalThis.window.addEventListener("focus", handleFocus);
    globalThis.document.addEventListener("visibilitychange", handleVisibilityChange);

    return () => {
      active = false;
      globalThis.clearTimeout(timerId);
      unsubscribePresence();
      globalThis.window.removeEventListener("focus", handleFocus);
      globalThis.document.removeEventListener("visibilitychange", handleVisibilityChange);
    };
  }, []);

  return null;
}
