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
import api, { ActivitySession } from '@/app/api/client';
import { format } from 'date-fns';
import LoadingSpinner from '../Common/LoadingSpinner';

interface TimelineChartProps {
    selectedDate: Date;
}

export default function TimelineChart({ selectedDate }: TimelineChartProps) {
    const [data, setData] = useState<any[]>([]);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        fetchTimeline();
    }, [selectedDate]);

    const fetchTimeline = async () => {
        setLoading(true);
        try {
            const dateStr = format(selectedDate, 'yyyy-MM-dd');
            const sessions: ActivitySession[] = await api.get(`/sessions?date=${dateStr}`);

            // Create 30-minute intervals
            const intervals: any[] = [];
            for (let i = 0; i < 48; i++) {
                intervals.push({
                    time: i,
                    label: `${Math.floor(i / 2)}:${i % 2 === 0 ? '00' : '30'}`,
                    duration: 0,
                    count: 0,
                });
            }

            sessions.forEach((session) => {
                const minutes = new Date(session.startTime).getHours() * 60 + new Date(session.startTime).getMinutes();
                const interval = Math.floor(minutes / 30);
                if (intervals[interval]) {
                    intervals[interval].duration += session.durationMs / 60000;
                    intervals[interval].count++;
                }
            });

            setData(intervals.slice(0, 24)); // Show only first 12 hours for better visibility
        } catch (error) {
            console.error('Failed to fetch timeline:', error);
        } finally {
            setLoading(false);
        }
    };

    if (loading) return <LoadingSpinner />;

    return (
        <div className="bg-card rounded-xl p-6">
            <h3 className="text-lg font-semibold text-white mb-4">Detailed Timeline</h3>
            <div className="h-96">
                <ResponsiveContainer width="100%" height="100%">
                    <ComposedChart data={data}>
                        <CartesianGrid strokeDasharray="3 3" stroke="#333" />
                        <XAxis dataKey="label" stroke="#666" interval={3} />
                        <YAxis yAxisId="left" stroke="#666" label={{ value: 'Minutes', angle: -90, position: 'insideLeft', fill: '#666' }} />
                        <YAxis yAxisId="right" orientation="right" stroke="#666" label={{ value: 'Sessions', angle: 90, position: 'insideRight', fill: '#666' }} />
                        <Tooltip
                            contentStyle={{ backgroundColor: '#2A2A2A', border: 'none', borderRadius: '8px' }}
                            labelStyle={{ color: '#fff' }}
                        />
                        <Legend />
                        <Bar yAxisId="left" dataKey="duration" fill="#1D9E75" name="Activity (minutes)" radius={[4, 4, 0, 0]} />
                        <Line yAxisId="right" type="monotone" dataKey="count" stroke="#F59E0B" name="Session Count" strokeWidth={2} dot={false} />
                    </ComposedChart>
                </ResponsiveContainer>
            </div>
        </div>
    );
}