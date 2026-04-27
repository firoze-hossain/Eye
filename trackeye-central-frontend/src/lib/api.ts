// src/lib/api.ts
import axios, { AxiosInstance, AxiosRequestConfig } from 'axios';
import { getSession } from 'next-auth/react';

class ApiClient {
    private api: AxiosInstance;

    constructor() {
        this.api = axios.create({
            baseURL: process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080',
            headers: {
                'Content-Type': 'application/json',
            },
        });

        // Request interceptor to add auth token
        this.api.interceptors.request.use(
            async (config) => {
                const session = await getSession();
                if (session?.accessToken) {
                    config.headers.Authorization = `Bearer ${session.accessToken}`;
                }
                return config;
            },
            (error) => Promise.reject(error)
        );

        // Response interceptor for error handling
        this.api.interceptors.response.use(
            (response) => response,
            (error) => {
                if (error.response?.status === 401) {
                    // Redirect to login
                    window.location.href = '/login';
                }
                return Promise.reject(error);
            }
        );
    }

    async get<T>(url: string, config?: AxiosRequestConfig): Promise<T> {
        const response = await this.api.get<T>(url, config);
        return response.data;
    }

    async post<T>(url: string, data?: any, config?: AxiosRequestConfig): Promise<T> {
        const response = await this.api.post<T>(url, data, config);
        return response.data;
    }

    async put<T>(url: string, data?: any, config?: AxiosRequestConfig): Promise<T> {
        const response = await this.api.put<T>(url, data, config);
        return response.data;
    }

    async delete<T>(url: string, config?: AxiosRequestConfig): Promise<T> {
        const response = await this.api.delete<T>(url, config);
        return response.data;
    }
}

export const apiClient = new ApiClient();

// API endpoints
export const API = {
    auth: {
        login: '/api/auth/login',
        logout: '/api/auth/logout',
        me: '/api/auth/me',
    },
    dashboard: {
        stats: '/api/admin/dashboard',
        live: '/api/admin/live',
    },
    employees: {
        list: '/api/admin/employees',
        details: (id: number) => `/api/admin/employees/${id}`,
        activities: (id: number, date: string) => `/api/admin/employees/${id}/activities?date=${date}`,
        screenshots: (id: number, date: string) => `/api/admin/employees/${id}/screenshots?date=${date}`,
        deactivate: (id: number) => `/api/admin/employees/${id}/deactivate`,
        activate: (id: number) => `/api/admin/employees/${id}/activate`,
        invite: '/api/admin/invite',
    },
    reports: {
        weekly: (userId?: number) => `/api/admin/reports/weekly${userId ? `?userId=${userId}` : ''}`,
    },
    devices: {
        revoke: (deviceId: number) => `/api/admin/devices/${deviceId}/revoke`,
    },
    screenshots: {
        image: (path: string) => `/api/screenshots/image?path=${encodeURIComponent(path)}`,
        organization: (date: string, page: number, size: number) =>
            `/api/screenshots/organization?date=${date}&page=${page}&size=${size}`,
    },
};