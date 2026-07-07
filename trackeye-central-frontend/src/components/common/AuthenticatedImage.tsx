// src/components/common/AuthenticatedImage.tsx
'use client';

import { useEffect, useState } from 'react';
import { apiClient } from '@/lib/api';

/**
 * A plain <img src="/api/screenshots/image?..."> cannot attach the
 * Authorization: Bearer header, so protected screenshot images fail with 401.
 *
 * This component fetches the image through the axios client (which DOES attach
 * the token), turns it into an object URL, and renders it. Object URLs are
 * revoked on unmount to avoid memory leaks.
 */
export default function AuthenticatedImage({
    url,
    alt,
    className,
    onClick,
}: {
    url: string;
    alt?: string;
    className?: string;
    onClick?: () => void;
}) {
    const [src, setSrc] = useState<string | null>(null);
    const [failed, setFailed] = useState(false);

    useEffect(() => {
        let objectUrl: string | null = null;
        let cancelled = false;

        (async () => {
            try {
                const blob = await apiClient.get<Blob>(url, { responseType: 'blob' });
                if (cancelled) return;
                objectUrl = URL.createObjectURL(blob);
                setSrc(objectUrl);
            } catch {
                if (!cancelled) setFailed(true);
            }
        })();

        return () => {
            cancelled = true;
            if (objectUrl) URL.revokeObjectURL(objectUrl);
        };
    }, [url]);

    if (failed) {
        return (
            <div className={`flex items-center justify-center bg-dark-100 text-dark-400 text-xs ${className || ''}`}>
                Unavailable
            </div>
        );
    }

    if (!src) {
        return <div className={`animate-pulse bg-dark-100 ${className || ''}`} />;
    }

    // eslint-disable-next-line @next/next/no-img-element
    return <img src={src} alt={alt || ''} className={className} onClick={onClick} />;
}
