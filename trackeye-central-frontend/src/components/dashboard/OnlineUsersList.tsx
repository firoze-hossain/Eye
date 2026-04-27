// src/components/dashboard/OnlineUsersList.tsx
import { User, Clock } from 'lucide-react';
import Card from '../ui/Card';

interface OnlineUsersListProps {
    users: Array<{
        userId: number;
        fullName: string;
        currentApp: string;
        lastActivityAt: number;
        isActive: boolean;
    }>;
}

export default function OnlineUsersList({ users }: OnlineUsersListProps) {
    const formatLastActive = (timestamp: number) => {
        const minutes = Math.floor((Date.now() - timestamp) / 60000);
        if (minutes < 1) return 'Just now';
        if (minutes < 60) return `${minutes} min ago`;
        return `${Math.floor(minutes / 60)} hours ago`;
    };

    return (
        <Card>
            <div className="p-6">
                <h3 className="text-lg font-semibold text-dark-900 mb-4">Online Users</h3>
                <div className="space-y-4">
                    {users.map((user) => (
                        <div key={user.userId} className="flex items-center justify-between p-3 bg-dark-50 rounded-lg">
                            <div className="flex items-center gap-3">
                                <div className="relative">
                                    <div className="w-10 h-10 bg-primary-100 rounded-full flex items-center justify-center">
                                        <User className="w-5 h-5 text-primary-600" />
                                    </div>
                                    <div className={`absolute bottom-0 right-0 w-3 h-3 rounded-full border-2 border-white ${user.isActive ? 'bg-green-500' : 'bg-gray-400'}`} />
                                </div>
                                <div>
                                    <p className="font-medium text-dark-900">{user.fullName}</p>
                                    <p className="text-sm text-dark-500">{user.currentApp}</p>
                                </div>
                            </div>
                            <div className="flex items-center gap-2 text-sm text-dark-500">
                                <Clock className="w-4 h-4" />
                                <span>{formatLastActive(user.lastActivityAt)}</span>
                            </div>
                        </div>
                    ))}
                    {users.length === 0 && (
                        <p className="text-center text-dark-500 py-8">No users currently online</p>
                    )}
                </div>
            </div>
        </Card>
    );
}