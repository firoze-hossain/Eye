// src/app/(dashboard)/employees/page.tsx
'use client';

import { useState } from 'react';
import { useQuery } from 'react-query';
import { API, apiClient } from '@/lib/api';
import EmployeeTable from '@/components/employees/EmployeeTable';
import LoadingSpinner from '@/components/common/LoadingSpinner';
import Card from '@/components/ui/Card';
import Modal from '@/components/ui/Modal';
import { UserPlus, Search, Copy, Check, Monitor } from 'lucide-react';
import toast from 'react-hot-toast';

export default function EmployeesPage() {
    const [searchTerm, setSearchTerm] = useState('');
    const [filterRole, setFilterRole] = useState('all');

    // Invite flow
    const [inviteOpen, setInviteOpen] = useState(false);
    const [form, setForm] = useState({ email: '', fullName: '', role: 'employee' });
    const [submitting, setSubmitting] = useState(false);
    const [invite, setInvite] = useState<any | null>(null);

    // Device-token flow (separate from invite - see explanation in the modal)
    const [deviceTokenFor, setDeviceTokenFor] = useState<{ id: number; fullName: string; email: string } | null>(null);
    const [deviceToken, setDeviceToken] = useState<string | null>(null);
    const [generatingToken, setGeneratingToken] = useState(false);

    const [copied, setCopied] = useState<'invite' | 'device' | null>(null);

    const { data: employees, isLoading, refetch } = useQuery(
        'employees',
        async () => apiClient.get(API.employees.list)
    );

    const submitInvite = async () => {
        if (!form.email || !form.fullName) {
            toast.error('Email and full name are required');
            return;
        }
        setSubmitting(true);
        try {
            const res = await apiClient.post<any>(API.employees.invite, form);
            setInvite(res);
            toast.success('Invitation created');
            refetch();
        } catch (e: any) {
            toast.error(e.response?.data?.message || e.response?.data?.error || 'Invite failed');
        } finally {
            setSubmitting(false);
        }
    };

    const copyToken = async (text: string, which: 'invite' | 'device') => {
        await navigator.clipboard.writeText(text);
        setCopied(which);
        toast.success('Token copied');
        setTimeout(() => setCopied(null), 2000);
    };

    const closeInvite = () => {
        setInviteOpen(false);
        setInvite(null);
        setForm({ email: '', fullName: '', role: 'employee' });
    };

    const openDeviceToken = async (employee: { id: number; fullName: string; email: string }) => {
        setDeviceTokenFor(employee);
        setDeviceToken(null);
        setGeneratingToken(true);
        try {
            const res = await apiClient.post<any>(API.employees.deviceToken(employee.id));
            setDeviceToken(res.registrationToken);
        } catch (e: any) {
            toast.error(e.response?.data?.error || 'Could not generate device token');
            setDeviceTokenFor(null);
        } finally {
            setGeneratingToken(false);
        }
    };

    if (isLoading) return <LoadingSpinner />;

    const filteredEmployees = (employees as any[])?.filter((emp: any) => {
        const matchesSearch = emp.fullName.toLowerCase().includes(searchTerm.toLowerCase()) ||
            emp.email.toLowerCase().includes(searchTerm.toLowerCase());
        const matchesRole = filterRole === 'all' || emp.role === filterRole;
        return matchesSearch && matchesRole;
    });

    return (
        <div className="space-y-6">
            <div className="flex items-center justify-between">
                <div>
                    <h1 className="text-2xl font-bold text-dark-900">Employees</h1>
                    <p className="text-dark-500 mt-1">Manage and monitor employee activities</p>
                </div>
                <button onClick={() => setInviteOpen(true)} className="btn-primary flex items-center gap-2">
                    <UserPlus className="w-4 h-4" />
                    Invite Employee
                </button>
            </div>

            <Card className="p-4">
                <div className="flex flex-wrap gap-4">
                    <div className="flex-1 min-w-[200px]">
                        <div className="relative">
                            <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 w-4 h-4 text-dark-400" />
                            <input
                                type="text"
                                placeholder="Search employees..."
                                value={searchTerm}
                                onChange={(e) => setSearchTerm(e.target.value)}
                                className="input pl-10"
                            />
                        </div>
                    </div>
                    <div className="flex gap-2">
                        {['all', 'admin', 'employee'].map((r) => (
                            <button
                                key={r}
                                onClick={() => setFilterRole(r)}
                                className={`px-4 py-2 rounded-lg transition-colors capitalize ${
                                    filterRole === r ? 'bg-primary-600 text-white' : 'bg-dark-100 text-dark-600 hover:bg-dark-200'
                                }`}
                            >
                                {r === 'all' ? 'All' : r + 's'}
                            </button>
                        ))}
                    </div>
                </div>
            </Card>

            <EmployeeTable
                employees={filteredEmployees || []}
                onRefresh={refetch}
                onGenerateDeviceToken={openDeviceToken}
            />

            {/* Invite modal - creates the account. Does NOT connect a device. */}
            <Modal isOpen={inviteOpen} onClose={closeInvite} title="Invite Employee">
                {invite ? (
                    <div className="space-y-4">
                        <p className="text-sm text-dark-600">
                            Account created for <span className="font-medium">{invite.email}</span>.
                        </p>
                        <div className="p-3 bg-blue-50 border border-blue-100 rounded-lg text-sm text-blue-800">
                            This does not connect their computer yet - that's a separate step.
                            Once they're ready to install the agent, use <strong>"Generate device token"</strong> from
                            their row in the table below and give them that token instead.
                        </div>
                        <div className="flex justify-end">
                            <button onClick={closeInvite} className="btn-primary">Done</button>
                        </div>
                    </div>
                ) : (
                    <div className="space-y-4">
                        <div>
                            <label className="label">Full name</label>
                            <input className="input" value={form.fullName}
                                onChange={(e) => setForm({ ...form, fullName: e.target.value })} placeholder="Jane Doe" />
                        </div>
                        <div>
                            <label className="label">Email</label>
                            <input className="input" type="email" value={form.email}
                                onChange={(e) => setForm({ ...form, email: e.target.value })} placeholder="jane@company.com" />
                        </div>
                        <div>
                            <label className="label">Role</label>
                            <select className="input" value={form.role}
                                onChange={(e) => setForm({ ...form, role: e.target.value })}>
                                <option value="employee">Employee</option>
                                <option value="supervisor">Supervisor</option>
                                <option value="admin">Admin</option>
                            </select>
                        </div>
                        <div className="flex justify-end gap-2">
                            <button onClick={closeInvite} className="px-4 py-2 rounded-lg border border-dark-200 text-dark-600 hover:bg-dark-50">
                                Cancel
                            </button>
                            <button onClick={submitInvite} disabled={submitting} className="btn-primary disabled:opacity-50">
                                {submitting ? 'Sending…' : 'Create invite'}
                            </button>
                        </div>
                    </div>
                )}
            </Modal>

            {/* Device-token modal - THE correct token for setup.html. */}
            <Modal
                isOpen={!!deviceTokenFor}
                onClose={() => { setDeviceTokenFor(null); setDeviceToken(null); }}
                title="Connect a computer"
            >
                {deviceTokenFor && (
                    <div className="space-y-4">
                        <p className="text-sm text-dark-600 flex items-center gap-2">
                            <Monitor className="w-4 h-4 flex-none" />
                            Device registration token for <span className="font-medium">{deviceTokenFor.fullName}</span>
                        </p>

                        {generatingToken ? (
                            <LoadingSpinner />
                        ) : deviceToken ? (
                            <>
                                <div className="flex gap-2">
                                    <code className="flex-1 px-3 py-2.5 bg-dark-50 border border-dark-200 rounded-lg text-sm font-mono break-all select-all">
                                        {deviceToken}
                                    </code>
                                    <button onClick={() => copyToken(deviceToken, 'device')} className="px-3 rounded-lg border border-dark-200 hover:bg-dark-50 flex-none">
                                        {copied === 'device' ? <Check className="w-4 h-4 text-green-600" /> : <Copy className="w-4 h-4 text-dark-500" />}
                                    </button>
                                </div>
                                <p className="text-xs text-dark-400">
                                    On <strong>{deviceTokenFor.fullName}'s</strong> computer, install the TrackEye agent, open{' '}
                                    <span className="font-mono">http://localhost:8765/setup.html</span>, and enter{' '}
                                    <strong>{deviceTokenFor.email}</strong> with this token.
                                </p>
                                <div className="flex justify-between items-center">
                                    <button
                                        onClick={() => openDeviceToken(deviceTokenFor)}
                                        className="text-sm text-primary-600 hover:underline"
                                    >
                                        Generate another
                                    </button>
                                    <button onClick={() => { setDeviceTokenFor(null); setDeviceToken(null); }} className="btn-primary">
                                        Done
                                    </button>
                                </div>
                            </>
                        ) : null}
                    </div>
                )}
            </Modal>
        </div>
    );
}
