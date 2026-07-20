// src/app/(auth)/register/page.tsx
'use client';

import { useState } from 'react';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { UserPlus, Eye, EyeOff, CheckCircle2, Copy, Check, Monitor, ArrowRight } from 'lucide-react';
import toast from 'react-hot-toast';
import { apiClient, API } from '@/lib/api';

interface RegistrationResponse {
    organizationId: number;
    organizationName: string;
    subdomain: string;
    userId: number;
    userEmail: string;
    userFullName: string;
    registrationToken: string;
    serverUrl: string;
    message: string;
}

/** The agent's local setup page, served by the desktop app on each employee PC. */
const AGENT_SETUP_URL = 'http://localhost:8765/setup.html';

export default function RegisterPage() {
    const router = useRouter();
    const [showPassword, setShowPassword] = useState(false);
    const [isLoading, setIsLoading] = useState(false);
    const [copied, setCopied] = useState(false);

    // When this is set, registration succeeded and we show the "connect" step.
    const [result, setResult] = useState<RegistrationResponse | null>(null);

    const [formData, setFormData] = useState({
        orgName: '',
        adminEmail: '',
        adminFullName: '',
        password: '',
        subdomain: '',
    });

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        setIsLoading(true);

        try {
            const response = await apiClient.post<RegistrationResponse>(
                API.public.register,
                formData
            );
            toast.success('Organization created');
            // FIXED: previously this discarded `response` and redirected straight to
            // /login, throwing away registrationToken - the one value a new admin
            // needs to connect their computer. Now we show it.
            setResult(response);
        } catch (error: any) {
            toast.error(error.response?.data?.message || error.response?.data?.error || 'Registration failed');
        } finally {
            setIsLoading(false);
        }
    };

    const copyToken = async () => {
        if (!result) return;
        try {
            await navigator.clipboard.writeText(result.registrationToken);
            setCopied(true);
            toast.success('Token copied');
            setTimeout(() => setCopied(false), 2000);
        } catch {
            toast.error('Could not copy — select and copy manually');
        }
    };

    // ---------- Step 2: connect your computer ----------
    if (result) {
        return (
            <div className="flex items-center justify-center min-h-screen py-12 px-4">
                <div className="bg-white rounded-2xl shadow-xl w-full max-w-lg p-8">
                    <div className="text-center mb-8">
                        <div className="w-16 h-16 bg-green-100 rounded-2xl flex items-center justify-center mx-auto mb-4">
                            <CheckCircle2 className="w-8 h-8 text-green-600" />
                        </div>
                        <h1 className="text-2xl font-bold text-dark-900">
                            {result.organizationName} is ready
                        </h1>
                        <p className="text-dark-500 mt-2">
                            One more step: connect a computer so TrackEye has something to track.
                        </p>
                    </div>

                    <div className="border border-dark-200 rounded-xl p-5 mb-6">
                        <label className="label">Your registration token</label>
                        <div className="flex gap-2">
                            <code className="flex-1 px-3 py-2.5 bg-dark-50 border border-dark-200 rounded-lg text-sm font-mono break-all select-all">
                                {result.registrationToken}
                            </code>
                            <button
                                type="button"
                                onClick={copyToken}
                                className="px-3 rounded-lg border border-dark-200 hover:bg-dark-50 transition-colors flex-none"
                                title="Copy token"
                            >
                                {copied
                                    ? <Check className="w-4 h-4 text-green-600" />
                                    : <Copy className="w-4 h-4 text-dark-500" />}
                            </button>
                        </div>
                        <p className="text-xs text-dark-400 mt-2">
                            Save this now — it isn't shown again. You can always generate a new one
                            from the Employees page later.
                        </p>
                    </div>

                    <div className="mb-6">
                        <h2 className="text-sm font-semibold text-dark-900 mb-3 flex items-center gap-2">
                            <Monitor className="w-4 h-4" /> Connect this computer
                        </h2>
                        <ol className="space-y-2 text-sm text-dark-600 list-decimal list-inside">
                            <li>Make sure the TrackEye desktop app is running.</li>
                            <li>
                                Open{' '}
                                <a
                                    href={AGENT_SETUP_URL}
                                    target="_blank"
                                    rel="noreferrer"
                                    className="text-primary-600 hover:text-primary-700 font-medium"
                                >
                                    {AGENT_SETUP_URL}
                                </a>
                            </li>
                            <li>
                                Enter <span className="font-medium text-dark-800">{result.userEmail}</span>{' '}
                                and paste the token above.
                            </li>
                        </ol>
                    </div>

                    <div className="flex gap-3">
                        <a
                            href={AGENT_SETUP_URL}
                            target="_blank"
                            rel="noreferrer"
                            className="flex-1 btn-primary py-2.5 text-center"
                        >
                            Open setup page
                        </a>
                        <button
                            type="button"
                            onClick={() => router.push('/login')}
                            className="flex-1 py-2.5 rounded-lg border border-dark-200 text-dark-700 hover:bg-dark-50 transition-colors flex items-center justify-center gap-1.5"
                        >
                            Go to sign in <ArrowRight className="w-4 h-4" />
                        </button>
                    </div>
                </div>
            </div>
        );
    }

    // ---------- Step 1: registration form ----------
    return (
        <div className="flex items-center justify-center min-h-screen py-12 px-4">
            <div className="bg-white rounded-2xl shadow-xl w-full max-w-md p-8">
                <div className="text-center mb-8">
                    <div className="w-16 h-16 bg-primary-600 rounded-2xl flex items-center justify-center mx-auto mb-4">
                        <UserPlus className="w-8 h-8 text-white" />
                    </div>
                    <h1 className="text-2xl font-bold text-dark-900">Create Account</h1>
                    <p className="text-dark-500 mt-2">Start tracking your team's productivity</p>
                </div>

                <form onSubmit={handleSubmit} className="space-y-6">
                    <div>
                        <label className="label">Organization Name</label>
                        <input
                            type="text"
                            required
                            value={formData.orgName}
                            onChange={(e) => setFormData({ ...formData, orgName: e.target.value })}
                            className="input"
                            placeholder="Acme Inc."
                        />
                    </div>

                    <div>
                        <label className="label">Your Full Name</label>
                        <input
                            type="text"
                            required
                            value={formData.adminFullName}
                            onChange={(e) => setFormData({ ...formData, adminFullName: e.target.value })}
                            className="input"
                            placeholder="John Doe"
                        />
                    </div>

                    <div>
                        <label className="label">Email Address</label>
                        <input
                            type="email"
                            required
                            value={formData.adminEmail}
                            onChange={(e) => setFormData({ ...formData, adminEmail: e.target.value })}
                            className="input"
                            placeholder="admin@company.com"
                        />
                    </div>

                    <div>
                        <label className="label">Subdomain (Optional)</label>
                        <input
                            type="text"
                            value={formData.subdomain}
                            onChange={(e) => setFormData({ ...formData, subdomain: e.target.value })}
                            className="input"
                            placeholder="acme"
                        />
                        <p className="text-xs text-dark-400 mt-1">Your workspace will be at: acme.trackeye.com</p>
                    </div>

                    <div>
                        <label className="label">Password</label>
                        <div className="relative">
                            <input
                                type={showPassword ? 'text' : 'password'}
                                required
                                value={formData.password}
                                onChange={(e) => setFormData({ ...formData, password: e.target.value })}
                                className="input pr-10"
                                placeholder="••••••••"
                            />
                            <button
                                type="button"
                                onClick={() => setShowPassword(!showPassword)}
                                className="absolute right-3 top-1/2 transform -translate-y-1/2"
                            >
                                {showPassword ? (
                                    <EyeOff className="w-4 h-4 text-dark-400" />
                                ) : (
                                    <Eye className="w-4 h-4 text-dark-400" />
                                )}
                            </button>
                        </div>
                    </div>

                    <button
                        type="submit"
                        disabled={isLoading}
                        className="w-full btn-primary py-2 disabled:opacity-50"
                    >
                        {isLoading ? 'Creating account...' : 'Create Account'}
                    </button>
                </form>

                <div className="mt-6 text-center">
                    <Link href="/login" className="text-sm text-primary-600 hover:text-primary-700">
                        Already have an account? Sign in
                    </Link>
                </div>
            </div>
        </div>
    );
}
