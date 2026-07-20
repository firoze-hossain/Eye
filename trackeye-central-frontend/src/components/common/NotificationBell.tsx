// src/components/common/NotificationBell.tsx
'use client';

import { useState } from 'react';
import { useQuery, useQueryClient } from 'react-query';
import { Bell, ShieldAlert, Info } from 'lucide-react';
import { apiClient, API } from '../../lib/api';

interface NotificationItem {
    id: number;
    type: string;
    title: string;
    body: string;
    read: boolean;
    createdAt: number;
}

function timeAgo(ts: number) {
    const mins = Math.floor((Date.now() - ts) / 60000);
    if (mins < 1) return 'just now';
    if (mins < 60) return `${mins}m ago`;
    const hours = Math.floor(mins / 60);
    if (hours < 24) return `${hours}h ago`;
    return `${Math.floor(hours / 24)}d ago`;
}

export default function NotificationBell() {
    const [open, setOpen] = useState(false);
    const queryClient = useQueryClient();

    const { data: unread } = useQuery(
        'notif-unread',
        () => apiClient.get<{ count: number }>(API.notifications.unreadCount),
        { refetchInterval: 20000 }
    );

    const { data: items } = useQuery(
        'notif-list',
        () => apiClient.get<NotificationItem[]>(API.notifications.list),
        { enabled: open, refetchInterval: open ? 15000 : false }
    );

    const markAllRead = async () => {
        await apiClient.post(API.notifications.markAllRead);
        queryClient.invalidateQueries('notif-unread');
        queryClient.invalidateQueries('notif-list');
    };

    const count = unread?.count || 0;

    return (
        <div className="relative">
            <button onClick={() => setOpen(!open)} className="p-2 rounded-lg hover:bg-dark-100 relative">
                <Bell className="w-5 h-5 text-dark-600" />
                {count > 0 && (
                    <span className="absolute top-1 right-1 w-2 h-2 bg-red-500 rounded-full" />
                )}
            </button>

            {open && (
                <>
                    <div className="fixed inset-0 z-10" onClick={() => setOpen(false)} />
                    <div className="absolute right-0 mt-2 w-80 bg-white rounded-lg shadow-lg border border-dark-200 z-20 overflow-hidden">
                        <div className="px-4 py-3 border-b border-dark-100 flex items-center justify-between">
                            <p className="text-sm font-semibold text-dark-900">Notifications</p>
                            {count > 0 && (
                                <button onClick={markAllRead} className="text-xs text-primary-600 hover:underline">
                                    Mark all read
                                </button>
                            )}
                        </div>
                        <div className="max-h-96 overflow-y-auto">
                            {!items?.length ? (
                                <p className="text-sm text-dark-400 text-center py-8">No notifications yet.</p>
                            ) : (
                                items.map((n) => (
                                    <div
                                        key={n.id}
                                        className={`px-4 py-3 border-b border-dark-50 flex gap-3 ${n.read ? '' : 'bg-blue-50/50'}`}
                                    >
                                        <div className="flex-none mt-0.5">
                                            {n.type === 'POLICY_VIOLATION'
                                                ? <ShieldAlert className="w-4 h-4 text-amber-600" />
                                                : <Info className="w-4 h-4 text-dark-400" />}
                                        </div>
                                        <div className="min-w-0">
                                            <p className="text-sm font-medium text-dark-900">{n.title}</p>
                                            <p className="text-xs text-dark-500 mt-0.5">{n.body}</p>
                                            <p className="text-xs text-dark-300 mt-1">{timeAgo(n.createdAt)}</p>
                                        </div>
                                    </div>
                                ))
                            )}
                        </div>
                    </div>
                </>
            )}
        </div>
    );
}
