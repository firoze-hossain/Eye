// src/app/(dashboard)/employees/[id]/page.tsx
'use client';

import { useState } from 'react';
import { useParams, useRouter } from 'next/navigation';
import { useQuery } from 'react-query';
import { ArrowLeft, Monitor, Clock, Coffee, Camera, Activity } from 'lucide-react';
import Card from '@/components/ui/Card';
import LoadingSpinner from '@/components/common/LoadingSpinner';
import AuthenticatedImage from '@/components/common/AuthenticatedImage';
import { apiClient, API } from '@/lib/api';

interface DeviceRow {
    id: number;
    deviceName: string;
    deviceIdentifier: string;
    osType: string;
    lastSeenAt: number;
    isActive: boolean;
}
interface UserDetail {
    id: number;
    email: string;
    fullName: string;
    role: string;
    status: string;
    devices: DeviceRow[];
}
interface ActivityItem {
    appName: string;
    windowTitle: string;
    startTime: number;
    endTime: number;
    durationMs: number;
}
interface ActivityResponse {
    date: string;
    totalMinutes: number;
    totalActiveMinutes: number;
    totalAfkMinutes: number;
    activities: ActivityItem[];
    topApps: Record<string, any>[];
}

const fmtDur = (ms: number) => {
    const s = Math.floor(ms / 1000);
    const h = Math.floor(s / 3600);
    const m = Math.floor((s % 3600) / 60);
    if (h > 0) return `${h}h ${m}m`;
    if (m > 0) return `${m}m ${s % 60}s`;
    return `${s}s`;
};
const fmtMin = (min: number) => {
    const h = Math.floor(min / 60);
    return h > 0 ? `${h}h ${min % 60}m` : `${min}m`;
};
const fmtTime = (ts: number) =>
    new Date(ts).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });

