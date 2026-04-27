// src/types/index.ts
export interface User {
    id: number;
    email: string;
    fullName: string;
    role: 'admin' | 'manager' | 'employee';
    status: 'active' | 'inactive' | 'invited';
    createdAt: number;
    lastLoginAt?: number;
}

export interface Device {
    id: number;
    deviceName: string;
    deviceIdentifier: string;
    osType: string;
    lastSeenAt: number;
    isActive: boolean;
    createdAt: number;
}

export interface Activity {
    id: number;
    appName: string;
    windowTitle: string;
    processName: string;
    startTime: number;
    endTime: number;
    durationMs: number;
}

export interface Screenshot {
    id: number;
    screenshotUrl: string;
    timestamp: number;
    windowTitle: string;
    processName: string;
    userFullName?: string;
    deviceName?: string;
}

export interface DashboardStats {
    summary: {
        totalActiveUsers: number;
        totalDevices: number;
        totalMinutesTracked: number;
        totalScreenshots: number;
        averageProductivity: number;
    };
    topActivities: Array<{
        appName: string;
        totalMinutes: number;
        userCount: number;
    }>;
    onlineUsers: Array<{
        userId: number;
        fullName: string;
        currentApp: string;
        lastActivityAt: number;
        isActive: boolean;
    }>;
    productivityScore: {
        score: number;
        grade: string;
        productiveMinutes: number;
        unproductiveMinutes: number;
        neutralMinutes: number;
    };
}

export interface ApiResponse<T> {
    success: boolean;
    data?: T;
    error?: string;
    message?: string;
}