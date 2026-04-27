// src/components/dashboard/StatsCards.tsx
'use client';

import { Users, Monitor, Clock, Camera } from 'lucide-react';
import Card from '../ui/Card';

interface StatsCardsProps {
    stats: {
        totalActiveUsers: number;
        totalDevices: number;
        totalMinutesTracked: number;
        totalScreenshots: number;
        averageProductivity: number;
    };
}

export default function StatsCards({ stats }: StatsCardsProps) {
    const cards = [
        {
            title: 'Active Users',
            value: stats.totalActiveUsers,
            icon: Users,
            color: 'bg-blue-500',
            change: '+12%',
        },
        {
            title: 'Active Devices',
            value: stats.totalDevices,
            icon: Monitor,
            color: 'bg-green-500',
            change: '+5%',
        },
        {
            title: 'Hours Tracked',
            value: Math.floor(stats.totalMinutesTracked / 60),
            icon: Clock,
            color: 'bg-purple-500',
            change: '+18%',
        },
        {
            title: 'Screenshots',
            value: stats.totalScreenshots,
            icon: Camera,
            color: 'bg-orange-500',
            change: '+23%',
        },
    ];

    return (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
            {cards.map((card) => (
                <Card key={card.title} className="p-6">
                    <div className="flex items-center justify-between">
                        <div>
                            <p className="text-sm text-dark-500">{card.title}</p>
                            <p className="text-2xl font-bold text-dark-900 mt-1">{card.value}</p>
                            <p className="text-xs text-green-600 mt-1">{card.change} from last week</p>
                        </div>
                        <div className={`${card.color} p-3 rounded-lg`}>
                            <card.icon className="w-6 h-6 text-white" />
                        </div>
                    </div>
                </Card>
            ))}
        </div>
    );
}