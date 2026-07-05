const DEFAULT_API_URL = process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080";
const FETCH_TIMEOUT_MS = 8000;
const FILE_UPLOAD_TIMEOUT_MS = 120000;

/**
 * API base URL을 반환한다. DDNS 접속 시 브라우저 호스트 기준 8080을 사용한다.
 */
export function getApiUrl(): string {
  if (globalThis.window !== undefined) {
    const { hostname, protocol } = globalThis.window.location;

    if (hostname !== "localhost" && hostname !== "127.0.0.1") {
      return `${protocol}//${hostname}:8080`;
    }
  }

  return DEFAULT_API_URL;
}

/**
 * STOMP WebSocket URL을 반환한다.
 */
export function getWsUrl(): string {
  return `${getApiUrl().replace(/^http/, "ws")}/ws`;
}

/**
 * 타임아웃이 적용된 fetch를 실행한다.
 */
export async function apiFetch(
  url: string,
  init?: RequestInit,
  timeoutMs: number = FETCH_TIMEOUT_MS,
): Promise<Response> {
  const controller = new AbortController();
  const timeoutId = globalThis.setTimeout(() => controller.abort(), timeoutMs);

  try {
    return await fetch(url, {
      ...init,
      signal: controller.signal,
    });
  } finally {
    globalThis.clearTimeout(timeoutId);
  }
}

export { FILE_UPLOAD_TIMEOUT_MS };
