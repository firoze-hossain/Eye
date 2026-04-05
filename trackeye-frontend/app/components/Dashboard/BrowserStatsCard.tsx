'use client';

import { useEffect, useState } from 'react';
import { Globe, Clock, ExternalLink, TrendingUp } from 'lucide-react';
import api, { TopWebsite } from '@/app/api/client';
import { format } from 'date-fns';
import LoadingSpinner from '../Common/LoadingSpinner';

interface BrowserStatsCardProps {
    selectedDate: Date;
}

export default function BrowserStatsCard({ selectedDate }: BrowserStatsCardProps) {
    const [topSites, setTopSites] = useState<TopWebsite[]>([]);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        fetchTopWebsites();
    }, [selectedDate]);

    const fetchTopWebsites = async () => {
        setLoading(true);
        try {
            const dateStr = format(selectedDate, 'yyyy-MM-dd');
            const data = await api.get(`/stats/top-websites?limit=5&date=${dateStr}`);
            setTopSites(data);
        } catch (error) {
            console.error('Failed to fetch top websites:', error);
        } finally {
            setLoading(false);
        }
    };

    const formatDuration = (minutes: number) => {
        if (minutes < 60) return `${minutes.toFixed(1)} min`;
        const hours = Math.floor(minutes / 60);
        const mins = minutes % 60;
        return `${hours}h ${mins.toFixed(0)}m`;
    };

    const getFaviconUrl = (url: string) => {
        try {
            const domain = new URL(url).hostname;
            return `https://www.google.com/s2/favicons?domain=${domain}&sz=32`;
        } catch {
            return '';
        }
    };

    if (loading) {
        return (
            <div className="bg-card rounded-xl p-6">
                <div className="flex items-center justify-between mb-4">
                    <h3 className="text-lg font-semibold text-white">Top Websites</h3>
                    <Globe size={20} className="text-primary" />
                </div>
                <LoadingSpinner />
            </div>
        );
    }

    return (
        <div className="bg-card rounded-xl p-6">
            <div className="flex items-center justify-between mb-4">
                <h3 className="text-lg font-semibold text-white">Top Websites</h3>
                <Globe size={20} className="text-primary" />
            </div>

            {topSites.length === 0 ? (
                <div className="text-center py-8">
                    <Globe size={40} className="mx-auto text-gray-600 mb-3" />
                    <p className="text-gray-500 text-sm">No browser activity tracked</p>
                    <p className="text-gray-600 text-xs mt-1">Browse websites to see stats</p>
                </div>
            ) : (
                <div className="space-y-4">
                    {topSites.map((site, index) => (
                        <div key={site.url} className="group">
                            <div className="flex items-center justify-between mb-1">
                                <div className="flex items-center space-x-2 flex-1 min-w-0">
                                    <span className="text-xs text-gray-500 w-5">{index + 1}</span>
                                    <img
                                        src={getFaviconUrl(site.url)}
                                        alt=""
                                        className="w-4 h-4"
                                        onError={(e) => {
                                            (e.target as HTMLImageElement).style.display = 'none';
                                        }}
                                    />
                                    <a
                                        href={site.url}
                                        target="_blank"
                                        rel="noopener noreferrer"
                                        className="text-sm text-gray-300 hover:text-primary truncate flex-1 transition-colors"
                                    >
                                        {new URL(site.url).hostname.replace('www.', '')}
                                    </a>
                                    <ExternalLink size={12} className="text-gray-600 opacity-0 group-hover:opacity-100 transition-opacity" />
                                </div>
                                <span className="text-sm text-primary font-mono">
                                    {formatDuration(site.totalMinutes)}
                                </span>
                            </div>
                            <div className="w-full bg-gray-700 rounded-full h-1.5 ml-7">
                                <div
                                    className="bg-primary rounded-full h-1.5 transition-all duration-500"
                                    style={{
                                        width: `${(site.totalMinutes / (topSites[0]?.totalMinutes || 1)) * 100}%`,
                                    }}
                                />
                            </div>
                            <div className="text-xs text-gray-500 ml-7 mt-1">
                                {site.visitCount} visit{site.visitCount !== 1 ? 's' : ''}
                            </div>
                        </div>
                    ))}
                </div>
            )}

            <div className="mt-4 pt-4 border-t border-gray-800">
                <div className="flex justify-between text-xs text-gray-500">
                    <span>Total tracked: {topSites.length} websites</span>
                    <span className="text-primary">Last 24 hours</span>
                </div>
            </div>
        </div>
    );
}