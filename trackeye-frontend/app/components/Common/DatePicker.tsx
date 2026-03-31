'use client';

import { useState } from 'react';
import { Calendar } from 'lucide-react';
import { format } from 'date-fns';

interface DatePickerProps {
    selectedDate: Date;
    onChange: (date: Date) => void;
}

export default function DatePicker({ selectedDate, onChange }: DatePickerProps) {
    return (
        <div className="relative">
            <input
                type="date"
                value={format(selectedDate, 'yyyy-MM-dd')}
                onChange={(e) => onChange(new Date(e.target.value))}
                className="bg-gray-800 text-white px-4 py-2 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary/50"
            />
            <Calendar
                size={18}
                className="absolute right-3 top-1/2 transform -translate-y-1/2 text-gray-500 pointer-events-none"
            />
        </div>
    );
}