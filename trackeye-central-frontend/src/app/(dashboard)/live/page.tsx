// src/app/(dashboard)/live/page.tsx
'use client';

import { useQuery } from 'react-query';
import { apiClient, API } from '@/lib/api';
import Card from '@/components/ui/Card';
import LoadingSpinner from '@/components/common/LoadingSpinner';
import { Activity, Clock, Monitor, Circle } from 'lucide-react';

interface LiveActivity {
    userId: number;
    userFullName: string;
    deviceName: string;
    currentApp: string;
    currentWindowTitle: string;
    lastActivityAt: number;
    isOnline: boolean;
    idleTimeMs: number;
    lastScreenshotUrl: string | null;
}

function formatIdle(ms: number) {
    const mins = Math.floor(ms / 60000);
    if (mins < 1) return 'active now';
    if (mins < 60) return `${mins} min idle`;
    return `${Math.floor(mins / 60)}h ${mins % 60}m idle`;
}

function formatLastSeen(ts: number) {
    const mins = Math.floor((Date.now() - ts) / 60000);
    if (mins < 1) return 'just now';
    if (mins < 60) return `${mins} min ago`;
    return `${Math.floor(mins / 60)}h ago`;
}

export default function LiveActivityPage() {
    const { data, isLoading, error, isFetching } = useQuery<LiveActivity[]>(
        'live-activity',
        async () => apiClient.get<LiveActivity[]>(API.dashboard.live),
        { refetchInterval: 10000 } // poll every 10s
    );

    if (isLoading) return <LoadingSpinner />;

    const users = data || [];

    return (
        <div className="space-y-6">
            <div className="flex items-center justify-between">
                <div>
                    <h1 className="text-2xl font-bold text-dark-900">Live Activity</h1>
                    <p className="text-dark-500 mt-1">
                        Who's active right now (seen in the last 5 minutes)
                    </p>
                </div>
                <div className="flex items-center gap-2 text-sm text-dark-500">
                    <Circle className={`w-3 h-3 ${isFetching ? 'text-green-500 fill-green-500 animate-pulse' : 'text-dark-300 fill-dark-300'}`} />
                    Auto-refreshing
                </div>
            </div>

            {error && (
                <Card className="p-6">
                    <p className="text-red-600">Couldn't load live activity.</p>
                </Card>
            )}

            {users.length === 0 ? (
                <Card className="p-12 text-center">
                    <Activity className="w-10 h-10 text-dark-300 mx-auto mb-3" />
                    <p className="text-dark-500">No one is currently online.</p>
                    <p className="text-dark-400 text-sm mt-1">
                        Users appear here once their agent syncs (within the last 5 minutes).
                    </p>
                </Card>
            ) : (
                <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
                    {users.map((u) => {
                        const idleMins = u.idleTimeMs / 60000;
                        const activeNow = idleMins < 3;
                        return (
                            <Card key={`${u.userId}-${u.deviceName}`} className="p-5">
                                <div className="flex items-start justify-between">
                                    <div className="flex items-center gap-3">
                                        <div className="relative">
                                            <div className="w-11 h-11 bg-primary-100 rounded-full flex items-center justify-center">
                                                <span className="text-primary-700 font-semibold">
                                                    {u.userFullName?.charAt(0)?.toUpperCase() || '?'}
                                                </span>
                                            </div>
                                            <span className={`absolute bottom-0 right-0 w-3 h-3 rounded-full border-2 border-white ${activeNow ? 'bg-green-500' : 'bg-yellow-400'}`} />
                                        </div>
                                        <div>
                                            <p className="font-medium text-dark-900">{u.userFullName}</p>
                                            <p className="text-xs text-dark-400 flex items-center gap-1">
                                                <Monitor className="w-3 h-3" /> {u.deviceName}
                                            </p>
                                        </div>
                                    </div>
                                    <span className={`text-xs px-2 py-1 rounded-full ${activeNow ? 'bg-green-100 text-green-700' : 'bg-yellow-100 text-yellow-700'}`}>
                                        {activeNow ? 'Active' : 'Idle'}
                                    </span>
                                </div>

                                <div className="mt-4 space-y-2">
                                    <div>
                                        <p className="text-xs text-dark-400">Current app</p>
                                        <p className="text-sm font-medium text-dark-800 truncate">{u.currentApp || 'Unknown'}</p>
                                    </div>
                                    {u.currentWindowTitle && (
                                        <p className="text-xs text-dark-500 truncate">{u.currentWindowTitle}</p>
                                    )}
                                    <div className="flex items-center justify-between text-xs text-dark-500 pt-2">
                                        <span className="flex items-center gap-1">
                                            <Clock className="w-3 h-3" /> {formatIdle(u.idleTimeMs)}
                                        </span>
                                        <span>seen {formatLastSeen(u.lastActivityAt)}</span>
                                    </div>
                                </div>
                            </Card>
                        );
                    })}
                </div>
            )}
        </div>
    );
}
