// src/app/api/auth/[...nextauth]/route.ts
import NextAuth from 'next-auth';
import CredentialsProvider from 'next-auth/providers/credentials';
import { apiClient } from '@/lib/api';

const handler = NextAuth({
    providers: [
        CredentialsProvider({
            name: 'Credentials',
            credentials: {
                email: { label: "Email", type: "email" },
                password: { label: "Password", type: "password" }
            },
            async authorize(credentials) {
                try {
                    // Call your backend API to authenticate
                    const response = await fetch(`${process.env.NEXT_PUBLIC_API_URL}/api/auth/login`, {
                        method: 'POST',
                        headers: { 'Content-Type': 'application/json' },
                        body: JSON.stringify({
                            email: credentials?.email,
                            password: credentials?.password,
                        }),
                    });

                    const user = await response.json();

                    if (response.ok && user) {
                        return {
                            id: user.id,
                            email: user.email,
                            name: user.fullName,
                            role: user.role,
                            accessToken: user.token,
                        };
                    }
                    return null;
                } catch (error) {
                    console.error('Auth error:', error);
                    return null;
                }
            }
        }),
    ],
    callbacks: {
        async jwt({ token, user }) {
            if (user) {
                token.accessToken = user.accessToken;
                token.role = user.role;
                token.id = user.id;
            }
            return token;
        },
        async session({ session, token }) {
            session.accessToken = token.accessToken as string;
            session.user.role = token.role as string;
            session.user.id = token.id as number;
            return session;
        }
    },
    pages: {
        signIn: '/login',
        error: '/login',
    },
    session: {
        strategy: 'jwt',
        maxAge: 30 * 24 * 60 * 60, // 30 days
    },
    // FIX: without this, NextAuth strictly compares the incoming request's
    // host against NEXTAUTH_URL. This app is routinely opened from more than
    // one address (localhost:3000 on this machine, a LAN IP from another
    // device) - exactly the setup documented in .env.local.example. On a host
    // that doesn't match NEXTAUTH_URL, session/cookie handling can misbehave
    // in ways that look exactly like "logged out on reload." trustHost tells
    // NextAuth to trust the actual request host instead.
    trustHost: true,
    secret: process.env.NEXTAUTH_SECRET,
});

export { handler as GET, handler as POST };