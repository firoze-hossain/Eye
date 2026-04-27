// src/components/employees/EmployeeTable.tsx
'use client';

import { useState } from 'react';
import { useRouter } from 'next/navigation';
import { MoreVertical, UserCheck, UserX, Eye } from 'lucide-react';
import Card from '../ui/Card';
import toast from 'react-hot-toast';
import { apiClient, API } from '@/lib/api';

interface EmployeeTableProps {
    employees: any[];
    onRefresh: () => void;
}

export default function EmployeeTable({ employees, onRefresh }: EmployeeTableProps) {
    const router = useRouter();
    const [openMenu, setOpenMenu] = useState<number | null>(null);

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
                            <td className="py-3 px-4">
                  <span className={`px-2 py-1 rounded-full text-xs font-medium ${getStatusBadge(employee.status)}`}>
                    {employee.status}
                  </span>
                            </td>
                            <td className="py-3 px-4 text-dark-600">{formatDate(employee.createdAt)}</td>
                            <td className="py-3 px-4 text-right relative">
                                <button
                                    onClick={() => setOpenMenu(openMenu === employee.id ? null : employee.id)}
                                    className="p-2 hover:bg-dark-100 rounded-lg"
                                >
                                    <MoreVertical className="w-4 h-4 text-dark-500" />
                                </button>

                                {openMenu === employee.id && (
                                    <>
                                        <div
                                            className="fixed inset-0 z-10"
                                            onClick={() => setOpenMenu(null)}
                                        />
                                        <div className="absolute right-4 mt-2 w-48 bg-white rounded-lg shadow-lg border border-dark-200 z-20">
                                            <button
                                                onClick={() => {
                                                    setOpenMenu(null);
                                                    router.push(`/employees/${employee.id}`);
                                                }}
                                                className="w-full flex items-center gap-3 px-4 py-2 text-sm text-dark-700 hover:bg-dark-50"
                                            >
                                                <Eye className="w-4 h-4" />
                                                View Details
                                            </button>
                                            <button
                                                onClick={() => {
                                                    setOpenMenu(null);
                                                    handleStatusChange(employee.id, employee.status);
                                                }}
                                                className="w-full flex items-center gap-3 px-4 py-2 text-sm text-dark-700 hover:bg-dark-50"
                                            >
                                                {employee.status === 'active' ? (
                                                    <>
                                                        <UserX className="w-4 h-4" />
                                                        Deactivate
                                                    </>
                                                ) : (
                                                    <>
                                                        <UserCheck className="w-4 h-4" />
                                                        Activate
                                                    </>
                                                )}
                                            </button>
                                        </div>
                                    </>
                                )}
                            </td>
                        </tr>
                    ))}
                    </tbody>
                </table>
            </div>
        </Card>
    );
}