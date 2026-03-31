'use client';

import { useEffect, useState } from 'react';
import { Clock, Monitor, Coffee, TrendingUp } from 'lucide-react';
import api, { StatsResponse } from '@/app/api/client';
import { format } from 'date-fns';
import LoadingSpinner from '../Common/LoadingSpinner';

interface StatsCardsProps {
  selectedDate: Date;
}

export default function StatsCards({ selectedDate }: StatsCardsProps) {
  const [stats, setStats] = useState<StatsResponse | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    fetchStats();
  }, [selectedDate]);

  const fetchStats = async () => {
    setLoading(true);
    try {
      const dateStr = format(selectedDate, 'yyyy-MM-dd');
      const data = await api.get(`/stats/date/${dateStr}`);
      setStats(data);
    } catch (error) {
      console.error('Failed to fetch stats:', error);
    } finally {
      setLoading(false);
    }
  };

  const cards = [
    {
      title: 'Total Time',
      value: stats ? `${stats.totalHours}h ${stats.totalMinutes % 60}m` : '0h 0m',
      icon: Clock,
      color: 'bg-blue-500/10 text-blue-500',
    },
    {
      title: 'Active Apps',
      value: stats?.topApps.length.toString() || '0',
      icon: Monitor,
      color: 'bg-green-500/10 text-green-500',
    },
    {
      title: 'Idle Time',
      value: '0h 0m',
      icon: Coffee,
      color: 'bg-yellow-500/10 text-yellow-500',
    },
    {
      title: 'Productivity',
      value: '85%',
      icon: TrendingUp,
      color: 'bg-purple-500/10 text-purple-500',
    },
  ];

  if (loading) {
    return (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
          {[1, 2, 3, 4].map((i) => (
              <div key={i} className="bg-card rounded-xl p-6 animate-pulse">
                <div className="h-4 bg-gray-700 rounded w-1/2 mb-4"></div>
                <div className="h-8 bg-gray-700 rounded w-3/4"></div>
              </div>
          ))}
        </div>
    );
  }

  return (
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
        {cards.map((card) => {
          const Icon = card.icon;
          return (
              <div key={card.title} className="bg-card rounded-xl p-6 hover:shadow-lg transition-shadow">
                <div className="flex items-center justify-between mb-4">
                  <span className="text-gray-400 text-sm">{card.title}</span>
                  <div className={`p-2 rounded-lg ${card.color}`}>
                    <Icon size={18} />
                  </div>
                </div>
                <div className="text-2xl font-bold text-white">{card.value}</div>
              </div>
          );
        })}
      </div>
  );
}