export default function EmployeeDetailsPage() {
    const params = useParams();
    const router = useRouter();
    const userId = Number(params.id);
    const [date, setDate] = useState(new Date().toISOString().split('T')[0]);

    const { data: user, isLoading: userLoading } = useQuery<UserDetail>(
        ['employee', userId],
        () => apiClient.get<UserDetail>(API.employees.details(userId))
    );

    const { data: activity, isLoading: actLoading } = useQuery<ActivityResponse>(
        ['employee-activities', userId, date],
        () => apiClient.get<ActivityResponse>(API.employees.activities(userId, date))
    );

    const { data: shots } = useQuery<any[]>(
        ['employee-screenshots', userId, date],
        () => apiClient.get<any[]>(API.employees.screenshots(userId, date))
    );

    if (userLoading) return <LoadingSpinner />;
    if (!user) return <Card className="p-8 text-center text-dark-500">Employee not found.</Card>;

    const stats = [
        { label: 'Total tracked', value: fmtMin(activity?.totalMinutes || 0), icon: Clock, color: 'bg-blue-100 text-blue-600' },
        { label: 'Active', value: fmtMin(activity?.totalActiveMinutes || 0), icon: Activity, color: 'bg-green-100 text-green-600' },
        { label: 'Idle / AFK', value: fmtMin(activity?.totalAfkMinutes || 0), icon: Coffee, color: 'bg-yellow-100 text-yellow-600' },
        { label: 'Screenshots', value: String(shots?.length || 0), icon: Camera, color: 'bg-purple-100 text-purple-600' },
    ];

    return (
        <div className="space-y-6">
            <button
                onClick={() => router.push('/employees')}
                className="flex items-center gap-1.5 text-sm text-dark-500 hover:text-dark-800"
            >
                <ArrowLeft className="w-4 h-4" /> Back to employees
            </button>

            <Card className="p-6">
                <div className="flex flex-wrap items-start justify-between gap-4">
                    <div className="flex items-center gap-4">
                        <div className="w-14 h-14 bg-primary-100 rounded-full flex items-center justify-center">
                            <span className="text-primary-700 text-xl font-semibold">
                                {user.fullName?.charAt(0)?.toUpperCase()}
                            </span>
                        </div>
                        <div>
                            <h1 className="text-2xl font-bold text-dark-900">{user.fullName}</h1>
                            <p className="text-dark-500">{user.email}</p>
                            <div className="flex gap-2 mt-2">
                                <span className="text-xs px-2 py-0.5 rounded-full bg-dark-100 text-dark-600 capitalize">{user.role}</span>
                                <span className={`text-xs px-2 py-0.5 rounded-full ${user.status === 'active' ? 'bg-green-100 text-green-700' : 'bg-dark-100 text-dark-500'}`}>
                                    {user.status}
                                </span>
                            </div>
                        </div>
                    </div>
                    <div>
                        <label className="label">Date</label>
                        <input type="date" value={date} onChange={(e) => setDate(e.target.value)} className="input" />
                    </div>
                </div>
            </Card>

            <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
                {stats.map((s) => (
                    <Card key={s.label} className="p-5 flex items-center justify-between">
                        <div>
                            <p className="text-sm text-dark-500">{s.label}</p>
                            <p className="text-2xl font-bold text-dark-900 mt-1">{s.value}</p>
                        </div>
                        <div className={`w-11 h-11 rounded-xl flex items-center justify-center ${s.color}`}>
                            <s.icon className="w-5 h-5" />
                        </div>
                    </Card>
                ))}
            </div>

            <Card className="p-6">
                <h2 className="font-semibold text-dark-900 mb-4 flex items-center gap-2">
                    <Monitor className="w-4 h-4" /> Devices ({user.devices?.length || 0})
                </h2>
                {!user.devices?.length ? (
                    <p className="text-dark-500 text-sm">No devices connected. Generate a device token from the Employees page.</p>
                ) : (
                    <div className="space-y-2">
                        {user.devices.map((d) => (
                            <div key={d.id} className="flex items-center justify-between p-3 border border-dark-100 rounded-lg">
                                <div>
                                    <p className="font-medium text-dark-800">{d.deviceName}</p>
                                    <p className="text-xs text-dark-400">{d.osType} · {d.deviceIdentifier}</p>
                                </div>
                                <div className="text-right">
                                    <span className={`text-xs px-2 py-0.5 rounded-full ${d.isActive ? 'bg-green-100 text-green-700' : 'bg-dark-100 text-dark-500'}`}>
                                        {d.isActive ? 'Active' : 'Revoked'}
                                    </span>
                                    <p className="text-xs text-dark-400 mt-1">
                                        {d.lastSeenAt ? `Seen ${new Date(d.lastSeenAt).toLocaleString()}` : 'Never seen'}
                                    </p>
                                </div>
                            </div>
                        ))}
                    </div>
                )}
            </Card>

            <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
                <Card className="lg:col-span-2 overflow-hidden">
                    <div className="p-5 border-b border-dark-100">
                        <h2 className="font-semibold text-dark-900">Activity timeline — {date}</h2>
                    </div>
                    {actLoading ? (
                        <div className="p-8"><LoadingSpinner /></div>
                    ) : !activity?.activities?.length ? (
                        <p className="p-8 text-center text-dark-500">No activity recorded on this date.</p>
                    ) : (
                        <div className="max-h-[420px] overflow-y-auto">
                            <table className="w-full text-sm">
                                <thead className="bg-dark-50 sticky top-0">
                                    <tr className="text-left text-dark-500">
                                        <th className="px-4 py-2.5 font-medium">App</th>
                                        <th className="px-4 py-2.5 font-medium">Window</th>
                                        <th className="px-4 py-2.5 font-medium">Time</th>
                                        <th className="px-4 py-2.5 font-medium">Duration</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    {activity.activities.map((a, i) => (
                                        <tr key={i} className="border-b border-dark-50">
                                            <td className="px-4 py-2 font-medium text-dark-800 whitespace-nowrap">{a.appName}</td>
                                            <td className="px-4 py-2 text-dark-600 max-w-[240px] truncate" title={a.windowTitle}>{a.windowTitle || '—'}</td>
                                            <td className="px-4 py-2 text-dark-500 whitespace-nowrap">{fmtTime(a.startTime)}</td>
                                            <td className="px-4 py-2 text-dark-700 whitespace-nowrap">{fmtDur(a.durationMs)}</td>
                                        </tr>
                                    ))}
                                </tbody>
                            </table>
                        </div>
                    )}
                </Card>

                <Card className="p-5">
                    <h2 className="font-semibold text-dark-900 mb-4">Top apps</h2>
                    {!activity?.topApps?.length ? (
                        <p className="text-dark-500 text-sm">No data.</p>
                    ) : (
                        <div className="space-y-3">
                            {activity.topApps.slice(0, 8).map((app: any, i: number) => {
                                const name = app.appName ?? app.app_name ?? 'Unknown';
                                const ms = Number(app.totalMs ?? app.total_ms ?? 0);
                                const first = activity.topApps[0] as any;
                                const max = Number(first?.totalMs ?? first?.total_ms ?? 1);
                                const pct = max > 0 ? (ms / max) * 100 : 0;
                                return (
                                    <div key={i}>
                                        <div className="flex justify-between text-sm mb-1">
                                            <span className="text-dark-700 truncate">{name}</span>
                                            <span className="text-dark-500 flex-none ml-2">{fmtDur(ms)}</span>
                                        </div>
                                        <div className="h-1.5 bg-dark-100 rounded-full overflow-hidden">
                                            <div className="h-full bg-primary-500 rounded-full" style={{ width: `${pct}%` }} />
                                        </div>
                                    </div>
                                );
                            })}
                        </div>
                    )}
                </Card>
            </div>

            <Card className="p-5">
                <h2 className="font-semibold text-dark-900 mb-4 flex items-center gap-2">
                    <Camera className="w-4 h-4" /> Screenshots — {date}
                </h2>
                {!shots?.length ? (
                    <p className="text-dark-500 text-sm">No screenshots for this date.</p>
                ) : (
                    <div className="grid grid-cols-2 md:grid-cols-4 gap-3">
                        {shots.map((s: any) => (
                            <div key={s.id} className="rounded-lg overflow-hidden border border-dark-100">
                                <AuthenticatedImage
                                    url={API.screenshots.image(s.id)}
                                    alt={s.windowTitle || 'Screenshot'}
                                    className="w-full aspect-video object-cover"
                                />
                                <p className="text-xs text-dark-400 p-2 truncate">{fmtTime(s.timestamp)} · {s.windowTitle || '—'}</p>
                            </div>
                        ))}
                    </div>
                )}
            </Card>
        </div>
    );
}
