"use client";

import { Fragment } from "react";

import LinkPreviewCard from "@/components/LinkPreviewCard";
import { extractUrls, normalizeUrl } from "@/lib/link-preview";

const LINK_URL_PATTERN = /https?:\/\/[^\s<>"')\]}]+/gi;

type MessageContentProps = {
  content: string;
  isMine: boolean;
};

/**
 * 메시지 본문과 링크 미리보기를 렌더링한다.
 */
export default function MessageContent({ content, isMine }: Readonly<MessageContentProps>) {
  const firstUrl = extractUrls(content)[0] ?? null;

  return (
    <>
      <p className="whitespace-pre-wrap break-words text-sm">{renderLinkedText(content, isMine)}</p>
      {firstUrl ? (
        <div className="mt-2">
          <LinkPreviewCard url={firstUrl} isMine={isMine} />
        </div>
      ) : null}
    </>
  );
}

/**
 * 메시지 텍스트의 URL을 클릭 가능한 링크로 변환한다.
 */
function renderLinkedText(content: string, isMine: boolean) {
  const parts: Array<string | { href: string; label: string }> = [];
  let lastIndex = 0;

  for (const match of content.matchAll(LINK_URL_PATTERN)) {
    const rawUrl = match[0];
    const start = match.index ?? 0;

    if (start > lastIndex) {
      parts.push(content.slice(lastIndex, start));
    }

    parts.push({
      href: normalizeUrl(rawUrl),
      label: rawUrl,
    });
    lastIndex = start + rawUrl.length;
  }

  if (parts.length === 0) {
    return content;
  }

  if (lastIndex < content.length) {
    parts.push(content.slice(lastIndex));
  }

  return parts.map((part, index) => {
    if (typeof part === "string") {
      return <Fragment key={`${index}-${part}`}>{part}</Fragment>;
    }

    return (
      <a
        key={`${index}-${part.href}`}
        href={part.href}
        target="_blank"
        rel="noopener noreferrer"
        className={`underline underline-offset-2 ${
          isMine ? "text-sky-200 hover:text-sky-100 dark:text-sky-700 dark:hover:text-sky-800" : "text-sky-600 hover:text-sky-500 dark:text-sky-400 dark:hover:text-sky-300"
        }`}
      >
        {part.label}
      </a>
    );
  });
}
