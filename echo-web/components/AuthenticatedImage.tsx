"use client";

import { useEffect, useState } from "react";

import { fetchAuthenticatedFileBlob } from "@/lib/files";

type AuthenticatedImageProps = {
  fileId: number;
  alt: string;
  className?: string;
};

/**
 * 인증이 필요한 이미지 파일을 표시한다.
 */
export default function AuthenticatedImage({
  fileId,
  alt,
  className,
}: Readonly<AuthenticatedImageProps>) {
  const [objectUrl, setObjectUrl] = useState<string | null>(null);

  useEffect(() => {
    let active = true;
    let currentUrl: string | null = null;

    void fetchAuthenticatedFileBlob(fileId).then((blob) => {
      if (!active || !blob) {
        return;
      }

      currentUrl = URL.createObjectURL(blob);
      setObjectUrl(currentUrl);
    });

    return () => {
      active = false;

      if (currentUrl) {
        URL.revokeObjectURL(currentUrl);
      }
    };
  }, [fileId]);

  if (!objectUrl) {
    return <div className={className} aria-hidden="true" />;
  }

  return <img src={objectUrl} alt={alt} className={className} />;
}
