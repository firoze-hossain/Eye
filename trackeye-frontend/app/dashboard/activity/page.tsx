'use client';

import { useState } from 'react';
import Sidebar from '@/app/components/Layout/Sidebar';
import Header from '@/app/components/Layout/Header';
import DatePicker from '@/app/components/Common/DatePicker';
import LoadingSpinner from '@/app/components/Common/LoadingSpinner';
import api, { ActivitySession } from '@/app/api/client';
import { format } from 'date-fns';
import { Clock, Monitor, ExternalLink } from 'lucide-react';

export default function ActivityPage() {
    const [selectedDate, setSelectedDate] = useState<Date>(new Date());
    const [sessions, setSessions] = useState<ActivitySession[]>([]);
    const [loading, setLoading] = useState(true);

    const fetchSessions = async (date: Date) => {
        setLoading(true);
        try {
            const dateStr = format(date, 'yyyy-MM-dd');
            const data = await api.get(`/sessions?date=${dateStr}`);
            setSessions(data);
        } catch (error) {
            console.error('Failed to fetch sessions:', error);
        } finally {
            setLoading(false);
        }
    };

    const handleDateChange = (date: Date) => {
        setSelectedDate(date);
        fetchSessions(date);
    };

    useState(() => {
        fetchSessions(selectedDate);
    }, []);

    const formatDuration = (ms: number) => {
        const hours = Math.floor(ms / 3600000);
        const minutes = Math.floor((ms % 3600000) / 60000);
        const seconds = Math.floor((ms % 60000) / 1000);
        return `${hours}h ${minutes}m ${seconds}s`;
    };

    const formatTime = (timestamp: number) => {
        return format(timestamp, 'h:mm a');
    };

    return (
        <div className="flex h-screen bg-dark">
            <Sidebar />
            <div className="flex-1 flex flex-col overflow-hidden">
                <Header title="Activity Timeline" onDateChange={handleDateChange} />
                <main className="flex-1 overflow-y-auto p-6">
                    <div className="max-w-7xl mx-auto">
                        <div className="bg-card rounded-xl overflow-hidden">
                            <div className="overflow-x-auto">
                                <table className="w-full">
                                    <thead className="bg-darker border-b border-gray-800">
                                    <tr>
                                        <th className="text-left p-4 text-gray-400 font-medium">Application</th>
                                        <th className="text-left p-4 text-gray-400 font-medium">Window Title</th>
                                        <th className="text-left p-4 text-gray-400 font-medium">Start Time</th>
                                        <th className="text-left p-4 text-gray-400 font-medium">End Time</th>
                                        <th className="text-left p-4 text-gray-400 font-medium">Duration</th>
                                    </tr>
                                    </thead>
                                    <tbody>
                                    {loading ? (
                                        <tr>
                                            <td colSpan={5} className="p-8 text-center">
                                                <LoadingSpinner />
                                            </td>
                                        </tr>
                                    ) : sessions.length === 0 ? (
                                        <tr>
                                            <td colSpan={5} className="p-8 text-center text-gray-500">
                                                No activity recorded for this date
                                            </td>
                                        </tr>
                                    ) : (
                                        sessions.map((session) => (
                                            <tr key={session.id} className="border-b border-gray-800 hover:bg-gray-800/50 transition-colors">
                                                <td className="p-4">
                                                    <div className="flex items-center space-x-3">
                                                        <Monitor size={16} className="text-primary" />
                                                        <span className="text-white">{session.appName}</span>
                                                    </div>
                                                </td>
                                                <td className="p-4 text-gray-300">{session.windowTitle || '-'}</td>
                                                <td className="p-4 text-gray-300">{formatTime(session.startTime)}</td>
                                                <td className="p-4 text-gray-300">{formatTime(session.endTime)}</td>
                                                <td className="p-4">
                            <span className="text-primary font-mono">
                              {formatDuration(session.durationMs)}
                            </span>
                                                </td>
                                            </tr>
                                        ))
                                    )}
                                    </tbody>
                                </table>
                            </div>
                        </div>
                    </div>
                </main>
            </div>
        </div>
    );
}