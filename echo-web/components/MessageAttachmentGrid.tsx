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
  const activeAttachment = attachments.find((attachment) => attachment.id === activeFileId) ?? null;
  const gridClassName =
    attachments.length === 1
      ? "grid-cols-1"
      : attachments.length === 2
        ? "grid-cols-2"
        : "grid-cols-2";

  return (
    <>
      <div className={`mt-2 grid gap-1 ${gridClassName}`}>
        {attachments.map((attachment) => (
          <button
            key={attachment.id}
            type="button"
            onClick={() => setActiveFileId(attachment.id)}
            className="overflow-hidden rounded-lg"
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
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/70 p-4">
          <button
            type="button"
            aria-label="이미지 닫기"
            className="absolute inset-0"
            onClick={() => setActiveFileId(null)}
          />
          <div
            className={`relative max-h-[90vh] max-w-3xl rounded-xl p-3 ${
              isMine ? "bg-zinc-900" : "bg-white dark:bg-zinc-900"
            }`}
          >
            <AuthenticatedImage
              fileId={activeAttachment.id}
              alt={activeAttachment.originalName}
              className="max-h-[75vh] w-full rounded-lg object-contain"
            />
            <div className="mt-3 flex justify-end gap-2">
              <button
                type="button"
                onClick={() => setActiveFileId(null)}
                className="rounded-lg border border-zinc-300 px-3 py-1.5 text-sm text-zinc-700 dark:border-zinc-600 dark:text-zinc-200"
              >
                닫기
              </button>
              <button
                type="button"
                onClick={() =>
                  void downloadFile(activeAttachment.id, activeAttachment.originalName)
                }
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
