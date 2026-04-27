// src/components/dashboard/ActivityChart.tsx
'use client';

import { useState, useEffect } from 'react';
import {
    LineChart,
    Line,
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

const generateMockData = () => {
    const data = [];
    for (let i = 0; i < 24; i++) {
        data.push({
            hour: `${i}:00`,
            activity: Math.floor(Math.random() * 100),
            productivity: Math.floor(Math.random() * 100),
        });
    }
    return data;
};

export default function ActivityChart() {
    const [data, setData] = useState(generateMockData());

    useEffect(() => {
        // In production, fetch real data from API
        const interval = setInterval(() => {
            setData(generateMockData());
        }, 60000);
        return () => clearInterval(interval);
    }, []);

    return (
        <Card>
            <div className="p-6">
                <div className="flex items-center justify-between mb-6">
                    <div>
                        <h3 className="text-lg font-semibold text-dark-900">Activity Overview</h3>
                        <p className="text-sm text-dark-500 mt-1">24-hour activity pattern</p>
                    </div>
                    <button className="flex items-center gap-2 px-3 py-2 text-sm text-dark-600 hover:bg-dark-100 rounded-lg">
                        <Calendar className="w-4 h-4" />
                        Today
                    </button>
                </div>

                <ResponsiveContainer width="100%" height={400}>
                    <AreaChart data={data}>
                        <defs>
                            <linearGradient id="colorActivity" x1="0" y1="0" x2="0" y2="1">
                                <stop offset="5%" stopColor="#3b82f6" stopOpacity={0.3} />
                                <stop offset="95%" stopColor="#3b82f6" stopOpacity={0} />
                            </linearGradient>
                            <linearGradient id="colorProductivity" x1="0" y1="0" x2="0" y2="1">
                                <stop offset="5%" stopColor="#10b981" stopOpacity={0.3} />
                                <stop offset="95%" stopColor="#10b981" stopOpacity={0} />
                            </linearGradient>
                        </defs>
                        <CartesianGrid strokeDasharray="3 3" stroke="#e5e7eb" />
                        <XAxis dataKey="hour" stroke="#6b7280" />
                        <YAxis stroke="#6b7280" />
                        <Tooltip
                            contentStyle={{
                                backgroundColor: '#fff',
                                border: '1px solid #e5e7eb',
                                borderRadius: '8px',
                            }}
                        />
                        <Area
                            type="monotone"
                            dataKey="activity"
                            stroke="#3b82f6"
                            fill="url(#colorActivity)"
                            name="Activity Level"
                        />
                        <Area
                            type="monotone"
                            dataKey="productivity"
                            stroke="#10b981"
                            fill="url(#colorProductivity)"
                            name="Productivity"
                        />
                    </AreaChart>
                </ResponsiveContainer>
            </div>
        </Card>
    );
}