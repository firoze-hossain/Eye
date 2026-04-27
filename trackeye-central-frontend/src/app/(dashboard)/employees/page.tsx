// src/app/(dashboard)/employees/page.tsx
'use client';

import { useState } from 'react';
import { useQuery } from 'react-query';
import { API, apiClient } from '@/lib/api';
import EmployeeTable from '@/components/employees/EmployeeTable';
import LoadingSpinner from '@/components/common/LoadingSpinner';
import Card from '@/components/ui/Card';
import { UserPlus, Search, Filter } from 'lucide-react';
import toast from 'react-hot-toast';

export default function EmployeesPage() {
    const [searchTerm, setSearchTerm] = useState('');
    const [filterRole, setFilterRole] = useState('all');

    const { data: employees, isLoading, refetch } = useQuery(
        'employees',
        async () => {
            const response = await apiClient.get(API.employees.list);
            return response;
        }
    );

    const handleInvite = async () => {
        // Show invite modal
        toast.success('Invite feature coming soon');
    };

    if (isLoading) return <LoadingSpinner />;

    const filteredEmployees = employees?.filter((emp: any) => {
        const matchesSearch = emp.fullName.toLowerCase().includes(searchTerm.toLowerCase()) ||
            emp.email.toLowerCase().includes(searchTerm.toLowerCase());
        const matchesRole = filterRole === 'all' || emp.role === filterRole;
        return matchesSearch && matchesRole;
    });

    return (
        <div className="space-y-6">
            {/* Header */}
            <div className="flex items-center justify-between">
                <div>
                    <h1 className="text-2xl font-bold text-dark-900">Employees</h1>
                    <p className="text-dark-500 mt-1">Manage and monitor employee activities</p>
                </div>
                <button onClick={handleInvite} className="btn-primary flex items-center gap-2">
                    <UserPlus className="w-4 h-4" />
                    Invite Employee
                </button>
            </div>

            {/* Filters */}
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
                        <button
                            onClick={() => setFilterRole('all')}
                            className={`px-4 py-2 rounded-lg transition-colors ${
                                filterRole === 'all'
                                    ? 'bg-primary-600 text-white'
                                    : 'bg-dark-100 text-dark-600 hover:bg-dark-200'
                            }`}
                        >
                            All
                        </button>
                        <button
                            onClick={() => setFilterRole('admin')}
                            className={`px-4 py-2 rounded-lg transition-colors ${
                                filterRole === 'admin'
                                    ? 'bg-primary-600 text-white'
                                    : 'bg-dark-100 text-dark-600 hover:bg-dark-200'
                            }`}
                        >
                            Admins
                        </button>
                        <button
                            onClick={() => setFilterRole('employee')}
                            className={`px-4 py-2 rounded-lg transition-colors ${
                                filterRole === 'employee'
                                    ? 'bg-primary-600 text-white'
                                    : 'bg-dark-100 text-dark-600 hover:bg-dark-200'
                            }`}
                        >
                            Employees
                        </button>
                    </div>
                </div>
            </Card>

            {/* Employee Table */}
            <EmployeeTable employees={filteredEmployees || []} onRefresh={refetch} />
        </div>
    );
}