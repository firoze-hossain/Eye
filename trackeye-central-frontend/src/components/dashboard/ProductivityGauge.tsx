// src/components/dashboard/ProductivityGauge.tsx
import Card from '../ui/Card';
import { TrendingUp, TrendingDown } from 'lucide-react';

interface ProductivityGaugeProps {
    score: {
        score: number;
        grade: string;
        productiveMinutes: number;
        unproductiveMinutes: number;
        neutralMinutes: number;
    };
}

export default function ProductivityGauge({ score }: ProductivityGaugeProps) {
    const getGradeColor = (grade: string) => {
        switch (grade) {
            case 'A+': return 'text-green-600';
            case 'A': return 'text-green-500';
            case 'B': return 'text-blue-500';
            case 'C': return 'text-yellow-500';
            case 'D': return 'text-orange-500';
            default: return 'text-red-500';
        }
    };

    const percentage = Math.min(100, Math.max(0, score.score));

    return (
        <Card>
            <div className="p-6">
                <h3 className="text-lg font-semibold text-dark-900 mb-4">Productivity Score</h3>

                <div className="relative mb-6">
                    <div className="w-32 h-32 mx-auto relative">
                        <svg className="w-full h-full transform -rotate-90">
                            <circle
                                cx="64"
                                cy="64"
                                r="56"
                                stroke="#e5e7eb"
                                strokeWidth="12"
                                fill="none"
                            />
                            <circle
                                cx="64"
                                cy="64"
                                r="56"
                                stroke="currentColor"
                                strokeWidth="12"
                                fill="none"
                                strokeDasharray={`${2 * Math.PI * 56}`}
                                strokeDashoffset={`${2 * Math.PI * 56 * (1 - percentage / 100)}`}
                                className="text-primary-600 transition-all duration-500"
                            />
                        </svg>
                        <div className="absolute inset-0 flex flex-col items-center justify-center">
              <span className={`text-3xl font-bold ${getGradeColor(score.grade)}`}>
                {score.grade}
              </span>
                            <span className="text-sm text-dark-500">{Math.round(percentage)}%</span>
                        </div>
                    </div>
                </div>

                <div className="space-y-3">
                    <div className="flex items-center justify-between">
                        <div className="flex items-center gap-2">
                            <TrendingUp className="w-4 h-4 text-green-500" />
                            <span className="text-sm text-dark-600">Productive</span>
                        </div>
                        <span className="text-sm font-medium text-dark-900">
              {Math.floor(score.productiveMinutes / 60)}h {score.productiveMinutes % 60}m
            </span>
                    </div>
                    <div className="flex items-center justify-between">
                        <div className="flex items-center gap-2">
                            <TrendingDown className="w-4 h-4 text-red-500" />
                            <span className="text-sm text-dark-600">Unproductive</span>
                        </div>
                        <span className="text-sm font-medium text-dark-900">
              {Math.floor(score.unproductiveMinutes / 60)}h {score.unproductiveMinutes % 60}m
            </span>
                    </div>
                </div>
            </div>
        </Card>
    );
}