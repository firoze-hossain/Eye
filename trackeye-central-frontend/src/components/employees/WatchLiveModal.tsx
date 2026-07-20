// src/components/employees/WatchLiveModal.tsx
'use client';

import { useEffect, useRef, useState } from 'react';
import { X, Loader2, MonitorX } from 'lucide-react';
import { apiClient, API } from '../../lib/api';

interface Props {
    deviceId: number;
    deviceName: string;
    userFullName: string;
    onClose: () => void;
}

/**
 * "Near-live" viewer: polls a JPEG frame roughly once a second and swaps the
 * displayed image. Not a video stream - see WatchService.java on the backend
 * for why this trade-off was made (no new infra, ~1-2s latency).
 */
export default function WatchLiveModal({ deviceId, deviceName, userFullName, onClose }: Props) {
    const [src, setSrc] = useState<string | null>(null);
    const [status, setStatus] = useState<'connecting' | 'live' | 'no-frame' | 'error'>('connecting');
    const objectUrlRef = useRef<string | null>(null);
    const stoppedRef = useRef(false);

    useEffect(() => {
        stoppedRef.current = false;
        let frameTimer: ReturnType<typeof setTimeout>;
        let renewTimer: ReturnType<typeof setInterval>;

        const fetchFrame = async () => {
            if (stoppedRef.current) return;
            try {
                const res = await fetch(
                    (process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080') + API.devices.watchFrame(deviceId),
                    { headers: await authHeader() }
                );
                if (res.status === 204) {
                    setStatus('no-frame');
                } else if (res.ok) {
                    const blob = await res.blob();
                    const url = URL.createObjectURL(blob);
                    if (objectUrlRef.current) URL.revokeObjectURL(objectUrlRef.current);
                    objectUrlRef.current = url;
                    setSrc(url);
                    setStatus('live');
                } else {
                    setStatus('error');
                }
            } catch {
                setStatus('error');
            } finally {
                if (!stoppedRef.current) frameTimer = setTimeout(fetchFrame, 1000);
            }
        };

        const authHeader = async (): Promise<HeadersInit> => {
            // apiClient attaches the session token via axios interceptors; for a
            // raw fetch (needed so we can read the response as a blob directly)
            // we grab the same NextAuth session token once and reuse it.
            const { getSession } = await import('next-auth/react');
            const session: any = await getSession();
            return session?.accessToken ? { Authorization: `Bearer ${session.accessToken}` } : {};
        };

        (async () => {
            try {
                await apiClient.post(API.devices.watchStart(deviceId));
                fetchFrame();
                renewTimer = setInterval(() => {
                    apiClient.post(API.devices.watchRenew(deviceId)).catch(() => {});
                }, 10000);
            } catch {
                setStatus('error');
            }
        })();

        return () => {
            stoppedRef.current = true;
            clearTimeout(frameTimer);
            clearInterval(renewTimer);
            if (objectUrlRef.current) URL.revokeObjectURL(objectUrlRef.current);
            apiClient.post(API.devices.watchStop(deviceId)).catch(() => {});
        };
    }, [deviceId]);

    return (
        <div className="fixed inset-0 z-50 bg-black/70 flex items-center justify-center p-4">
            <div className="bg-dark-900 rounded-xl overflow-hidden w-full max-w-4xl shadow-2xl">
                <div className="flex items-center justify-between px-4 py-3 bg-dark-800">
                    <div className="flex items-center gap-2 text-white">
                        <span className={`w-2 h-2 rounded-full ${status === 'live' ? 'bg-green-500 animate-pulse' : 'bg-yellow-500'}`} />
                        <span className="font-medium">{userFullName}</span>
                        <span className="text-dark-400 text-sm">· {deviceName}</span>
                    </div>
                    <button onClick={onClose} className="p-1.5 hover:bg-dark-700 rounded-lg text-dark-300 hover:text-white">
                        <X className="w-5 h-5" />
                    </button>
                </div>

                <div className="aspect-video bg-black flex items-center justify-center relative">
                    {status === 'connecting' && (
                        <div className="text-dark-400 flex flex-col items-center gap-2">
                            <Loader2 className="w-8 h-8 animate-spin" />
                            <span className="text-sm">Connecting…</span>
                        </div>
                    )}
                    {status === 'no-frame' && !src && (
                        <div className="text-dark-400 flex flex-col items-center gap-2">
                            <MonitorX className="w-8 h-8" />
                            <span className="text-sm">Waiting for the first frame — this can take a few seconds.</span>
                        </div>
                    )}
                    {status === 'error' && !src && (
                        <div className="text-red-400 flex flex-col items-center gap-2">
                            <MonitorX className="w-8 h-8" />
                            <span className="text-sm">Couldn't reach this device. It may be offline.</span>
                        </div>
                    )}
                    {src && (
                        // eslint-disable-next-line @next/next/no-img-element
                        <img src={src} alt="Live screen" className="w-full h-full object-contain" />
                    )}
                </div>

                <div className="px-4 py-2 bg-dark-800 text-xs text-dark-400">
                    Live view refreshes about once a second. Closing this window stops the session.
                </div>
            </div>
        </div>
    );
}
