// src/types/next-auth.d.ts
import 'next-auth';

declare module 'next-auth' {
    interface User {
        id: number;
        role: string;
        accessToken: string;
    }

    interface Session {
        accessToken: string;
        user: {
            id: number;
            email: string;
            name: string;
            role: string;
        }
    }
}