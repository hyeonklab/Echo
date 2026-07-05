"use client";

import { useState } from "react";

import AuthenticatedImage from "@/components/AuthenticatedImage";
import { downloadFile } from "@/lib/files";
import type { MessageFile } from "@/lib/messages";

type MessageAttachmentGridProps = {
  attachments: MessageFile[];
  isMine: boolean;
};

/**
 * 메시지 이미지 앨범을 렌더링한다.
 */
export default function MessageAttachmentGrid({
  attachments,
  isMine,
}: Readonly<MessageAttachmentGridProps>) {
  const [activeFileId, setActiveFileId] = useState<number | null>(null);
  const [downloadError, setDownloadError] = useState<string | null>(null);
  const activeAttachment = attachments.find((attachment) => attachment.id === activeFileId) ?? null;
  const gridClassName =
    attachments.length === 1
      ? "grid-cols-1"
      : attachments.length === 2
        ? "grid-cols-2"
        : "grid-cols-2";

  function handleOpenAttachment(fileId: number) {
    setDownloadError(null);
    setActiveFileId(fileId);
  }

  function handleCloseViewer() {
    setDownloadError(null);
    setActiveFileId(null);
  }

  async function handleDownload() {
    if (!activeAttachment) {
      return;
    }

    setDownloadError(null);

    const success = await downloadFile(activeAttachment.id, activeAttachment.originalName);

    if (!success) {
      setDownloadError("파일 다운로드에 실패했습니다.");
    }
  }

  return (
    <>
      <div className={`mt-2 grid gap-1 ${gridClassName}`}>
        {attachments.map((attachment) => (
          <button
            key={attachment.id}
            type="button"
            onClick={() => handleOpenAttachment(attachment.id)}
            className="min-h-20 overflow-hidden rounded-lg"
          >
            <AuthenticatedImage
              fileId={attachment.id}
              alt={attachment.originalName}
              className="max-h-56 w-full object-cover"
            />
          </button>
        ))}
      </div>

      {activeAttachment ? (
        <div
          className="fixed inset-0 z-[60] flex items-center justify-center bg-black/70 p-4"
          onClick={handleCloseViewer}
          onKeyDown={(event) => {
            if (event.key === "Escape") {
              handleCloseViewer();
            }
          }}
          role="dialog"
          aria-modal="true"
          aria-label="이미지 보기"
        >
          <div
            className={`relative z-10 max-h-[90vh] max-w-3xl rounded-xl p-3 ${
              isMine ? "bg-zinc-900" : "bg-white dark:bg-zinc-900"
            }`}
            onClick={(event) => event.stopPropagation()}
          >
            <AuthenticatedImage
              fileId={activeAttachment.id}
              alt={activeAttachment.originalName}
              className="max-h-[75vh] w-full rounded-lg object-contain"
            />
            {downloadError ? (
              <p className="mt-2 text-sm text-red-500 dark:text-red-400">{downloadError}</p>
            ) : null}
            <div className="mt-3 flex justify-end gap-2">
              <button
                type="button"
                onClick={handleCloseViewer}
                className="rounded-lg border border-zinc-300 px-3 py-1.5 text-sm text-zinc-700 dark:border-zinc-600 dark:text-zinc-200"
              >
                닫기
              </button>
              <button
                type="button"
                onClick={() => void handleDownload()}
                className="rounded-lg bg-zinc-900 px-3 py-1.5 text-sm font-medium text-white dark:bg-zinc-100 dark:text-zinc-900"
              >
                다운로드
              </button>
            </div>
          </div>
        </div>
      ) : null}
    </>
  );
}
