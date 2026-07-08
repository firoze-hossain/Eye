// src/context/AuthContext.tsx
'use client';

import React, { createContext, useContext, useEffect, useState } from 'react';
import { useSession, signIn, signOut } from 'next-auth/react';
import { useQueryClient } from 'react-query';
import { User } from '../types';

interface AuthContextType {
    user: User | null;
    isLoading: boolean;
    login: (email: string, password: string) => Promise<void>;
    logout: () => Promise<void>;
    hasPermission: (roles: string[]) => boolean;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export function AuthProvider({ children }: { children: React.ReactNode }) {
    const { data: session, status } = useSession();
    const [user, setUser] = useState<User | null>(null);
    const queryClient = useQueryClient();

    useEffect(() => {
        if (session?.user) {
            // FIX: NextAuth stores the display name on `user.name`, but the rest of
            // the app (and the User type) expects `fullName`. The old code did a bare
            // `session.user as User` cast, so fullName was always undefined and the
            // header rendered the literal fallback "User".
            const s = session.user as any;
            setUser({
                id: s.id,
                email: s.email,
                fullName: s.name ?? s.fullName ?? '',
                role: s.role,
                status: 'active',
                createdAt: 0,
            } as User);
        } else {
            setUser(null);
        }
    }, [session]);

    const login = async (email: string, password: string) => {
        const result = await signIn('credentials', {
            email,
            password,
            redirect: false,
        });

        if (result?.error) {
            throw new Error(result.error);
        }
    };

    const logout = async () => {
        // Drop every cached react-query result so the next person to sign in on this
        // browser never sees the previous user's employees/screenshots/activity.
        queryClient.clear();
        setUser(null);
        // FIX: the old code used { redirect: false }, so nothing navigated away and
        // you stayed on the dashboard after signing out. Let NextAuth do the redirect.
        await signOut({ callbackUrl: '/login' });
    };

    const hasPermission = (roles: string[]) => {
        if (!user) return false;
        return roles.includes(user.role);
    };

    return (
        <AuthContext.Provider
            value={{
                user,
                isLoading: status === 'loading',
                login,
                logout,
                hasPermission,
            }}
        >
            {children}
        </AuthContext.Provider>
    );
}

export function useAuth() {
    const context = useContext(AuthContext);
    if (context === undefined) {
        throw new Error('useAuth must be used within an AuthProvider');
    }
    return context;
}
