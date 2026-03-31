'use client';

import { useEffect, useState } from 'react';
import {
    AreaChart,
    Area,
    XAxis,
    YAxis,
    CartesianGrid,
    Tooltip,
    ResponsiveContainer,
} from 'recharts';
import api, { ActivitySession } from '@/app/api/client';
import { format } from 'date-fns';
import LoadingSpinner from '../Common/LoadingSpinner';

interface ActivityChartProps {
    selectedDate: Date;
}

export default function ActivityChart({ selectedDate }: ActivityChartProps) {
    const [data, setData] = useState<any[]>([]);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        fetchActivity();
    }, [selectedDate]);

    const fetchActivity = async () => {
        setLoading(true);
        try {
            const dateStr = format(selectedDate, 'yyyy-MM-dd');
            const sessions: ActivitySession[] = await api.get(`/sessions?date=${dateStr}`);

            // Group by hour
            const hourlyData = new Array(24).fill(0).map((_, i) => ({
                hour: i,
                minutes: 0,
                label: `${i.toString().padStart(2, '0')}:00`,
            }));

            sessions.forEach((session) => {
                const hour = new Date(session.startTime).getHours();
                hourlyData[hour].minutes += session.durationMs / 60000;
            });

            setData(hourlyData);
        } catch (error) {
            console.error('Failed to fetch activity:', error);
        } finally {
            setLoading(false);
        }
    };

    if (loading) return <LoadingSpinner />;

    return (
        <div className="bg-card rounded-xl p-6">
            <h3 className="text-lg font-semibold text-white mb-4">Activity Timeline</h3>
            <div className="h-80">
                <ResponsiveContainer width="100%" height="100%">
                    <AreaChart data={data}>
                        <defs>
                            <linearGradient id="colorMinutes" x1="0" y1="0" x2="0" y2="1">
                                <stop offset="5%" stopColor="#1D9E75" stopOpacity={0.3} />
                                <stop offset="95%" stopColor="#1D9E75" stopOpacity={0} />
                            </linearGradient>
                        </defs>
                        <CartesianGrid strokeDasharray="3 3" stroke="#333" />
                        <XAxis dataKey="label" stroke="#666" />
                        <YAxis stroke="#666" label={{ value: 'Minutes', angle: -90, position: 'insideLeft', fill: '#666' }} />
                        <Tooltip
                            contentStyle={{ backgroundColor: '#2A2A2A', border: 'none', borderRadius: '8px' }}
                            labelStyle={{ color: '#fff' }}
                            formatter={(value: number) => [`${value.toFixed(1)} minutes`, 'Activity']}
                        />
                        <Area
                            type="monotone"
                            dataKey="minutes"
                            stroke="#1D9E75"
                            fill="url(#colorMinutes)"
                            strokeWidth={2}
                        />
                    </AreaChart>
                </ResponsiveContainer>
            </div>
        </div>
    );
}