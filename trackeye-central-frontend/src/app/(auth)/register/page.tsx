// src/app/(auth)/register/page.tsx
'use client';

import { useState } from 'react';
import { useRouter } from 'next/navigation';
import Link from 'next/link';
import { UserPlus, Eye, EyeOff } from 'lucide-react';
import toast from 'react-hot-toast';
// import { apiClient } from '@/lib/api';
import {apiClient} from "../../../lib/api";

export default function RegisterPage() {
    const router = useRouter();
    const [showPassword, setShowPassword] = useState(false);
    const [isLoading, setIsLoading] = useState(false);
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
            const response = await apiClient.post('/api/public/register', formData);
            toast.success('Organization registered successfully!');
            router.push('/login');
        } catch (error: any) {
            toast.error(error.response?.data?.message || 'Registration failed');
        } finally {
            setIsLoading(false);
        }
    };

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