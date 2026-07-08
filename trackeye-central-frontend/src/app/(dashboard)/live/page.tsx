// src/app/(dashboard)/live/page.tsx
'use client';

import { useQuery } from 'react-query';
import { apiClient, API } from '@/lib/api';
import Card from '@/components/ui/Card';
import LoadingSpinner from '@/components/common/LoadingSpinner';
import { Activity, Clock, Monitor, Circle, AppWindow } from 'lucide-react';

interface LiveUser {
    userId: number;
    userFullName: string;
    deviceName: string;
    currentApp: string;
    currentWindowTitle: string;
    lastActivityAt: number;
    isOnline: boolean;
    idleTimeMs: number;
}

interface ActivityRow {
    appName: string;
    windowTitle: string;
    processName: string;
    startTime: number;
    endTime: number;
    durationMs: number;
    userFullName: string;
    deviceName: string;
}

function formatDuration(ms: number) {
    const s = Math.floor(ms / 1000);
    const h = Math.floor(s / 3600);
    const m = Math.floor((s % 3600) / 60);
    const sec = s % 60;
    if (h > 0) return `${h}h ${m}m`;
    if (m > 0) return `${m}m ${sec}s`;
    return `${sec}s`;
}

function formatIdle(ms: number) {
    const mins = Math.floor(ms / 60000);
    if (mins < 1) return 'active now';
    if (mins < 60) return `${mins} min idle`;
    return `${Math.floor(mins / 60)}h ${mins % 60}m idle`;
}

function formatTime(ts: number) {
    return new Date(ts).toLocaleString([], {
        month: 'short', day: 'numeric',
        hour: '2-digit', minute: '2-digit', second: '2-digit',
    });
}

export default function LiveActivityPage() {
    const { data: liveUsers, isLoading: usersLoading, isFetching } = useQuery<LiveUser[]>(
        'live-activity',
        async () => apiClient.get<LiveUser[]>(API.dashboard.live),
        { refetchInterval: 10000 }
    );

    const { data: activities, isLoading: actsLoading } = useQuery<ActivityRow[]>(
        'activity-feed',
        async () => apiClient.get<ActivityRow[]>('/api/admin/activities?limit=300&hours=168'),
        { refetchInterval: 15000 }
    );

    if (usersLoading) return <LoadingSpinner />;

    const users = liveUsers || [];
    const rows = activities || [];

    return (
        <div className="space-y-8">
            <div className="flex items-center justify-between">
                <div>
                    <h1 className="text-2xl font-bold text-dark-900">Live Activity</h1>
                    <p className="text-dark-500 mt-1">Who's active now, and the full activity log</p>
                </div>
                <div className="flex items-center gap-2 text-sm text-dark-500">
                    <Circle className={`w-3 h-3 ${isFetching ? 'text-green-500 fill-green-500 animate-pulse' : 'text-dark-300 fill-dark-300'}`} />
                    Auto-refreshing
                </div>
            </div>

            {/* ---- Online now ---- */}
            <section>
                <h2 className="text-sm font-semibold text-dark-500 uppercase tracking-wide mb-3">
                    Online now ({users.length})
                </h2>
                {users.length === 0 ? (
                    <Card className="p-8 text-center">
                        <Activity className="w-8 h-8 text-dark-300 mx-auto mb-2" />
                        <p className="text-dark-500">No one is currently online.</p>
                    </Card>
                ) : (
                    <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
                        {users.map((u) => {
                            const activeNow = u.idleTimeMs / 60000 < 3;
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
                                    <div className="mt-4 space-y-1">
                                        <p className="text-xs text-dark-400">Current app</p>
                                        <p className="text-sm font-medium text-dark-800 truncate">{u.currentApp || 'Unknown'}</p>
                                        {u.currentWindowTitle && (
                                            <p className="text-xs text-dark-500 truncate">{u.currentWindowTitle}</p>
                                        )}
                                        <div className="flex items-center gap-1 text-xs text-dark-500 pt-2">
                                            <Clock className="w-3 h-3" /> {formatIdle(u.idleTimeMs)}
                                        </div>
                                    </div>
                                </Card>
                            );
                        })}
                    </div>
                )}
            </section>

            {/* ---- Full activity list ---- */}
            <section>
                <h2 className="text-sm font-semibold text-dark-500 uppercase tracking-wide mb-3 flex items-center gap-2">
                    <AppWindow className="w-4 h-4" /> Activity log ({rows.length})
                </h2>
                <Card className="overflow-hidden">
                    {actsLoading ? (
                        <div className="p-8"><LoadingSpinner /></div>
                    ) : rows.length === 0 ? (
                        <div className="p-8 text-center text-dark-500">No activity recorded yet.</div>
                    ) : (
                        <div className="overflow-x-auto">
                            <table className="w-full text-sm">
                                <thead>
                                    <tr className="text-left text-dark-500 border-b border-dark-100 bg-dark-50">
                                        <th className="px-4 py-3 font-medium">App</th>
                                        <th className="px-4 py-3 font-medium">Window</th>
                                        <th className="px-4 py-3 font-medium">User</th>
                                        <th className="px-4 py-3 font-medium">Device</th>
                                        <th className="px-4 py-3 font-medium">Duration</th>
                                        <th className="px-4 py-3 font-medium">Started</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    {rows.map((r, i) => (
                                        <tr key={i} className="border-b border-dark-50 hover:bg-dark-50/60">
                                            <td className="px-4 py-2.5 font-medium text-dark-800 whitespace-nowrap">{r.appName}</td>
                                            <td className="px-4 py-2.5 text-dark-600 max-w-xs truncate" title={r.windowTitle}>{r.windowTitle || '—'}</td>
                                            <td className="px-4 py-2.5 text-dark-600 whitespace-nowrap">{r.userFullName}</td>
                                            <td className="px-4 py-2.5 text-dark-500 whitespace-nowrap">{r.deviceName}</td>
                                            <td className="px-4 py-2.5 text-dark-700 whitespace-nowrap">{formatDuration(r.durationMs)}</td>
                                            <td className="px-4 py-2.5 text-dark-500 whitespace-nowrap">{formatTime(r.startTime)}</td>
                                        </tr>
                                    ))}
                                </tbody>
                            </table>
                        </div>
                    )}
                </Card>
            </section>
        </div>
    );
}
