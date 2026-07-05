import { ensureAccessToken, normalizeAuthUser, type AuthUser } from "@/lib/auth";
import { apiFetch, getApiUrl } from "@/lib/api";

export type FilePurpose = "AVATAR" | "MESSAGE";

export type UploadedFile = {
  id: number;
  originalName: string;
  contentType: string;
  sizeBytes: number;
};

/**
 * 인증이 필요한 파일 URL을 반환한다.
 */
export function getFileUrl(fileId: number, download = false): string {
  const params = download ? "?download=true" : "";

  return `${getApiUrl()}/api/files/${fileId}${params}`;
}

/**
 * 이미지 파일을 업로드한다.
 */
export async function uploadFiles(
  purpose: FilePurpose,
  files: File[],
): Promise<UploadedFile[] | null> {
  const token = await ensureAccessToken();

  if (!token || files.length === 0) {
    return null;
  }

  const formData = new FormData();

  formData.set("purpose", purpose);

  for (const file of files) {
    formData.append("files", file);
  }

  const response = await apiFetch(`${getApiUrl()}/api/files`, {
    method: "POST",
    headers: {
      Authorization: `Bearer ${token}`,
    },
    body: formData,
    cache: "no-store",
  });

  if (!response.ok) {
    return null;
  }

  const payload = (await response.json()) as { files: UploadedFile[] };

  return payload.files;
}

/**
 * 인증 헤더로 파일 blob을 조회한다.
 */
export async function fetchAuthenticatedFileBlob(
  fileId: number,
  download = false,
): Promise<Blob | null> {
  const token = await ensureAccessToken();

  if (!token) {
    return null;
  }

  const response = await apiFetch(getFileUrl(fileId, download), {
    headers: {
      Authorization: `Bearer ${token}`,
    },
    cache: "no-store",
  });

  if (!response.ok) {
    return null;
  }

  return response.blob();
}

/**
 * 파일을 다운로드한다.
 */
export async function downloadFile(fileId: number, fileName: string): Promise<boolean> {
  const blob = await fetchAuthenticatedFileBlob(fileId, true);

  if (!blob || globalThis.window === undefined) {
    return false;
  }

  const objectUrl = URL.createObjectURL(blob);
  const anchor = document.createElement("a");

  anchor.href = objectUrl;
  anchor.download = fileName;
  anchor.click();
  URL.revokeObjectURL(objectUrl);

  return true;
}

/**
 * 프로필 사진을 업로드한다.
 */
export async function uploadAvatar(file: File): Promise<AuthUser | null> {
  const token = await ensureAccessToken();

  if (!token) {
    return null;
  }

  const formData = new FormData();

  formData.set("file", file);

  const response = await apiFetch(`${getApiUrl()}/api/users/me/avatar`, {
    method: "POST",
    headers: {
      Authorization: `Bearer ${token}`,
    },
    body: formData,
    cache: "no-store",
  });

  if (!response.ok) {
    return null;
  }

  return normalizeAuthUser((await response.json()) as AuthUser);
}
