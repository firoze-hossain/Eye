'use client';

import { useState } from 'react';
import Sidebar from '@/app/components/Layout/Sidebar';
import Header from '@/app/components/Layout/Header';
import StatsCards from '@/app/components/Layout/StatsCards';
import ActivityChart from '@/app/components/Charts/ActivityChart';
import TopAppsChart from '@/app/components/Charts/TopAppsChart';
import TimelineChart from '@/app/components/Charts/TimelineChart';
import { format, subDays } from 'date-fns';

export default function DashboardPage() {
    const [selectedDate, setSelectedDate] = useState<Date>(new Date());

    return (
        <div className="flex h-screen bg-dark">
            <Sidebar />
            <div className="flex-1 flex flex-col overflow-hidden">
                <Header title="Dashboard" />
                <main className="flex-1 overflow-y-auto p-6">
                    <div className="max-w-7xl mx-auto space-y-6">
                        {/* Stats Cards */}
                        <StatsCards selectedDate={selectedDate} />

                        {/* Charts Grid */}
                        <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
                            <ActivityChart selectedDate={selectedDate} />
                            <TopAppsChart selectedDate={selectedDate} />
                        </div>

                        {/* Timeline */}
                        <TimelineChart selectedDate={selectedDate} />
                    </div>
                </main>
            </div>
        </div>
    );
}