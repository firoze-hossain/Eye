// src/app/(dashboard)/screenshots/page.tsx
'use client';

import { useState } from 'react';
import { useQuery } from 'react-query';
import { Calendar, Search, ZoomIn } from 'lucide-react';
import Card from '@/components/ui/Card';
import LoadingSpinner from '@/components/common/LoadingSpinner';
import { apiClient, API } from '@/lib/api';
import Image from 'next/image';
import Modal from '@/components/ui/Modal';

export default function ScreenshotsPage() {
    const [selectedDate, setSelectedDate] = useState(new Date().toISOString().split('T')[0]);
    const [selectedImage, setSelectedImage] = useState<string | null>(null);
    const [selectedUser, setSelectedUser] = useState<string | null>(null);

    const { data: screenshots, isLoading } = useQuery(
        ['screenshots', selectedDate],
        async () => {
            const response = await apiClient.get(API.screenshots.organization(selectedDate, 0, 100));
            return response;
        }
    );

    if (isLoading) return <LoadingSpinner />;

    return (
        <div className="space-y-6">
            <div>
                <h1 className="text-2xl font-bold text-dark-900">Screenshots</h1>
                <p className="text-dark-500 mt-1">View employee screenshots by date</p>
            </div>

            <Card className="p-4">
                <div className="flex gap-4">
                    <div className="flex-1">
                        <label className="label">Select Date</label>
                        <input
                            type="date"
                            value={selectedDate}
                            onChange={(e) => setSelectedDate(e.target.value)}
                            className="input"
                        />
                    </div>
                    <div className="flex-1">
                        <label className="label">Filter by User</label>
                        <select
                            value={selectedUser || ''}
                            onChange={(e) => setSelectedUser(e.target.value || null)}
                            className="input"
                        >
                            <option value="">All Users</option>
                            {screenshots?.userNames && Object.entries(screenshots.userNames).map(([id, name]) => (
                                <option key={id} value={id}>{name as string}</option>
                            ))}
                        </select>
                    </div>
                </div>
            </Card>

            {screenshots?.screenshots && Object.entries(screenshots.screenshots).map(([userId, userScreenshots]) => {
                if (selectedUser && userId !== selectedUser) return null;
                const userName = screenshots.userNames[userId];

                return (
                    <div key={userId}>
                        <h2 className="text-lg font-semibold text-dark-900 mb-4">{userName}</h2>
                        <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-4">
                            {(userScreenshots as any[]).map((screenshot) => (
                                <Card
                                    key={screenshot.id}
                                    className="overflow-hidden cursor-pointer hover:shadow-lg transition-shadow"
                                    onClick={() => setSelectedImage(screenshot.screenshotUrl)}
                                >
                                    <div className="relative aspect-video bg-dark-100">
                                        {/* eslint-disable-next-line @next/next/no-img-element */}
                                        <img
                                            src={`${process.env.NEXT_PUBLIC_API_URL}${API.screenshots.image(screenshot.screenshotUrl)}`}
                                            alt={`Screenshot at ${new Date(screenshot.timestamp).toLocaleTimeString()}`}
                                            className="w-full h-full object-cover"
                                        />
                                        <div className="absolute bottom-2 right-2 bg-black/50 rounded-full p-1">
                                            <ZoomIn className="w-4 h-4 text-white" />
                                        </div>
                                    </div>
                                    <div className="p-3">
                                        <p className="text-sm text-dark-600 truncate">{screenshot.windowTitle || 'Unknown Window'}</p>
                                        <p className="text-xs text-dark-400 mt-1">
                                            {new Date(screenshot.timestamp).toLocaleTimeString()}
                                        </p>
                                    </div>
                                </Card>
                            ))}
                        </div>
                    </div>
                );
            })}

            {(!screenshots?.screenshots || Object.keys(screenshots.screenshots).length === 0) && (
                <Card className="p-12 text-center">
                    <p className="text-dark-500">No screenshots found for this date</p>
                </Card>
            )}

            <Modal
                isOpen={!!selectedImage}
                onClose={() => setSelectedImage(null)}
                title="Screenshot Preview"
            >
                {selectedImage && (
                    // eslint-disable-next-line @next/next/no-img-element
                    <img
                        src={`${process.env.NEXT_PUBLIC_API_URL}${API.screenshots.image(selectedImage)}`}
                        alt="Screenshot preview"
                        className="w-full"
                    />
                )}
            </Modal>
        </div>
    );
}