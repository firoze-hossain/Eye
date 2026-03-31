'use client';

import { useState } from 'react';
import Sidebar from '@/app/components/Layout/Sidebar';
import Header from '@/app/components/Layout/Header';
import DatePicker from '@/app/components/Common/DatePicker';
import LoadingSpinner from '@/app/components/Common/LoadingSpinner';
import api, { ScreenshotRecord } from '@/app/api/client';
import { format } from 'date-fns';
import { Camera, Download, Maximize2 } from 'lucide-react';
import Image from 'next/image';

export default function ScreenshotsPage() {
    const [selectedDate, setSelectedDate] = useState<Date>(new Date());
    const [screenshots, setScreenshots] = useState<ScreenshotRecord[]>([]);
    const [loading, setLoading] = useState(true);
    const [selectedImage, setSelectedImage] = useState<ScreenshotRecord | null>(null);

    const fetchScreenshots = async (date: Date) => {
        setLoading(true);
        try {
            const dateStr = format(date, 'yyyy-MM-dd');
            const data = await api.get(`/screenshots?date=${dateStr}`);
            setScreenshots(data);
        } catch (error) {
            console.error('Failed to fetch screenshots:', error);
        } finally {
            setLoading(false);
        }
    };

    const handleDateChange = (date: Date) => {
        setSelectedDate(date);
        fetchScreenshots(date);
    };

    useState(() => {
        fetchScreenshots(selectedDate);
    }, []);

    return (
        <div className="flex h-screen bg-dark">
            <Sidebar />
            <div className="flex-1 flex flex-col overflow-hidden">
                <Header title="Screenshots" onDateChange={handleDateChange} />
                <main className="flex-1 overflow-y-auto p-6">
                    <div className="max-w-7xl mx-auto">
                        {loading ? (
                            <div className="flex justify-center py-12">
                                <LoadingSpinner />
                            </div>
                        ) : screenshots.length === 0 ? (
                            <div className="text-center py-12">
                                <Camera size={48} className="mx-auto text-gray-600 mb-4" />
                                <p className="text-gray-500">No screenshots captured for this date</p>
                            </div>
                        ) : (
                            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
                                {screenshots.map((screenshot) => (
                                    <div
                                        key={screenshot.id}
                                        className="bg-card rounded-xl overflow-hidden hover:shadow-lg transition-shadow cursor-pointer"
                                        onClick={() => setSelectedImage(screenshot)}
                                    >
                                        <div className="relative aspect-video bg-darker">
                                            <img
                                                src={`/api/screenshots/image?path=${encodeURIComponent(screenshot.filePath)}`}
                                                alt={`Screenshot at ${format(screenshot.timestamp, 'HH:mm:ss')}`}
                                                className="w-full h-full object-cover"
                                                onError={(e) => {
                                                    (e.target as HTMLImageElement).src = '/placeholder-image.png';
                                                }}
                                            />
                                            <div className="absolute top-2 right-2 bg-black/50 rounded-lg p-1">
                                                <Maximize2 size={14} className="text-white" />
                                            </div>
                                        </div>
                                        <div className="p-4">
                                            <div className="flex items-center justify-between mb-2">
                        <span className="text-sm text-primary">
                          {format(screenshot.timestamp, 'h:mm:ss a')}
                        </span>
                                                <button
                                                    onClick={(e) => {
                                                        e.stopPropagation();
                                                        window.open(`/api/screenshots/image?path=${encodeURIComponent(screenshot.filePath)}`, '_blank');
                                                    }}
                                                    className="text-gray-400 hover:text-white transition-colors"
                                                >
                                                    <Download size={16} />
                                                </button>
                                            </div>
                                            <p className="text-sm text-gray-400 truncate">{screenshot.windowTitle || screenshot.processName}</p>
                                        </div>
                                    </div>
                                ))}
                            </div>
                        )}
                    </div>
                </main>
            </div>

            {/* Image Modal */}
            {selectedImage && (
                <div
                    className="fixed inset-0 bg-black/90 z-50 flex items-center justify-center p-4"
                    onClick={() => setSelectedImage(null)}
                >
                    <div className="relative max-w-5xl w-full">
                        <img
                            src={`/api/screenshots/image?path=${encodeURIComponent(selectedImage.filePath)}`}
                            alt="Screenshot preview"
                            className="w-full h-auto rounded-lg"
                        />
                        <button
                            className="absolute top-4 right-4 bg-black/50 rounded-full p-2 hover:bg-black/70 transition-colors"
                            onClick={() => setSelectedImage(null)}
                        >
                            <svg className="w-6 h-6 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
                            </svg>
                        </button>
                        <div className="absolute bottom-4 left-4 right-4 bg-black/50 rounded-lg p-2 text-center text-white text-sm">
                            {format(selectedImage.timestamp, 'PPPpp')} - {selectedImage.windowTitle || selectedImage.processName}
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
}