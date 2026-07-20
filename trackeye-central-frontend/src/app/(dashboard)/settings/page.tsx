// src/app/(dashboard)/settings/page.tsx
'use client';

import { useState, useEffect } from 'react';
import { useQuery } from 'react-query';
import { Copy, Check, Monitor, Shield, User as UserIcon, Info, ShieldAlert, Trash2 } from 'lucide-react';
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

            {isManager && <PolicyRulesCard />}
        </div>
    );
}

// ---- Policy rules management ------------------------------------------------

interface PolicyRule {
    id: number;
    category: string;
    matchType: string;
    pattern: string;
    severity: string;
    active: boolean;
}

const CATEGORIES = ['GAMBLING', 'ADULT', 'GAMING', 'SOCIAL_MEDIA', 'CUSTOM'];
const MATCH_TYPES = [
    { value: 'URL_DOMAIN', label: 'Website domain' },
    { value: 'URL_KEYWORD', label: 'URL contains' },
    { value: 'APP_NAME', label: 'Desktop app name' },
    { value: 'WINDOW_TITLE_KEYWORD', label: 'Window title contains' },
];

function PolicyRulesCard() {
    const [rules, setRules] = useState<PolicyRule[]>([]);
    const [loading, setLoading] = useState(true);
    const [form, setForm] = useState({ category: 'GAMBLING', matchType: 'URL_DOMAIN', pattern: '', severity: 'MEDIUM' });
    const [saving, setSaving] = useState(false);

    const load = async () => {
        setLoading(true);
        try {
            const data = await apiClient.get<PolicyRule[]>('/api/admin/policy-rules');
            setRules(data);
        } catch {
            toast.error('Could not load policy rules');
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => { load(); }, []);

    const addRule = async () => {
        if (!form.pattern.trim()) {
            toast.error('Enter a pattern to match');
            return;
        }
        setSaving(true);
        try {
            await apiClient.post('/api/admin/policy-rules', form);
            toast.success('Rule added');
            setForm({ ...form, pattern: '' });
            load();
        } catch {
            toast.error('Could not add rule');
        } finally {
            setSaving(false);
        }
    };

    const removeRule = async (id: number) => {
        try {
            await apiClient.delete(`/api/admin/policy-rules/${id}`);
            toast.success('Rule removed');
            load();
        } catch {
            toast.error('Could not remove rule');
        }
    };

    return (
        <Card className="p-6 max-w-3xl">
            <h2 className="font-semibold text-dark-900 mb-1 flex items-center gap-2">
                <ShieldAlert className="w-4 h-4" /> Policy alerts
            </h2>
            <p className="text-sm text-dark-500 mb-4">
                Get notified when an employee's activity matches a rule you define here -
                a gambling site, a game launcher, an adult-content keyword, or anything
                else specific to your organization. Nothing is blocked pre-emptively;
                admins (and the employee's manager) are simply alerted.
            </p>

            <div className="grid grid-cols-1 sm:grid-cols-4 gap-2 mb-4">
                <select
                    className="input text-sm"
                    value={form.category}
                    onChange={(e) => setForm({ ...form, category: e.target.value })}
                >
                    {CATEGORIES.map((c) => <option key={c} value={c}>{c.replace('_', ' ')}</option>)}
                </select>
                <select
                    className="input text-sm"
                    value={form.matchType}
                    onChange={(e) => setForm({ ...form, matchType: e.target.value })}
                >
                    {MATCH_TYPES.map((m) => <option key={m.value} value={m.value}>{m.label}</option>)}
                </select>
                <input
                    className="input text-sm"
                    placeholder="e.g. bet365.com or steam"
                    value={form.pattern}
                    onChange={(e) => setForm({ ...form, pattern: e.target.value })}
                />
                <button onClick={addRule} disabled={saving} className="btn-primary text-sm disabled:opacity-50">
                    {saving ? 'Adding…' : 'Add rule'}
                </button>
            </div>

            {loading ? (
                <LoadingSpinner />
            ) : !rules.length ? (
                <p className="text-sm text-dark-400">No rules yet — add one above to start getting alerts.</p>
            ) : (
                <div className="space-y-2">
                    {rules.map((r) => (
                        <div key={r.id} className="flex items-center justify-between p-2.5 border border-dark-100 rounded-lg text-sm">
                            <div className="flex items-center gap-2">
                                <span className="px-2 py-0.5 rounded-full bg-dark-100 text-dark-600 text-xs capitalize">
                                    {r.category.replace('_', ' ').toLowerCase()}
                                </span>
                                <span className="text-dark-700">{r.pattern}</span>
                                <span className="text-dark-400 text-xs">
                                    ({MATCH_TYPES.find((m) => m.value === r.matchType)?.label})
                                </span>
                            </div>
                            <button onClick={() => removeRule(r.id)} className="text-dark-400 hover:text-red-600">
                                <Trash2 className="w-4 h-4" />
                            </button>
                        </div>
                    ))}
                </div>
            )}
        </Card>
    );
}
