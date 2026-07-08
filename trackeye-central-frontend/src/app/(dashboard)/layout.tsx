// src/app/(dashboard)/layout.tsx
'use client';

import { useEffect, useState } from 'react';
import { useAuth } from '../../context/AuthContext';
import { useRouter } from 'next/navigation';
import Sidebar from '../../components/common/Sidebar';
import Header from '../../components/common/Header';
import LoadingSpinner from '../../components/common/LoadingSpinner';

export default function DashboardLayout({
    children,
}: {
    children: React.ReactNode;
}) {
    const { user, isLoading } = useAuth();
    const router = useRouter();
    const [sidebarOpen, setSidebarOpen] = useState(true);

    // FIX: the old code called router.push('/login') during render, which React
    // warns about ("cannot update a component while rendering another") and can
    // loop. Navigation belongs in an effect.
    useEffect(() => {
        if (!isLoading && !user) {
            router.replace('/login');
        }
    }, [isLoading, user, router]);

    if (isLoading || !user) {
        return <LoadingSpinner />;
    }

    return (
        <div className="flex h-screen bg-dark-50">
            <Sidebar sidebarOpen={sidebarOpen} setSidebarOpen={setSidebarOpen} />
            {/* Sidebar is position:fixed and always shown on lg, so offset the content. */}
            <div className="flex-1 flex flex-col overflow-hidden lg:ml-64">
                <Header sidebarOpen={sidebarOpen} setSidebarOpen={setSidebarOpen} />
                <main className="flex-1 overflow-y-auto p-6">
                    {children}
                </main>
            </div>
        </div>
    );
}
