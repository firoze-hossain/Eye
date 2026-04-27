// src/app/(dashboard)/layout.tsx (Update this)
'use client';

import { useState } from 'react';
import {useAuth} from "../../context/AuthContext";
import { useRouter } from 'next/navigation';
//import Sidebar from '@/components/common/Sidebar';
import Sidebar from "../../components/common/Sidebar";
import Header from "../../components/common/Header";
import LoadingSpinner from "../../components/common/LoadingSpinner";

export default function DashboardLayout({
                                            children,
                                        }: {
    children: React.ReactNode;
}) {
    const { user, isLoading } = useAuth();
    const router = useRouter();
    const [sidebarOpen, setSidebarOpen] = useState(true);

    if (isLoading) {
        return <LoadingSpinner />;
    }

    if (!user) {
        router.push('/login');
        return null;
    }

    return (
        <div className="flex h-screen bg-dark-50">
            <Sidebar sidebarOpen={sidebarOpen} setSidebarOpen={setSidebarOpen} />
            <div className="flex-1 flex flex-col overflow-hidden">
                <Header sidebarOpen={sidebarOpen} setSidebarOpen={setSidebarOpen} />
                <main className="flex-1 overflow-y-auto p-6">
                    {children}
                </main>
            </div>
        </div>
    );
}