// src/app/(dashboard)/reports/page.tsx
'use client';

import { useState } from 'react';
import { useQuery } from 'react-query';
import { Download, TrendingUp, Clock, Coffee, Camera } from 'lucide-react';
import Card from '@/components/ui/Card';
import LoadingSpinner from '@/components/common/LoadingSpinner';
import { apiClient, API } from '@/lib/api';
import toast from 'react-hot-toast';

interface DailySummary {
    date: string;
    totalMinutes: number;
    productiveMinutes: number;
    screenshotCount: number;
    productivityScore: number;
}
interface WeeklyReport {
    startDate: string;
    endDate: string;
    userFullName: string;
    dailySummaries: DailySummary[];
    totals: {
        totalMinutes: number;
        productiveMinutes: number;
        afkMinutes: number;
        totalScreenshots: number;
        averageProductivity: number;
    };
    topActivities: Record<string, any>[];
}

const fmtMin = (min: number) => {
    const h = Math.floor(min / 60);
    return h > 0 ? `${h}h ${min % 60}m` : `${min}m`;
};

export default function ReportsPage() {
    const [userId, setUserId] = useState<string>('');

    const { data: employees } = useQuery<any[]>('employees', () =>
        apiClient.get<any[]>(API.employees.list)
    );

    const { data: report, isLoading } = useQuery<WeeklyReport>(
        ['weekly-report', userId],
        () => apiClient.get<WeeklyReport>(API.reports.weekly(userId ? Number(userId) : undefined))
    );

    const exportCsv = () => {
        if (!report?.dailySummaries?.length) {
            toast.error('Nothing to export');
            return;
        }
        const header = 'Date,Total Minutes,Productive Minutes,Screenshots,Productivity %';
        const rows = report.dailySummaries.map(
            (d) => `${d.date},${d.totalMinutes},${d.productiveMinutes},${d.screenshotCount},${d.productivityScore.toFixed(1)}`
        );
        const csv = [header, ...rows].join('\n');
        const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' });
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `trackeye-report-${report.startDate}-to-${report.endDate}.csv`;
        a.click();
        URL.revokeObjectURL(url);
        toast.success('Report exported');
    };

    if (isLoading) return <LoadingSpinner />;

    const totals = report?.totals;
    const cards = [
        { label: 'Total tracked', value: fmtMin(totals?.totalMinutes || 0), icon: Clock, color: 'bg-blue-100 text-blue-600' },
        { label: 'Productive', value: fmtMin(totals?.productiveMinutes || 0), icon: TrendingUp, color: 'bg-green-100 text-green-600' },
        { label: 'Idle / AFK', value: fmtMin(totals?.afkMinutes || 0), icon: Coffee, color: 'bg-yellow-100 text-yellow-600' },
        { label: 'Screenshots', value: String(totals?.totalScreenshots || 0), icon: Camera, color: 'bg-purple-100 text-purple-600' },
    ];

    const maxDaily = Math.max(1, ...(report?.dailySummaries || []).map((d) => d.totalMinutes));

    return (
        <div className="space-y-6">
            <div className="flex flex-wrap items-center justify-between gap-4">
                <div>
                    <h1 className="text-2xl font-bold text-dark-900">Reports</h1>
                    <p className="text-dark-500 mt-1">
                        {report ? `${report.startDate} → ${report.endDate}` : 'Weekly productivity summary'}
                    </p>
                </div>
                <div className="flex items-end gap-3">
                    <div>
                        <label className="label">Employee</label>
                        <select value={userId} onChange={(e) => setUserId(e.target.value)} className="input">
                            <option value="">Whole organization</option>
                            {employees?.map((e) => (
                                <option key={e.id} value={e.id}>{e.fullName}</option>
                            ))}
                        </select>
                    </div>
                    <button onClick={exportCsv} className="btn-primary flex items-center gap-2 h-[42px]">
                        <Download className="w-4 h-4" /> Export CSV
                    </button>
                </div>
            </div>

            <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
                {cards.map((c) => (
                    <Card key={c.label} className="p-5 flex items-center justify-between">
                        <div>
                            <p className="text-sm text-dark-500">{c.label}</p>
                            <p className="text-2xl font-bold text-dark-900 mt-1">{c.value}</p>
                        </div>
                        <div className={`w-11 h-11 rounded-xl flex items-center justify-center ${c.color}`}>
                            <c.icon className="w-5 h-5" />
                        </div>
                    </Card>
                ))}
            </div>

            <Card className="p-6">
                <div className="flex items-center justify-between mb-6">
                    <h2 className="font-semibold text-dark-900">Daily breakdown</h2>
                    <span className="text-sm text-dark-500">
                        Avg productivity: {(totals?.averageProductivity || 0).toFixed(1)}%
                    </span>
                </div>

                {!report?.dailySummaries?.length ? (
                    <p className="text-center text-dark-500 py-8">No data for this period.</p>
                ) : (
                    <>
                        <div className="flex items-end gap-3 h-48 mb-6">
                            {report.dailySummaries.map((d) => (
                                <div key={d.date} className="flex-1 flex flex-col items-center gap-2">
                                    <div className="w-full flex-1 flex items-end">
                                        <div
                                            className="w-full bg-primary-500 rounded-t-md min-h-[2px] transition-all"
                                            style={{ height: `${(d.totalMinutes / maxDaily) * 100}%` }}
                                            title={`${fmtMin(d.totalMinutes)}`}
                                        />
                                    </div>
                                    <span className="text-xs text-dark-400">
                                        {new Date(d.date).toLocaleDateString([], { weekday: 'short' })}
                                    </span>
                                </div>
                            ))}
                        </div>

                        <div className="overflow-x-auto">
                            <table className="w-full text-sm">
                                <thead>
                                    <tr className="text-left text-dark-500 border-b border-dark-100">
                                        <th className="px-3 py-2 font-medium">Date</th>
                                        <th className="px-3 py-2 font-medium">Tracked</th>
                                        <th className="px-3 py-2 font-medium">Productive</th>
                                        <th className="px-3 py-2 font-medium">Screenshots</th>
                                        <th className="px-3 py-2 font-medium">Productivity</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    {report.dailySummaries.map((d) => (
                                        <tr key={d.date} className="border-b border-dark-50">
                                            <td className="px-3 py-2 text-dark-700">{d.date}</td>
                                            <td className="px-3 py-2 text-dark-600">{fmtMin(d.totalMinutes)}</td>
                                            <td className="px-3 py-2 text-dark-600">{fmtMin(d.productiveMinutes)}</td>
                                            <td className="px-3 py-2 text-dark-600">{d.screenshotCount}</td>
                                            <td className="px-3 py-2">
                                                <span className={`px-2 py-0.5 rounded-full text-xs ${
                                                    d.productivityScore >= 70 ? 'bg-green-100 text-green-700'
                                                    : d.productivityScore >= 40 ? 'bg-yellow-100 text-yellow-700'
                                                    : 'bg-red-100 text-red-700'
                                                }`}>
                                                    {d.productivityScore.toFixed(0)}%
                                                </span>
                                            </td>
                                        </tr>
                                    ))}
                                </tbody>
                            </table>
                        </div>
                    </>
                )}
            </Card>

            {!!report?.topActivities?.length && (
                <Card className="p-6">
                    <h2 className="font-semibold text-dark-900 mb-4">Top applications</h2>
                    <div className="space-y-3">
                        {report.topActivities.slice(0, 10).map((a: any, i: number) => {
                            const name = a.appName ?? a.app_name ?? 'Unknown';
                            const ms = Number(a.totalMs ?? a.total_ms ?? 0);
                            const first = report.topActivities[0] as any;
                            const max = Number(first?.totalMs ?? first?.total_ms ?? 1);
                            return (
                                <div key={i}>
                                    <div className="flex justify-between text-sm mb-1">
                                        <span className="text-dark-700">{name}</span>
                                        <span className="text-dark-500">{fmtMin(Math.round(ms / 60000))}</span>
                                    </div>
                                    <div className="h-1.5 bg-dark-100 rounded-full overflow-hidden">
                                        <div className="h-full bg-primary-500 rounded-full" style={{ width: `${(ms / max) * 100}%` }} />
                                    </div>
                                </div>
                            );
                        })}
                    </div>
                </Card>
            )}
        </div>
    );
}
