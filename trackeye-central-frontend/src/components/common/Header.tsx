// src/components/common/Header.tsx
'use client';

import { useAuth } from '@/context/AuthContext';
import { Menu, Bell, User, LogOut } from 'lucide-react';
import { useState } from 'react';
import Image from 'next/image';

interface HeaderProps {
    sidebarOpen: boolean;
    setSidebarOpen: (open: boolean) => void;
}

export default function Header({ sidebarOpen, setSidebarOpen }: HeaderProps) {
    const { user, logout } = useAuth();
    const [showDropdown, setShowDropdown] = useState(false);

    return (
        <header className="bg-white border-b border-dark-200 h-16 flex items-center justify-between px-6">
            <button
                onClick={() => setSidebarOpen(!sidebarOpen)}
                className="p-2 rounded-lg hover:bg-dark-100 lg:hidden"
            >
                <Menu className="w-5 h-5 text-dark-600" />
            </button>

            <div className="flex items-center gap-4">
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
                            <User className="w-4 h-4 text-primary-600" />
                        </div>
                        <div className="hidden md:block text-left">
                            <p className="text-sm font-medium text-dark-900">{user?.fullName || 'User'}</p>
                            <p className="text-xs text-dark-500 capitalize">{user?.role || 'Employee'}</p>
                        </div>
                    </button>

                    {showDropdown && (
                        <>
                            <div
                                className="fixed inset-0 z-10"
                                onClick={() => setShowDropdown(false)}
                            />
                            <div className="absolute right-0 mt-2 w-48 bg-white rounded-lg shadow-lg border border-dark-200 z-20">
                                <button
                                    onClick={() => {
                                        setShowDropdown(false);
                                        logout();
                                    }}
                                    className="w-full flex items-center gap-3 px-4 py-3 text-sm text-dark-700 hover:bg-dark-50 rounded-lg"
                                >
                                    <LogOut className="w-4 h-4" />
                                    Sign Out
                                </button>
                            </div>
                        </>
                    )}
                </div>
            </div>
        </header>
    );
}