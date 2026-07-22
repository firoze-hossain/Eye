// // src/lib/api.ts
// import axios, { AxiosInstance, AxiosRequestConfig } from 'axios';
// import { getSession } from 'next-auth/react';
//
// class ApiClient {
//     private api: AxiosInstance;
//
//     constructor() {
//         this.api = axios.create({
//             baseURL: process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080',
//             headers: {
//                 'Content-Type': 'application/json',
//             },
//         });
//
//         // Log all requests for debugging
//         this.api.interceptors.request.use(
//             async (config) => {
//                 console.log(`🚀 [API Request] ${config.method?.toUpperCase()} ${config.url}`);
//                 console.log('📦 Request data:', config.data);
//
//                 const session = await getSession();
//                 if (session?.accessToken) {
//                     config.headers.Authorization = `Bearer ${session.accessToken}`;
//                 }
//                 return config;
//             },
//             (error) => {
//                 console.error('❌ [API Request Error]', error);
//                 return Promise.reject(error);
//             }
//         );
//
//         this.api.interceptors.response.use(
//             (response) => {
//                 console.log(`✅ [API Response] ${response.config.url}`, response.data);
//                 return response;
//             },
//             (error) => {
//                 console.error('❌ [API Response Error]', error.response?.data || error.message);
//                 if (error.response?.status === 401) {
//                     window.location.href = '/login';
//                 }
//                 return Promise.reject(error);
//             }
//         );
//     }
//
//     async get<T>(url: string, config?: AxiosRequestConfig): Promise<T> {
//         const response = await this.api.get<T>(url, config);
//         return response.data;
//     }
//
//     async post<T>(url: string, data?: any, config?: AxiosRequestConfig): Promise<T> {
//         const response = await this.api.post<T>(url, data, config);
//         return response.data;
//     }
//
//     async put<T>(url: string, data?: any, config?: AxiosRequestConfig): Promise<T> {
//         const response = await this.api.put<T>(url, data, config);
//         return response.data;
//     }
//
//     async delete<T>(url: string, config?: AxiosRequestConfig): Promise<T> {
//         const response = await this.api.delete<T>(url, config);
//         return response.data;
//     }
// }
//
// export const apiClient = new ApiClient();
//
// // API endpoints - use /api/backend prefix for backend calls
// export const API = {
//     // Auth endpoints are handled by NextAuth, NOT backend
//     auth: {
//         login: '/api/auth/login',
//         logout: '/api/auth/logout',
//         me: '/api/auth/me',
//     },
//     // Backend endpoints - use /api/backend prefix
//     public: {
//         register: '/api/backend/public/register',
//         registerDevice: '/api/backend/public/register-device',
//         health: '/api/backend/public/health',
//         verifyInvite: (token: string) => `/api/backend/public/verify-invite?token=${token}`,
//     },
//     dashboard: {
//         stats: '/api/backend/admin/dashboard',
//         live: '/api/backend/admin/live',
//     },
//     employees: {
//         list: '/api/backend/admin/employees',
//         details: (id: number) => `/api/backend/admin/employees/${id}`,
//         activities: (id: number, date: string) => `/api/backend/admin/employees/${id}/activities?date=${date}`,
//         screenshots: (id: number, date: string) => `/api/backend/admin/employees/${id}/screenshots?date=${date}`,
//         deactivate: (id: number) => `/api/backend/admin/employees/${id}/deactivate`,
//         activate: (id: number) => `/api/backend/admin/employees/${id}/activate`,
//         invite: '/api/backend/admin/invite',
//     },
//     reports: {
//         weekly: (userId?: number) => `/api/backend/admin/reports/weekly${userId ? `?userId=${userId}` : ''}`,
//     },
//     devices: {
//         revoke: (deviceId: number) => `/api/backend/admin/devices/${deviceId}/revoke`,
//     },
//     screenshots: {
//         image: (path: string) => `/api/backend/screenshots/image?path=${encodeURIComponent(path)}`,
//         organization: (date: string, page: number, size: number) =>
//             `/api/backend/screenshots/organization?date=${date}&page=${page}&size=${size}`,
//     },
// };

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

        this.api.interceptors.response.use(
            (response) => response,
            (error) => {
                if (error.response?.status === 401 && typeof window !== 'undefined') {
                    // FIX: don't blindly sign out on the first 401 - a request
                    // fired the instant a page reloads can occasionally race
                    // ahead of the session cookie being fully readable, and
                    // this would kill a perfectly valid session over a
                    // transient blip. Re-check getSession() itself first;
                    // only sign out if it agrees there's truly nothing there.
                    import('next-auth/react').then(async ({ getSession, signOut }) => {
                        const stillHasSession = await getSession();
                        if (!stillHasSession?.accessToken) {
                            signOut({ callbackUrl: '/login' });
                        }
                    }).catch(() => {
                        window.location.href = '/login';
                    });
                }

                // No `error.response` at all means the request never got a real
                // reply - almost always a CORS rejection or the server being
                // unreachable, NOT a business-logic error. Without this, these
                // show up as a generic, misleading "Could not update X" with no
                // clue why. Attach a clear reason so callers can surface it.
                if (!error.response) {
                    error.friendlyMessage =
                        'Could not reach the server. If you are viewing this dashboard from ' +
                        'a different address than usual, add it to app.frontend-origins on the ' +
                        'server (CORS) - otherwise check your network connection.';
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

/**
 * IMPORTANT FIX: these paths now match the real backend routes.
 *
 * The old code prefixed everything with "/api/backend/...", but the backend
 * only serves "/api/admin/**", "/api/screenshots/**" and "/api/public/**"
 * (the "backend" segment existed nowhere), so every dashboard/employee/
 * screenshot call returned 404. Prefix removed.
 *
 * Auth (login) is handled by NextAuth, which calls the backend itself, so the
 * "auth" entries here are unused by the axios client and kept only for reference.
 */
export const API = {
    auth: {
        login: '/api/auth/login',
        me: '/api/auth/me',
    },
    public: {
        register: '/api/public/register',
        registerDevice: '/api/public/register-device',
        health: '/api/public/health',
        verifyInvite: (token: string) => `/api/public/verify-invite?token=${token}`,
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
        browserActivities: (id: number, date: string) => `/api/admin/employees/${id}/browser-activities?date=${date}`,
        deactivate: (id: number) => `/api/admin/employees/${id}/deactivate`,
        activate: (id: number) => `/api/admin/employees/${id}/activate`,
        invite: '/api/admin/invite',
        deviceToken: (userId: number) => `/api/admin/device-token?forUserId=${userId}`,
        assignManager: (id: number) => `/api/admin/employees/${id}/manager`,
    },
    reports: {
        weekly: (userId?: number) => `/api/admin/reports/weekly${userId ? `?userId=${userId}` : ''}`,
    },
    devices: {
        revoke: (deviceId: number) => `/api/admin/devices/${deviceId}/revoke`,
        pause: (deviceId: number) => `/api/admin/devices/${deviceId}/pause`,
        resume: (deviceId: number) => `/api/admin/devices/${deviceId}/resume`,
        watchStart: (deviceId: number) => `/api/admin/devices/${deviceId}/watch/start`,
        watchRenew: (deviceId: number) => `/api/admin/devices/${deviceId}/watch/renew`,
        watchStop: (deviceId: number) => `/api/admin/devices/${deviceId}/watch/stop`,
        watchFrame: (deviceId: number) => `/api/admin/devices/${deviceId}/watch/frame`,
    },
    policyRules: {
        list: '/api/admin/policy-rules',
        create: '/api/admin/policy-rules',
        delete: (id: number) => `/api/admin/policy-rules/${id}`,
    },
    notifications: {
        list: '/api/admin/notifications',
        unreadCount: '/api/admin/notifications/unread-count',
        markRead: (id: number) => `/api/admin/notifications/${id}/read`,
        markAllRead: '/api/admin/notifications/read-all',
    },
    screenshots: {
        // Fetch a screenshot image by its record id (auth + org-ownership enforced
        // server-side). Use with the AuthenticatedImage component so the Bearer
        // token is sent - a plain <img src> can't send Authorization headers.
        image: (id: number) => `/api/screenshots/image?id=${id}`,
        organization: (date: string, page: number, size: number) =>
            `/api/screenshots/organization?date=${date}&page=${page}&size=${size}`,
    },
};