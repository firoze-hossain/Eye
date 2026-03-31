import axios from 'axios';

const api = axios.create({
    baseURL: process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8765/api',
    timeout: 10000,
    headers: {
        'Content-Type': 'application/json',
    },
});

// Response interceptor for error handling
api.interceptors.response.use(
    (response) => response.data,
    (error) => {
        console.error('API Error:', error.response?.data || error.message);
        throw error;
    }
);

export interface StatsResponse {
    totalSeconds: number;
    totalMinutes: number;
    totalHours: number;
    topApps: Array<{
        appName: string;
        totalMs: number;
        sessions: number;
    }>;
    date: string;
}

export interface ActivitySession {
    id: number;
    appName: string;
    windowTitle: string;
    processName: string;
    startTime: number;
    endTime: number;
    durationMs: number;
}

export interface ScreenshotRecord {
    id: number;
    timestamp: number;
    filePath: string;
    windowTitle: string;
    processName: string;
}

export default api;