// src/app/(dashboard)/dashboard/page.tsx
'use client';

import { useEffect, useState } from 'react';
import { useQuery } from 'react-query';
import { API, apiClient } from '@/lib/api';
import { DashboardStats } from '@/types';
import StatsCards from '@/components/dashboard/StatsCards';
import ActivityChart from '@/components/dashboard/ActivityChart';
import TopAppsTable from '@/components/dashboard/TopAppsTable';
import OnlineUsersList from '@/components/dashboard/OnlineUsersList';
import ProductivityGauge from '@/components/dashboard/ProductivityGauge';
import LoadingSpinner from '@/components/common/LoadingSpinner';
import toast from 'react-hot-toast';

export default function DashboardPage() {
    const [date, setDate] = useState(new Date().toISOString().split('T')[0]);

    const { data: stats, isLoading, error } = useQuery(
        ['dashboard', date],
        async () => {
            const response = await apiClient.get<DashboardStats>(API.dashboard.stats);
            return response;
        },
        {
            refetchInterval: 30000, // Refresh every 30 seconds
        }
    );

    if (isLoading) return <LoadingSpinner />;
    if (error) {
        toast.error('Failed to load dashboard data');
        return <div>Error loading dashboard</div>;
    }

    return (
        <div className="space-y-6">
            {/* Page Header */}
            <div>
                <h1 className="text-2xl font-bold text-dark-900">Dashboard</h1>
                <p className="text-dark-500 mt-1">Real-time activity monitoring overview</p>
            </div>

            {/* Stats Cards */}
            {stats && <StatsCards stats={stats.summary} />}

            {/* Productivity Gauge */}
            <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
                <div className="lg:col-span-1">
                    {stats && <ProductivityGauge score={stats.productivityScore} />}
                </div>
                <div className="lg:col-span-2">
                    <OnlineUsersList users={stats?.onlineUsers || []} />
                </div>
            </div>

            {/* Activity Chart */}
            <ActivityChart />

            {/* Top Apps */}
            <TopAppsTable apps={stats?.topActivities || []} />
        </div>
    );
}