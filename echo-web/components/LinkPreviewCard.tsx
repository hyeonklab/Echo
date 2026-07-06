"use client";

import { useEffect, useState } from "react";

import {
  fetchLinkPreview,
  formatLinkHost,
  hasLinkPreviewContent,
  truncatePreviewText,
  type LinkPreview,
} from "@/lib/link-preview";

type LinkPreviewCardProps = {
  url: string;
  isMine?: boolean;
  debounceMs?: number;
  onMediaLoad?: () => void;
};

/**
 * 링크 미리보기 카드.
 */
export default function LinkPreviewCard({
  url,
  isMine = false,
  debounceMs = 0,
  onMediaLoad,
}: Readonly<LinkPreviewCardProps>) {
  const [debouncedUrl, setDebouncedUrl] = useState(url);
  const [preview, setPreview] = useState<LinkPreview | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (debounceMs <= 0) {
      setDebouncedUrl(url);
      return;
    }

    const timerId = globalThis.setTimeout(() => {
      setDebouncedUrl(url);
    }, debounceMs);

    return () => {
      globalThis.clearTimeout(timerId);
    };
  }, [debounceMs, url]);

  useEffect(() => {
    let cancelled = false;

    async function loadPreview() {
      setLoading(true);
      setPreview(null);

      const result = await fetchLinkPreview(debouncedUrl);

      if (cancelled) {
        return;
      }

      setPreview(result);
      setLoading(false);
      onMediaLoad?.();
    }

    void loadPreview();

    return () => {
      cancelled = true;
    };
  }, [debouncedUrl]);

  if (loading) {
    return (
      <div
        className={`overflow-hidden rounded-lg border ${
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
      className={`block overflow-hidden rounded-lg border transition hover:opacity-90 ${
        isMine
          ? "border-zinc-700/60 bg-zinc-800/40 dark:border-zinc-300/30 dark:bg-zinc-200/40"
          : "border-zinc-200 bg-zinc-50 dark:border-zinc-600 dark:bg-zinc-800/80"
      }`}
    >
      {preview.imageUrl ? (
        <div className="border-b border-zinc-200/80 bg-zinc-100 dark:border-zinc-600/80 dark:bg-zinc-900">
          <img
            src={preview.imageUrl}
            alt={preview.title ?? hostLabel}
            className="max-h-40 w-full object-cover"
            loading="lazy"
            onLoad={onMediaLoad}
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
