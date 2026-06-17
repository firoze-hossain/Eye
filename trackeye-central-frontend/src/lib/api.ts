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

        // Log all requests for debugging
        this.api.interceptors.request.use(
            async (config) => {
                console.log(`🚀 [API Request] ${config.method?.toUpperCase()} ${config.url}`);
                console.log('📦 Request data:', config.data);

                const session = await getSession();
                if (session?.accessToken) {
                    config.headers.Authorization = `Bearer ${session.accessToken}`;
                }
                return config;
            },
            (error) => {
                console.error('❌ [API Request Error]', error);
                return Promise.reject(error);
            }
        );

        this.api.interceptors.response.use(
            (response) => {
                console.log(`✅ [API Response] ${response.config.url}`, response.data);
                return response;
            },
            (error) => {
                console.error('❌ [API Response Error]', error.response?.data || error.message);
                if (error.response?.status === 401) {
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

// API endpoints - use /api/backend prefix for backend calls
export const API = {
    // Auth endpoints are handled by NextAuth, NOT backend
    auth: {
        login: '/api/auth/login',
        logout: '/api/auth/logout',
        me: '/api/auth/me',
    },
    // Backend endpoints - use /api/backend prefix
    public: {
        register: '/api/backend/public/register',
        registerDevice: '/api/backend/public/register-device',
        health: '/api/backend/public/health',
        verifyInvite: (token: string) => `/api/backend/public/verify-invite?token=${token}`,
    },
    dashboard: {
        stats: '/api/backend/admin/dashboard',
        live: '/api/backend/admin/live',
    },
    employees: {
        list: '/api/backend/admin/employees',
        details: (id: number) => `/api/backend/admin/employees/${id}`,
        activities: (id: number, date: string) => `/api/backend/admin/employees/${id}/activities?date=${date}`,
        screenshots: (id: number, date: string) => `/api/backend/admin/employees/${id}/screenshots?date=${date}`,
        deactivate: (id: number) => `/api/backend/admin/employees/${id}/deactivate`,
        activate: (id: number) => `/api/backend/admin/employees/${id}/activate`,
        invite: '/api/backend/admin/invite',
    },
    reports: {
        weekly: (userId?: number) => `/api/backend/admin/reports/weekly${userId ? `?userId=${userId}` : ''}`,
    },
    devices: {
        revoke: (deviceId: number) => `/api/backend/admin/devices/${deviceId}/revoke`,
    },
    screenshots: {
        image: (path: string) => `/api/backend/screenshots/image?path=${encodeURIComponent(path)}`,
        organization: (date: string, page: number, size: number) =>
            `/api/backend/screenshots/organization?date=${date}&page=${page}&size=${size}`,
    },
};