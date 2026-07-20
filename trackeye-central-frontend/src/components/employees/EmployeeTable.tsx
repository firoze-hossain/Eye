// src/components/employees/EmployeeTable.tsx
'use client';

import { useRouter } from 'next/navigation';
import { UserCheck, UserX, Eye, Monitor } from 'lucide-react';
import Card from '../ui/Card';
import ActionMenu from '../ui/ActionMenu';
import toast from 'react-hot-toast';
import { apiClient, API } from '@/lib/api';
import { useAuth } from '@/context/AuthContext';

interface EmployeeTableProps {
    employees: any[];
    onRefresh: () => void;
    onGenerateDeviceToken?: (employee: { id: number; fullName: string; email: string }) => void;
}

export default function EmployeeTable({ employees, onRefresh, onGenerateDeviceToken }: EmployeeTableProps) {
    const router = useRouter();
    const { user } = useAuth();
    const isAdmin = user?.role === 'admin';

    // Anyone with role 'supervisor' can be picked as a manager for another employee.
    const supervisors = employees.filter((e) => e.role === 'supervisor' || e.role === 'admin');

    const handleStatusChange = async (userId: number, currentStatus: string) => {
        try {
            if (currentStatus === 'active') {
                await apiClient.post(API.employees.deactivate(userId));
                toast.success('Employee deactivated');
            } else {
                await apiClient.post(API.employees.activate(userId));
                toast.success('Employee activated');
            }
            onRefresh();
        } catch (error) {
            toast.error('Failed to update status');
        }
    };

    const handleManagerChange = async (employeeId: number, managerId: string) => {
        try {
            await apiClient.post(API.employees.assignManager(employeeId), {
                managerId: managerId ? Number(managerId) : null,
            });
            toast.success('Manager updated');
            onRefresh();
        } catch {
            toast.error('Failed to update manager');
        }
    };

    const formatDate = (timestamp: number) => {
        return new Date(timestamp).toLocaleDateString();
    };

    const getStatusBadge = (status: string) => {
        const styles = {
            active: 'bg-green-100 text-green-800',
            inactive: 'bg-red-100 text-red-800',
            invited: 'bg-yellow-100 text-yellow-800',
        };
        return styles[status as keyof typeof styles] || styles.invited;
    };

    return (
        <Card>
            <div className="overflow-x-auto">
                <table className="w-full">
                    <thead>
                    <tr className="border-b border-dark-200 bg-dark-50">
                        <th className="text-left py-3 px-4 text-sm font-medium text-dark-500">Employee</th>
                        <th className="text-left py-3 px-4 text-sm font-medium text-dark-500">Email</th>
                        <th className="text-left py-3 px-4 text-sm font-medium text-dark-500">Role</th>
                        {isAdmin && (
                            <th className="text-left py-3 px-4 text-sm font-medium text-dark-500">Reports to</th>
                        )}
                        <th className="text-left py-3 px-4 text-sm font-medium text-dark-500">Status</th>
                        <th className="text-left py-3 px-4 text-sm font-medium text-dark-500">Joined</th>
                        <th className="text-right py-3 px-4 text-sm font-medium text-dark-500">Actions</th>
                    </tr>
                    </thead>
                    <tbody>
                    {employees.map((employee) => (
                        <tr key={employee.id} className="border-b border-dark-100 hover:bg-dark-50">
                            <td className="py-3 px-4">
                                <div className="flex items-center gap-3">
                                    <div className="w-8 h-8 bg-primary-100 rounded-full flex items-center justify-center">
                      <span className="text-sm font-medium text-primary-600">
                        {employee.fullName.charAt(0)}
                      </span>
                                    </div>
                                    <span className="font-medium text-dark-900">{employee.fullName}</span>
                                </div>
                            </td>
                            <td className="py-3 px-4 text-dark-600">{employee.email}</td>
                            <td className="py-3 px-4">
                                <span className="capitalize text-dark-600">{employee.role}</span>
                            </td>
                            {isAdmin && (
                                <td className="py-3 px-4">
                                    {employee.id === user?.id ? (
                                        <span className="text-dark-300 text-sm">—</span>
                                    ) : (
                                        <select
                                            value={employee.managerId ?? ''}
                                            onChange={(e) => handleManagerChange(employee.id, e.target.value)}
                                            className="text-sm border border-dark-200 rounded-md px-2 py-1 bg-white"
                                        >
                                            <option value="">Unassigned</option>
                                            {supervisors
                                                .filter((s) => s.id !== employee.id)
                                                .map((s) => (
                                                    <option key={s.id} value={s.id}>{s.fullName}</option>
                                                ))}
                                        </select>
                                    )}
                                </td>
                            )}
                            <td className="py-3 px-4">
                  <span className={`px-2 py-1 rounded-full text-xs font-medium ${getStatusBadge(employee.status)}`}>
                    {employee.status}
                  </span>
                            </td>
                            <td className="py-3 px-4 text-dark-600">{formatDate(employee.createdAt)}</td>
                            <td className="py-3 px-4 text-right">
                                <ActionMenu
                                    items={[
                                        {
                                            label: 'View Details',
                                            icon: <Eye className="w-4 h-4" />,
                                            onClick: () => router.push(`/employees/${employee.id}`),
                                        },
                                        ...(onGenerateDeviceToken ? [{
                                            label: 'Generate device token',
                                            icon: <Monitor className="w-4 h-4" />,
                                            onClick: () => onGenerateDeviceToken(employee),
                                        }] : []),
                                        {
                                            label: employee.status === 'active' ? 'Deactivate' : 'Activate',
                                            icon: employee.status === 'active'
                                                ? <UserX className="w-4 h-4" />
                                                : <UserCheck className="w-4 h-4" />,
                                            danger: employee.status === 'active',
                                            onClick: () => handleStatusChange(employee.id, employee.status),
                                        },
                                    ]}
                                />
                            </td>
                        </tr>
                    ))}
                    </tbody>
                </table>
            </div>
        </Card>
    );
}