'use client';

import { useEffect, useState } from 'react';
import {
    BarChart,
    Bar,
    XAxis,
    YAxis,
    CartesianGrid,
    Tooltip,
    ResponsiveContainer,
    Cell,
} from 'recharts';
import api, { StatsResponse } from '@/app/api/client';
import { format } from 'date-fns';
import LoadingSpinner from '../Common/LoadingSpinner';

interface TopAppsChartProps {
    selectedDate: Date;
}

const COLORS = ['#1D9E75', '#2E8B57', '#3D7B47', '#4C6B37', '#5B5B27', '#6A4B17'];

export default function TopAppsChart({ selectedDate }: TopAppsChartProps) {
    const [data, setData] = useState<any[]>([]);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        fetchTopApps();
    }, [selectedDate]);

    const fetchTopApps = async () => {
        setLoading(true);
        try {
            const dateStr = format(selectedDate, 'yyyy-MM-dd');
            const stats: StatsResponse = await api.get(`/stats/date/${dateStr}`);

            const chartData = stats.topApps.map((app) => ({
                name: app.appName.length > 15 ? app.appName.substring(0, 12) + '...' : app.appName,
                minutes: app.totalMs / 60000,
                fullName: app.appName,
                sessions: app.sessions,
            }));

            setData(chartData);
        } catch (error) {
            console.error('Failed to fetch top apps:', error);
        } finally {
            setLoading(false);
        }
    };

    if (loading) return <LoadingSpinner />;

    return (
        <div className="bg-card rounded-xl p-6">
            <h3 className="text-lg font-semibold text-white mb-4">Top Applications</h3>
            <div className="h-80">
                <ResponsiveContainer width="100%" height="100%">
                    <BarChart data={data} layout="vertical" margin={{ left: 40 }}>
                        <CartesianGrid strokeDasharray="3 3" stroke="#333" horizontal={false} />
                        <XAxis type="number" stroke="#666" unit=" min" />
                        <YAxis type="category" dataKey="name" stroke="#666" width={100} />
                        <Tooltip
                            contentStyle={{ backgroundColor: '#2A2A2A', border: 'none', borderRadius: '8px' }}
                            labelStyle={{ color: '#fff' }}
                            formatter={(value: number, name: string, props: any) => [
                                `${value.toFixed(1)} minutes (${props.payload.sessions} sessions)`,
                                props.payload.fullName,
                            ]}
                        />
                        <Bar dataKey="minutes" radius={[0, 4, 4, 0]}>
                            {data.map((entry, index) => (
                                <Cell key={`cell-${index}`} fill={COLORS[index % COLORS.length]} />
                            ))}
                        </Bar>
                    </BarChart>
                </ResponsiveContainer>
            </div>
        </div>
    );
}