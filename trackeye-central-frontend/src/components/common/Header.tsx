// src/components/common/Header.tsx
'use client';

import { useAuth } from '../../context/AuthContext';
import { Menu, Bell, User, LogOut, Settings, Loader2 } from 'lucide-react';
import { useState } from 'react';
import { useRouter } from 'next/navigation';

interface HeaderProps {
    sidebarOpen: boolean;
    setSidebarOpen: (open: boolean) => void;
}

export default function Header({ sidebarOpen, setSidebarOpen }: HeaderProps) {
    const { user, logout } = useAuth();
    const router = useRouter();
    const [showDropdown, setShowDropdown] = useState(false);
    const [signingOut, setSigningOut] = useState(false);

    const handleLogout = async () => {
        setSigningOut(true);
        try {
            await logout();   // clears cache + redirects to /login
        } catch {
            setSigningOut(false);
        }
    };

    const initial = user?.fullName?.charAt(0)?.toUpperCase();

    return (
        <header className="bg-white border-b border-dark-200 h-16 flex items-center justify-between px-6">
            <button
                onClick={() => setSidebarOpen(!sidebarOpen)}
                className="p-2 rounded-lg hover:bg-dark-100 lg:hidden"
            >
                <Menu className="w-5 h-5 text-dark-600" />
            </button>

            <div className="flex items-center gap-4 ml-auto">
                <button className="p-2 rounded-lg hover:bg-dark-100 relative">
                    <Bell className="w-5 h-5 text-dark-600" />
                    <span className="absolute top-1 right-1 w-2 h-2 bg-red-500 rounded-full"></span>
                </button>

                <div className="relative">
                    <button
                        onClick={() => setShowDropdown(!showDropdown)}
                        className="flex items-center gap-3 p-2 rounded-lg hover:bg-dark-100"
                    >
                        <div className="w-8 h-8 bg-primary-100 rounded-full flex items-center justify-center">
                            {initial
                                ? <span className="text-sm font-semibold text-primary-700">{initial}</span>
                                : <User className="w-4 h-4 text-primary-600" />}
                        </div>
                        <div className="hidden md:block text-left">
                            <p className="text-sm font-medium text-dark-900">{user?.fullName || 'User'}</p>
                            <p className="text-xs text-dark-500 capitalize">{user?.role || 'Employee'}</p>
                        </div>
                    </button>

                    {showDropdown && (
                        <>
                            <div className="fixed inset-0 z-10" onClick={() => setShowDropdown(false)} />
                            <div className="absolute right-0 mt-2 w-56 bg-white rounded-lg shadow-lg border border-dark-200 z-20 overflow-hidden">
                                <div className="px-4 py-3 border-b border-dark-100">
                                    <p className="text-sm font-medium text-dark-900 truncate">{user?.fullName}</p>
                                    <p className="text-xs text-dark-500 truncate">{user?.email}</p>
                                </div>

                                <button
                                    onClick={() => { setShowDropdown(false); router.push('/settings'); }}
                                    className="w-full flex items-center gap-3 px-4 py-3 text-sm text-dark-700 hover:bg-dark-50"
                                >
                                    <Settings className="w-4 h-4" />
                                    Settings
                                </button>

                                <button
                                    onClick={handleLogout}
                                    disabled={signingOut}
                                    className="w-full flex items-center gap-3 px-4 py-3 text-sm text-red-600 hover:bg-red-50 disabled:opacity-60"
                                >
                                    {signingOut
                                        ? <Loader2 className="w-4 h-4 animate-spin" />
                                        : <LogOut className="w-4 h-4" />}
                                    {signingOut ? 'Signing out…' : 'Sign Out'}
                                </button>
                            </div>
                        </>
                    )}
                </div>
            </div>
        </header>
    );
}
