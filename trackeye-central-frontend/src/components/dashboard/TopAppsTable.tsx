// src/components/dashboard/TopAppsTable.tsx
import Card from '../ui/Card';

interface TopAppsTableProps {
    apps: Array<{
        appName: string;
        totalMinutes: number;
        userCount: number;
    }>;
}

export default function TopAppsTable({ apps }: TopAppsTableProps) {
    return (
        <Card>
            <div className="p-6">
                <h3 className="text-lg font-semibold text-dark-900 mb-4">Top Applications</h3>
                <div className="overflow-x-auto">
                    <table className="w-full">
                        <thead>
                        <tr className="border-b border-dark-200">
                            <th className="text-left py-3 px-4 text-sm font-medium text-dark-500">Application</th>
                            <th className="text-right py-3 px-4 text-sm font-medium text-dark-500">Total Hours</th>
                            <th className="text-right py-3 px-4 text-sm font-medium text-dark-500">Users</th>
                        </tr>
                        </thead>
                        <tbody>
                        {apps.map((app, index) => (
                            <tr key={index} className="border-b border-dark-100 hover:bg-dark-50">
                                <td className="py-3 px-4 text-dark-900">{app.appName}</td>
                                <td className="py-3 px-4 text-right text-dark-700">
                                    {(app.totalMinutes / 60).toFixed(1)}h
                                </td>
                                <td className="py-3 px-4 text-right text-dark-700">{app.userCount}</td>
                            </tr>
                        ))}
                        {apps.length === 0 && (
                            <tr>
                                <td colSpan={3} className="py-8 text-center text-dark-500">
                                    No data available
                                </td>
                            </tr>
                        )}
                        </tbody>
                    </table>
                </div>
            </div>
        </Card>
    );
}