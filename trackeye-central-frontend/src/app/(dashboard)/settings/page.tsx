// src/app/(dashboard)/settings/page.tsx
'use client';

import { useState } from 'react';
import { useQuery } from 'react-query';
import { Copy, Check, Monitor, Shield, User as UserIcon, Info } from 'lucide-react';
import Card from '@/components/ui/Card';
import LoadingSpinner from '@/components/common/LoadingSpinner';
import { apiClient } from '@/lib/api';
import { useAuth } from '@/context/AuthContext';
import toast from 'react-hot-toast';

const AGENT_SETUP_URL = 'http://localhost:8765/setup.html';

export default function SettingsPage() {
    const { user } = useAuth();
    const [token, setToken] = useState<string | null>(null);
    const [copied, setCopied] = useState(false);
    const [generating, setGenerating] = useState(false);

    const { data: me, isLoading } = useQuery<any>('me', () =>
        apiClient.get<any>('/api/auth/me')
    );

    const generateToken = async () => {
        setGenerating(true);
        try {
            const res = await apiClient.post<any>('/api/admin/device-token');
            setToken(res.registrationToken);
            toast.success('Device token generated');
        } catch (e: any) {
            toast.error(e.response?.data?.error || 'Could not generate token');
        } finally {
            setGenerating(false);
        }
    };

    const copy = async () => {
        if (!token) return;
        await navigator.clipboard.writeText(token);
        setCopied(true);
        toast.success('Copied');
        setTimeout(() => setCopied(false), 2000);
    };

    if (isLoading) return <LoadingSpinner />;

    const isManager = user?.role === 'admin' || user?.role === 'supervisor';

    return (
        <div className="space-y-6 max-w-3xl">
            <div>
                <h1 className="text-2xl font-bold text-dark-900">Settings</h1>
                <p className="text-dark-500 mt-1">Your account and workspace configuration</p>
            </div>

            {/* Account */}
            <Card className="p-6">
                <h2 className="font-semibold text-dark-900 mb-4 flex items-center gap-2">
                    <UserIcon className="w-4 h-4" /> Account
                </h2>
                <dl className="grid grid-cols-1 sm:grid-cols-2 gap-4 text-sm">
                    <div>
                        <dt className="text-dark-400">Full name</dt>
                        <dd className="text-dark-800 font-medium mt-0.5">{me?.fullName || user?.name}</dd>
                    </div>
                    <div>
                        <dt className="text-dark-400">Email</dt>
                        <dd className="text-dark-800 font-medium mt-0.5">{me?.email || user?.email}</dd>
                    </div>
                    <div>
                        <dt className="text-dark-400">Role</dt>
                        <dd className="text-dark-800 font-medium mt-0.5 capitalize">{me?.role || user?.role}</dd>
                    </div>
                    <div>
                        <dt className="text-dark-400">Organization ID</dt>
                        <dd className="text-dark-800 font-medium mt-0.5">{me?.organizationId ?? '—'}</dd>
                    </div>
                </dl>
            </Card>

            {/* Connect a device */}
            {isManager && (
                <Card className="p-6">
                    <h2 className="font-semibold text-dark-900 mb-1 flex items-center gap-2">
                        <Monitor className="w-4 h-4" /> Connect a computer
                    </h2>
                    <p className="text-sm text-dark-500 mb-4">
                        Generate a registration token, then paste it into the desktop agent's setup page.
                    </p>

                    {token ? (
                        <div className="space-y-3">
                            <div className="flex gap-2">
                                <code className="flex-1 px-3 py-2.5 bg-dark-50 border border-dark-200 rounded-lg text-sm font-mono break-all select-all">
                                    {token}
                                </code>
                                <button onClick={copy} className="px-3 rounded-lg border border-dark-200 hover:bg-dark-50 flex-none">
                                    {copied ? <Check className="w-4 h-4 text-green-600" /> : <Copy className="w-4 h-4 text-dark-500" />}
                                </button>
                            </div>
                            <p className="text-xs text-dark-400">
                                Open{' '}
                                <a href={AGENT_SETUP_URL} target="_blank" rel="noreferrer" className="text-primary-600 hover:underline">
                                    {AGENT_SETUP_URL}
                                </a>{' '}
                                on the computer you want to track, then enter your email and this token.
                            </p>
                            <button onClick={generateToken} className="text-sm text-primary-600 hover:underline">
                                Generate another
                            </button>
                        </div>
                    ) : (
                        <button onClick={generateToken} disabled={generating} className="btn-primary disabled:opacity-50">
                            {generating ? 'Generating…' : 'Generate device token'}
                        </button>
                    )}
                </Card>
            )}

            {/* Tracking info */}
            <Card className="p-6">
                <h2 className="font-semibold text-dark-900 mb-4 flex items-center gap-2">
                    <Shield className="w-4 h-4" /> What is tracked
                </h2>
                <ul className="text-sm text-dark-600 space-y-2 list-disc list-inside">
                    <li>Active application and window title</li>
                    <li>Browser activity (site titles and URLs)</li>
                    <li>Idle / away-from-keyboard periods</li>
                    <li>Periodic screenshots (every 5 minutes, and on app switch)</li>
                </ul>
                <div className="mt-4 flex gap-2 p-3 bg-blue-50 border border-blue-100 rounded-lg text-sm text-blue-800">
                    <Info className="w-4 h-4 flex-none mt-0.5" />
                    <p>
                        Employee monitoring is legally regulated in many countries and usually requires
                        informing staff and obtaining consent. Make sure your deployment complies with
                        local law before tracking real employees.
                    </p>
                </div>
            </Card>
        </div>
    );
}
