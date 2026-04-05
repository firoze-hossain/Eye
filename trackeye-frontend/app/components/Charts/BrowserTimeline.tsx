'use client';

import { useEffect, useState } from 'react';
import {
    ComposedChart,
    Line,
    Bar,
    XAxis,
    YAxis,
    CartesianGrid,
    Tooltip,
    Legend,
    ResponsiveContainer,
} from 'recharts';
import api, { BrowserActivity } from '@/app/api/client';
import { format } from 'date-fns';
import LoadingSpinner from '../Common/LoadingSpinner';

interface BrowserTimelineProps {
    selectedDate: Date;
}

export default function BrowserTimeline({ selectedDate }: BrowserTimelineProps) {
    const [data, setData] = useState<any[]>([]);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        fetchBrowserActivities();
    }, [selectedDate]);

    const fetchBrowserActivities = async () => {
        setLoading(true);
        try {
            const dateStr = format(selectedDate, 'yyyy-MM-dd');
            const activities: BrowserActivity[] = await api.get(`/stats/browser-activities?date=${dateStr}`);

            // Group by hour
            const hourlyData = new Array(24).fill(0).map((_, i) => ({
                hour: i,
                minutes: 0,
                sessions: 0,
                label: `${i.toString().padStart(2, '0')}:00`,
            }));

            activities.forEach((activity) => {
                const hour = new Date(activity.startTime).getHours();
                hourlyData[hour].minutes += activity.durationMs / 60000;
                hourlyData[hour].sessions++;
            });

            setData(hourlyData);
        } catch (error) {
            console.error('Failed to fetch browser activities:', error);
        } finally {
            setLoading(false);
        }
    };

    if (loading) return <LoadingSpinner />;

    const hasData = data.some(hour => hour.minutes > 0);

    if (!hasData) {
        return (
            <div className="bg-card rounded-xl p-6">
                <h3 className="text-lg font-semibold text-white mb-4">Browser Activity Timeline</h3>
                <div className="text-center py-12">
                    <div className="text-gray-500">No browser activity recorded for this date</div>
                </div>
            </div>
        );
    }

    return (
        <div className="bg-card rounded-xl p-6">
            <h3 className="text-lg font-semibold text-white mb-4">Browser Activity Timeline</h3>
            <div className="h-80">
                <ResponsiveContainer width="100%" height="100%">
                    <ComposedChart data={data}>
                        <defs>
                            <linearGradient id="browserGradient" x1="0" y1="0" x2="0" y2="1">
                                <stop offset="5%" stopColor="#F59E0B" stopOpacity={0.3} />
                                <stop offset="95%" stopColor="#F59E0B" stopOpacity={0} />
                            </linearGradient>
                        </defs>
                        <CartesianGrid strokeDasharray="3 3" stroke="#333" />
                        <XAxis dataKey="label" stroke="#666" />
                        <YAxis yAxisId="left" stroke="#666" label={{ value: 'Minutes', angle: -90, position: 'insideLeft', fill: '#666' }} />
                        <YAxis yAxisId="right" orientation="right" stroke="#666" label={{ value: 'Sessions', angle: 90, position: 'insideRight', fill: '#666' }} />
                        <Tooltip
                            contentStyle={{ backgroundColor: '#2A2A2A', border: 'none', borderRadius: '8px' }}
                            labelStyle={{ color: '#fff' }}
                            formatter={(value: number, name: string) => {
                                if (name === 'minutes') return [`${value.toFixed(1)} minutes`, 'Browsing Time'];
                                return [`${value} sessions`, 'Browser Sessions'];
                            }}
                        />
                        <Legend />
                        <Bar yAxisId="left" dataKey="minutes" fill="#F59E0B" name="Browsing Time (minutes)" radius={[4, 4, 0, 0]} />
                        <Line yAxisId="right" type="monotone" dataKey="sessions" stroke="#1D9E75" name="Active Sessions" strokeWidth={2} dot={false} />
                    </ComposedChart>
                </ResponsiveContainer>
            </div>
        </div>
    );
}