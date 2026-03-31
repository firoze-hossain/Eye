'use client';

import { useState } from 'react';
import Sidebar from '@/app/components/Layout/Sidebar';
import Header from '@/app/components/Layout/Header';
import DatePicker from '@/app/components/Common/DatePicker';
import LoadingSpinner from '@/app/components/Common/LoadingSpinner';
import api, { StatsResponse } from '@/app/api/client';
import { format, subDays } from 'date-fns';
import { Download, Calendar, TrendingUp, Clock, Monitor } from 'lucide-react';

export default function ReportsPage() {
    const [dateRange, setDateRange] = useState<'today' | 'week' | 'month'>('today');
    const [stats, setStats] = useState<StatsResponse | null>(null);
    const [loading, setLoading] = useState(true);

    const fetchReport = async () => {
        setLoading(true);
        try {
            const dateStr = format(new Date(), 'yyyy-MM-dd');
            const data = await api.get(`/stats/date/${dateStr}`);
            setStats(data);
        } catch (error) {
            console.error('Failed to fetch report:', error);
        } finally {
            setLoading(false);
        }
    };

    useState(() => {
        fetchReport();
    }, [dateRange]);

    const handleExport = () => {
        if (!stats) return;

        const csvData = [
            ['Date', stats.date],
            ['Total Hours', stats.totalHours],
            ['Total Minutes', stats.totalMinutes],
            ['Total Seconds', stats.totalSeconds],
            [''],
            ['Top Applications'],
            ['Application', 'Minutes', 'Sessions'],
            ...stats.topApps.map(app => [app.appName, (app.totalMs / 60000).toFixed(1), app.sessions]),
        ];

        const csvContent = csvData.map(row => row.join(',')).join('\n');
        const blob = new Blob([csvContent], { type: 'text/csv' });
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `trackeye-report-${stats.date}.csv`;
        a.click();
        URL.revokeObjectURL(url);
    };

    const rangeButtons = [
        { value: 'today', label: 'Today' },
        { value: 'week', label: 'This Week' },
        { value: 'month', label: 'This Month' },
    ];

    return (
        <div className="flex h-screen bg-dark">
            <Sidebar />
            <div className="flex-1 flex flex-col overflow-hidden">
                <Header title="Reports" />
                <main className="flex-1 overflow-y-auto p-6">
                    <div className="max-w-7xl mx-auto space-y-6">
                        {/* Controls */}
                        <div className="flex items-center justify-between">
                            <div className="flex space-x-2">
                                {rangeButtons.map((btn) => (
                                    <button
                                        key={btn.value}
                                        onClick={() => setDateRange(btn.value as any)}
                                        className={`px-4 py-2 rounded-lg transition-colors ${
                                            dateRange === btn.value
                                                ? 'bg-primary text-white'
                                                : 'bg-card text-gray-400 hover:bg-gray-800'
                                        }`}
                                    >
                                        {btn.label}
                                    </button>
                                ))}
                            </div>

                            <button
                                onClick={handleExport}
                                className="flex items-center space-x-2 px-4 py-2 bg-primary text-white rounded-lg hover:bg-primary/90 transition-colors"
                            >
                                <Download size={18} />
                                <span>Export Report</span>
                            </button>
                        </div>

                        {/* Stats Summary */}
                        {loading ? (
                            <LoadingSpinner />
                        ) : stats ? (
                            <>
                                <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
                                    <div className="bg-card rounded-xl p-6">
                                        <div className="flex items-center justify-between mb-4">
                                            <Clock className="text-primary" size={24} />
                                            <span className="text-3xl font-bold text-white">{stats.totalHours}h</span>
                                        </div>
                                        <p className="text-gray-400">Total Active Time</p>
                                        <p className="text-sm text-gray-500 mt-2">≈ {stats.totalMinutes} minutes</p>
                                    </div>

                                    <div className="bg-card rounded-xl p-6">
                                        <div className="flex items-center justify-between mb-4">
                                            <Monitor className="text-primary" size={24} />
                                            <span className="text-3xl font-bold text-white">{stats.topApps.length}</span>
                                        </div>
                                        <p className="text-gray-400">Applications Used</p>
                                        <p className="text-sm text-gray-500 mt-2">Different apps tracked</p>
                                    </div>

                                    <div className="bg-card rounded-xl p-6">
                                        <div className="flex items-center justify-between mb-4">
                                            <TrendingUp className="text-primary" size={24} />
                                            <span className="text-3xl font-bold text-white">
                        {stats.topApps[0]?.appName?.substring(0, 10) || 'N/A'}
                      </span>
                                        </div>
                                        <p className="text-gray-400">Most Used App</p>
                                        <p className="text-sm text-gray-500 mt-2">
                                            {stats.topApps[0] ? `${(stats.topApps[0].totalMs / 60000).toFixed(1)} minutes` : 'No data'}
                                        </p>
                                    </div>
                                </div>

                                {/* Top Apps Detailed */}
                                <div className="bg-card rounded-xl p-6">
                                    <h3 className="text-lg font-semibold text-white mb-4">Application Usage Breakdown</h3>
                                    <div className="space-y-4">
                                        {stats.topApps.map((app, index) => (
                                            <div key={app.appName}>
                                                <div className="flex justify-between mb-1">
                                                    <span className="text-gray-300">{index + 1}. {app.appName}</span>
                                                    <span className="text-primary">{(app.totalMs / 60000).toFixed(1)} min</span>
                                                </div>
                                                <div className="w-full bg-gray-700 rounded-full h-2">
                                                    <div
                                                        className="bg-primary rounded-full h-2 transition-all duration-500"
                                                        style={{
                                                            width: `${(app.totalMs / (stats.topApps[0]?.totalMs || 1)) * 100}%`,
                                                        }}
                                                    />
                                                </div>
                                                <p className="text-xs text-gray-500 mt-1">{app.sessions} sessions</p>
                                            </div>
                                        ))}
                                    </div>
                                </div>
                            </>
                        ) : (
                            <div className="text-center py-12">
                                <p className="text-gray-500">No data available for the selected period</p>
                            </div>
                        )}
                    </div>
                </main>
            </div>
        </div>
    );
}