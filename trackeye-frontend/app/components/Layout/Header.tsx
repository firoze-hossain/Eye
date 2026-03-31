'use client';

import { useState } from 'react';
import { Bell, Search, Calendar } from 'lucide-react';
import DatePicker from '../Common/DatePicker';

interface HeaderProps {
  title: string;
  onDateChange?: (date: Date) => void;
}

export default function Header({ title, onDateChange }: HeaderProps) {
  const [selectedDate, setSelectedDate] = useState<Date>(new Date());

  const handleDateChange = (date: Date) => {
    setSelectedDate(date);
    onDateChange?.(date);
  };

  return (
      <header className="bg-darker border-b border-gray-800 px-6 py-4">
        <div className="flex items-center justify-between">
          <h1 className="text-2xl font-semibold text-white">{title}</h1>

          <div className="flex items-center space-x-4">
            <div className="relative">
              <Search size={18} className="absolute left-3 top-1/2 transform -translate-y-1/2 text-gray-500" />
              <input
                  type="text"
                  placeholder="Search..."
                  className="bg-gray-800 text-white pl-10 pr-4 py-2 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary/50 w-64"
              />
            </div>

            <button className="relative p-2 hover:bg-gray-800 rounded-lg transition-colors">
              <Bell size={20} className="text-gray-400" />
              <span className="absolute top-1 right-1 w-2 h-2 bg-red-500 rounded-full"></span>
            </button>

            <div className="flex items-center space-x-3 pl-4 border-l border-gray-800">
              <div className="w-8 h-8 bg-primary rounded-full flex items-center justify-center">
                <span className="text-sm font-semibold">JD</span>
              </div>
            </div>
          </div>
        </div>
      </header>
  );
}