'use client';

import { useState, useEffect } from 'react';
import Sidebar from '@/app/components/Layout/Sidebar';
import Header from '@/app/components/Layout/Header';
import LoadingSpinner from '@/app/components/Common/LoadingSpinner';
import api, { BrowserActivity, TopWebsite } from '@/app/api/client';
import { format } from 'date-fns';
import {
    Globe,
    Clock,
    ExternalLink,
    Search,
    Filter,
    TrendingUp,
    Calendar,
    Chrome,
    Compass
} from 'lucide-react';

export default function BrowserPage() {
    const [selectedDate, setSelectedDate] = useState<Date>(new Date());
    const [activities, setActivities] = useState<BrowserActivity[]>([]);
    const [topSites, setTopSites] = useState<TopWebsite[]>([]);
    const [loading, setLoading] = useState(true);
    const [searchTerm, setSearchTerm] = useState('');
    const [selectedBrowser, setSelectedBrowser] = useState<string>('all');

    useEffect(() => {
        fetchBrowserData();
    }, [selectedDate]);

    const fetchBrowserData = async () => {
        setLoading(true);
        try {
            const dateStr = format(selectedDate, 'yyyy-MM-dd');
            const [activitiesData, topSitesData] = await Promise.all([
                api.get(`/stats/browser-activities?date=${dateStr}`),
                api.get(`/stats/top-websites?limit=20&date=${dateStr}`)
            ]);
            setActivities(activitiesData);
            setTopSites(topSitesData);
        } catch (error) {
            console.error('Failed to fetch browser data:', error);
        } finally {
            setLoading(false);
        }
    };

    const handleDateChange = (date: Date) => {
        setSelectedDate(date);
    };

    const formatDuration = (ms: number) => {
        const minutes = Math.floor(ms / 60000);
        const seconds = Math.floor((ms % 60000) / 1000);
        return `${minutes}m ${seconds}s`;
    };

    const formatTime = (timestamp: number) => {
        return format(timestamp, 'h:mm a');
    };

    const getBrowserIcon = (browserName: string) => {
        const name = browserName.toLowerCase();
        if (name.includes('chrome')) return '🌐';
        if (name.includes('firefox')) return '🦊';
        if (name.includes('brave')) return '🦁';
        if (name.includes('edge')) return '🔷';
        if (name.includes('opera')) return '🎭';
        return '🌍';
    };

    const getBrowserColor = (browserName: string) => {
        const name = browserName.toLowerCase();
        if (name.includes('chrome')) return 'text-green-500';
        if (name.includes('firefox')) return 'text-orange-500';
        if (name.includes('brave')) return 'text-red-500';
        if (name.includes('edge')) return 'text-blue-500';
        if (name.includes('opera')) return 'text-red-400';
        return 'text-gray-400';
    };

    const filteredActivities = activities.filter(activity => {
        const matchesSearch = activity.url.toLowerCase().includes(searchTerm.toLowerCase()) ||
            activity.pageTitle.toLowerCase().includes(searchTerm.toLowerCase());
        const matchesBrowser = selectedBrowser === 'all' ||
            activity.browserName.toLowerCase().includes(selectedBrowser.toLowerCase());
        return matchesSearch && matchesBrowser;
    });

    const uniqueBrowsers = Array.from(new Set(activities.map(a => a.browserName)));

    const totalBrowsingTime = activities.reduce((sum, a) => sum + a.durationMs, 0);
    const totalMinutes = Math.floor(totalBrowsingTime / 60000);
    const totalHours = Math.floor(totalMinutes / 60);
    const remainingMinutes = totalMinutes % 60;

    return (
        <div className="flex h-screen bg-dark">
            <Sidebar />
            <div className="flex-1 flex flex-col overflow-hidden">
                <Header title="Browser Activity" onDateChange={handleDateChange} />
                <main className="flex-1 overflow-y-auto p-6">
                    <div className="max-w-7xl mx-auto space-y-6">
                        {/* Stats Overview */}
                        <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
                            <div className="bg-card rounded-xl p-6">
                                <div className="flex items-center justify-between mb-4">
                                    <Globe className="text-primary" size={24} />
                                    <span className="text-2xl font-bold text-white">{topSites.length}</span>
                                </div>
                                <p className="text-gray-400">Unique Websites Visited</p>
                                <p className="text-xs text-gray-500 mt-2">Tracked websites</p>
                            </div>

                            <div className="bg-card rounded-xl p-6">
                                <div className="flex items-center justify-between mb-4">
                                    <Clock className="text-primary" size={24} />
                                    <span className="text-2xl font-bold text-white">
                                        {totalHours > 0 ? `${totalHours}h ${remainingMinutes}m` : `${totalMinutes}m`}
                                    </span>
                                </div>
                                <p className="text-gray-400">Total Browsing Time</p>
                                <p className="text-xs text-gray-500 mt-2">Active browsing</p>
                            </div>

                            <div className="bg-card rounded-xl p-6">
                                <div className="flex items-center justify-between mb-4">
                                    <TrendingUp className="text-primary" size={24} />
                                    <span className="text-2xl font-bold text-white">{activities.length}</span>
                                </div>
                                <p className="text-gray-400">Browser Sessions</p>
                                <p className="text-xs text-gray-500 mt-2">Total browsing sessions</p>
                            </div>
                        </div>

                        {/* Top Websites Section */}
                        <div className="bg-card rounded-xl p-6">
                            <div className="flex items-center justify-between mb-6">
                                <h3 className="text-lg font-semibold text-white">Top Websites</h3>
                                <div className="flex items-center space-x-2">
                                    <Search size={16} className="text-gray-500" />
                                    <input
                                        type="text"
                                        placeholder="Search websites..."
                                        value={searchTerm}
                                        onChange={(e) => setSearchTerm(e.target.value)}
                                        className="bg-gray-800 text-white px-3 py-1 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-primary/50"
                                    />
                                </div>
                            </div>

                            <div className="space-y-4">
                                {topSites.slice(0, 10).map((site, index) => {
                                    const totalTime = Math.floor(site.totalMinutes);
                                    const hours = Math.floor(totalTime / 60);
                                    const minutes = totalTime % 60;

                                    return (
                                        <div key={site.url} className="group">
                                            <div className="flex items-center justify-between mb-1">
                                                <div className="flex items-center space-x-3 flex-1 min-w-0">
                                                    <span className="text-sm text-gray-500 w-6">{index + 1}</span>
                                                    <img
                                                        src={`https://www.google.com/s2/favicons?domain=${new URL(site.url).hostname}&sz=24`}
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
                                                        className="text-gray-300 hover:text-primary truncate flex-1 transition-colors"
                                                    >
                                                        {new URL(site.url).hostname.replace('www.', '')}
                                                    </a>
                                                    <ExternalLink size={14} className="text-gray-600 opacity-0 group-hover:opacity-100 transition-opacity" />
                                                </div>
                                                <div className="flex items-center space-x-4">
                                                    <span className="text-xs text-gray-500">
                                                        {site.visitCount} visit{site.visitCount !== 1 ? 's' : ''}
                                                    </span>
                                                    <span className="text-sm text-primary font-mono">
                                                        {hours > 0 ? `${hours}h ${minutes}m` : `${minutes}m`}
                                                    </span>
                                                </div>
                                            </div>
                                            <div className="w-full bg-gray-700 rounded-full h-1.5">
                                                <div
                                                    className="bg-primary rounded-full h-1.5 transition-all duration-500"
                                                    style={{
                                                        width: `${(site.totalMinutes / (topSites[0]?.totalMinutes || 1)) * 100}%`,
                                                    }}
                                                />
                                            </div>
                                        </div>
                                    );
                                })}
                            </div>
                        </div>

                        {/* Detailed Activity Log */}
                        <div className="bg-card rounded-xl overflow-hidden">
                            <div className="p-6 border-b border-gray-800">
                                <div className="flex items-center justify-between">
                                    <h3 className="text-lg font-semibold text-white">Detailed Browser Activity</h3>
                                    <div className="flex items-center space-x-2">
                                        <Filter size={16} className="text-gray-500" />
                                        <select
                                            value={selectedBrowser}
                                            onChange={(e) => setSelectedBrowser(e.target.value)}
                                            className="bg-gray-800 text-white px-3 py-1 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-primary/50"
                                        >
                                            <option value="all">All Browsers</option>
                                            {uniqueBrowsers.map(browser => (
                                                <option key={browser} value={browser}>{browser}</option>
                                            ))}
                                        </select>
                                    </div>
                                </div>
                            </div>

                            {loading ? (
                                <div className="p-8 text-center">
                                    <LoadingSpinner />
                                </div>
                            ) : filteredActivities.length === 0 ? (
                                <div className="p-8 text-center">
                                    <Compass size={48} className="mx-auto text-gray-600 mb-3" />
                                    <p className="text-gray-500">No browser activity found</p>
                                    <p className="text-xs text-gray-600 mt-1">Browse websites to see activity here</p>
                                </div>
                            ) : (
                                <div className="overflow-x-auto">
                                    <table className="w-full">
                                        <thead className="bg-darker border-b border-gray-800">
                                        <tr>
                                            <th className="text-left p-4 text-gray-400 font-medium">Browser</th>
                                            <th className="text-left p-4 text-gray-400 font-medium">Website</th>
                                            <th className="text-left p-4 text-gray-400 font-medium">Page Title</th>
                                            <th className="text-left p-4 text-gray-400 font-medium">Start Time</th>
                                            <th className="text-left p-4 text-gray-400 font-medium">Duration</th>
                                        </tr>
                                        </thead>
                                        <tbody>
                                        {filteredActivities.map((activity) => (
                                            <tr key={activity.id} className="border-b border-gray-800 hover:bg-gray-800/50 transition-colors">
                                                <td className="p-4">
                                                    <div className="flex items-center space-x-2">
                                                            <span className={getBrowserColor(activity.browserName)}>
                                                                {getBrowserIcon(activity.browserName)}
                                                            </span>
                                                        <span className="text-white text-sm">{activity.browserName}</span>
                                                    </div>
                                                </td>
                                                <td className="p-4">
                                                    <a
                                                        href={activity.url}
                                                        target="_blank"
                                                        rel="noopener noreferrer"
                                                        className="text-primary hover:underline text-sm flex items-center space-x-1"
                                                    >
                                                        <span>{new URL(activity.url).hostname.replace('www.', '')}</span>
                                                        <ExternalLink size={12} />
                                                    </a>
                                                </td>
                                                <td className="p-4">
                                                    <div className="text-gray-300 text-sm truncate max-w-md">
                                                        {activity.pageTitle || 'No title'}
                                                    </div>
                                                </td>
                                                <td className="p-4 text-gray-400 text-sm">
                                                    {formatTime(activity.startTime)}
                                                </td>
                                                <td className="p-4">
                                                        <span className="text-primary font-mono text-sm">
                                                            {formatDuration(activity.durationMs)}
                                                        </span>
                                                </td>
                                            </tr>
                                        ))}
                                        </tbody>
                                    </table>
                                </div>
                            )}
                        </div>
                    </div>
                </main>
            </div>
        </div>
    );
}