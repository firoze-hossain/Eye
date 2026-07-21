// src/app/(dashboard)/layout.tsx
'use client';

import { useEffect, useState, useRef } from 'react';
import { useAuth } from '../../context/AuthContext';
import { useRouter } from 'next/navigation';
import { getSession } from 'next-auth/react';
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
    const recheckedRef = useRef(false);

    // FIX: on a hard reload, useSession() can occasionally settle on
    // "unauthenticated" for a moment before a background refetch confirms the
    // (perfectly valid) session cookie - a known NextAuth v4 timing quirk. The
    // old code redirected to /login on the FIRST instant there was no user,
    // with no chance for that self-correction to happen - by the time it would
    // have corrected itself, the browser had already navigated away. Now, the
    // first time this looks unauthenticated, double-check with a fresh
    // getSession() call before actually redirecting.
    useEffect(() => {
        if (isLoading || user) {
            recheckedRef.current = false;
            return;
        }
        if (recheckedRef.current) {
            router.replace('/login');
            return;
        }
        recheckedRef.current = true;
        getSession().then((s) => {
            if (!s?.accessToken) {
                router.replace('/login');
            }
            // else: a real session exists after all - AuthContext's own
            // useSession() subscription will pick it up and populate `user`
            // on its own; nothing else to do here.
        });
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
