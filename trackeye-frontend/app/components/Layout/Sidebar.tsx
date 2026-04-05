'use client';

import Link from 'next/link';
import { usePathname } from 'next/navigation';
import {
    LayoutDashboard,
    Clock,
    Camera,
    FileText,
    Settings,
    Activity,
    Globe
} from 'lucide-react';

const menuItems = [
    { icon: LayoutDashboard, label: 'Dashboard', href: '/dashboard' },
    { icon: Activity, label: 'Activity', href: '/dashboard/activity' },
    { icon: Globe, label: 'Browser', href: '/dashboard/browser' },  // New browser menu
    { icon: Camera, label: 'Screenshots', href: '/dashboard/screenshots' },
    { icon: FileText, label: 'Reports', href: '/dashboard/reports' },
    { icon: Settings, label: 'Settings', href: '/dashboard/settings' },
];

export default function Sidebar() {
    const pathname = usePathname();

    return (
        <aside className="w-64 bg-darker border-r border-gray-800 flex flex-col">
            <div className="p-6 border-b border-gray-800">
                <div className="flex items-center space-x-3">
                    <div className="w-8 h-8 bg-primary rounded-lg flex items-center justify-center">
                        <Activity size={18} className="text-white" />
                    </div>
                    <span className="text-xl font-bold text-white">TrackEye</span>
                </div>
            </div>

            <nav className="flex-1 p-4">
                {menuItems.map((item) => {
                    const Icon = item.icon;
                    const isActive = pathname === item.href;

                    return (
                        <Link
                            key={item.href}
                            href={item.href}
                            className={`flex items-center space-x-3 px-4 py-3 rounded-lg mb-1 transition-colors ${
                                isActive
                                    ? 'bg-primary/10 text-primary'
                                    : 'text-gray-400 hover:bg-gray-800 hover:text-white'
                            }`}
                        >
                            <Icon size={20} />
                            <span>{item.label}</span>
                        </Link>
                    );
                })}
            </nav>

            <div className="p-4 border-t border-gray-800">
                <div className="flex items-center space-x-3">
                    <div className="w-2 h-2 bg-green-500 rounded-full animate-pulse"></div>
                    <span className="text-sm text-gray-400">Tracking Active</span>
                </div>
            </div>
        </aside>
    );
}