"use client";

import { Fragment, useEffect, useState } from "react";

import {
  extractUrls,
  fetchLinkPreview,
  formatLinkHost,
  hasLinkPreviewContent,
  normalizeUrl,
  truncatePreviewText,
  type LinkPreview,
} from "@/lib/link-preview";

const LINK_URL_PATTERN = /https?:\/\/[^\s<>"')\]}]+/gi;

type LinkPreviewCardProps = {
  url: string;
  isMine: boolean;
};

/**
 * 링크 미리보기 카드.
 */
function LinkPreviewCard({ url, isMine }: Readonly<LinkPreviewCardProps>) {
  const [preview, setPreview] = useState<LinkPreview | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    let cancelled = false;

    async function loadPreview() {
      setLoading(true);

      const result = await fetchLinkPreview(url);

      if (cancelled) {
        return;
      }

      setPreview(result);
      setLoading(false);
    }

    void loadPreview();

    return () => {
      cancelled = true;
    };
  }, [url]);

  if (loading) {
    return (
      <div
        className={`mt-2 overflow-hidden rounded-lg border ${
          isMine
            ? "border-zinc-700/60 bg-zinc-800/40 dark:border-zinc-300/30 dark:bg-zinc-200/40"
            : "border-zinc-200 bg-zinc-50 dark:border-zinc-600 dark:bg-zinc-800/80"
        }`}
      >
        <div className="animate-pulse space-y-2 p-3">
          <div className="h-3 w-2/3 rounded bg-zinc-300/70 dark:bg-zinc-600/70" />
          <div className="h-3 w-full rounded bg-zinc-300/50 dark:bg-zinc-600/50" />
        </div>
      </div>
    );
  }

  if (!preview || !hasLinkPreviewContent(preview)) {
    return null;
  }

  const hostLabel = preview.siteName ?? formatLinkHost(preview.url);

  return (
    <a
      href={preview.url}
      target="_blank"
      rel="noopener noreferrer"
      className={`mt-2 block overflow-hidden rounded-lg border transition hover:opacity-90 ${
        isMine
          ? "border-zinc-700/60 bg-zinc-800/40 dark:border-zinc-300/30 dark:bg-zinc-200/40"
          : "border-zinc-200 bg-zinc-50 dark:border-zinc-600 dark:bg-zinc-800/80"
      }`}
    >
      {preview.imageUrl ? (
        <div className="border-b border-zinc-200/80 bg-zinc-100 dark:border-zinc-600/80 dark:bg-zinc-900">
          {/* eslint-disable-next-line @next/next/no-img-element */}
          <img
            src={preview.imageUrl}
            alt={preview.title ?? hostLabel}
            className="max-h-40 w-full object-cover"
            loading="lazy"
          />
        </div>
      ) : null}
      <div className="space-y-1 p-3">
        <p
          className={`text-[11px] font-medium uppercase tracking-wide ${
            isMine ? "text-zinc-300 dark:text-zinc-500" : "text-zinc-400"
          }`}
        >
          {hostLabel}
        </p>
        {preview.title ? (
          <p
            className={`line-clamp-2 text-sm font-semibold ${
              isMine ? "text-white dark:text-zinc-900" : "text-zinc-900 dark:text-zinc-100"
            }`}
          >
            {preview.title}
          </p>
        ) : null}
        {preview.description ? (
          <p
            className={`line-clamp-2 text-xs ${
              isMine ? "text-zinc-300 dark:text-zinc-600" : "text-zinc-500 dark:text-zinc-400"
            }`}
          >
            {truncatePreviewText(preview.description)}
          </p>
        ) : null}
      </div>
    </a>
  );
}

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
      {firstUrl ? <LinkPreviewCard url={firstUrl} isMine={isMine} /> : null}
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
