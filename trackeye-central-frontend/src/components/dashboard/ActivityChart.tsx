// src/components/dashboard/ActivityChart.tsx
'use client';

import { useMemo } from 'react';
import { useQuery } from 'react-query';
import {
    Area,
    AreaChart,
    XAxis,
    YAxis,
    CartesianGrid,
    Tooltip,
    ResponsiveContainer,
} from 'recharts';
import Card from '../ui/Card';
import { Calendar } from 'lucide-react';
import { apiClient } from '../../lib/api';

interface FeedRow {
    startTime: number;
    durationMs: number;
}

export default function ActivityChart() {
    // Real data: last 24h of activity from the org feed, bucketed by hour.
    const { data: rows } = useQuery<FeedRow[]>(
        'activity-chart',
        async () => apiClient.get<FeedRow[]>('/api/admin/activities?limit=1000&hours=24'),
        { refetchInterval: 60000 }
    );

    const data = useMemo(() => {
        const buckets = Array.from({ length: 24 }, (_, h) => ({
            hour: `${h}:00`,
            minutes: 0,
        }));
        (rows || []).forEach((r) => {
            const h = new Date(r.startTime).getHours();
            buckets[h].minutes += Math.round((r.durationMs || 0) / 60000);
        });
        return buckets;
    }, [rows]);

    const hasData = (rows?.length || 0) > 0;

    return (
        <Card>
            <div className="p-6">
                <div className="flex items-center justify-between mb-6">
                    <div>
                        <h3 className="text-lg font-semibold text-dark-900">Activity Overview</h3>
                        <p className="text-sm text-dark-500 mt-1">Tracked minutes per hour (last 24h)</p>
                    </div>
                    <button className="flex items-center gap-2 px-3 py-2 text-sm text-dark-600 hover:bg-dark-100 rounded-lg">
                        <Calendar className="w-4 h-4" />
                        Today
                    </button>
                </div>

                {!hasData ? (
                    <div className="h-[300px] flex items-center justify-center text-dark-400 text-sm">
                        No activity recorded in the last 24 hours yet.
                    </div>
                ) : (
                    <ResponsiveContainer width="100%" height={360}>
                        <AreaChart data={data}>
                            <defs>
                                <linearGradient id="colorActivity" x1="0" y1="0" x2="0" y2="1">
                                    <stop offset="5%" stopColor="#3b82f6" stopOpacity={0.3} />
                                    <stop offset="95%" stopColor="#3b82f6" stopOpacity={0} />
                                </linearGradient>
                            </defs>
                            <CartesianGrid strokeDasharray="3 3" stroke="#e5e7eb" />
                            <XAxis dataKey="hour" stroke="#6b7280" interval={2} />
                            <YAxis stroke="#6b7280" />
                            <Tooltip
                                contentStyle={{ backgroundColor: '#fff', border: '1px solid #e5e7eb', borderRadius: '8px' }}
                                formatter={(v: any) => [`${v} min`, 'Tracked']}
                            />
                            <Area type="monotone" dataKey="minutes" stroke="#3b82f6" fill="url(#colorActivity)" name="Tracked minutes" />
                        </AreaChart>
                    </ResponsiveContainer>
                )}
            </div>
        </Card>
    );
}
