// next.config.js
/** @type {import('next').NextConfig} */
const nextConfig = {
    reactStrictMode: true,
    images: {
        // Screenshots are rendered via <AuthenticatedImage> (blob fetch), not
        // next/image, so remotePatterns aren't required for them. Kept for any
        // other same-host images.
        remotePatterns: [
            { protocol: 'http', hostname: 'localhost' },
        ],
    },

    // NOTE: the previous config had:
    //   async rewrites() { return [{ source: '/api/:path*', destination: 'http://localhost:8080/api/:path*' }]; }
    // That proxied EVERY /api/* call (including NextAuth's /api/auth/*) to the
    // backend, which is wrong. The axios client talks to the backend directly
    // via NEXT_PUBLIC_API_URL + CORS, so no rewrite is needed. Removed.
    //
    // If you prefer a same-origin proxy (browser only talks to :3000, no CORS),
    // use this instead and set baseURL to '' in src/lib/api.ts and the '/api/backend'
    // prefix on the API paths:
    //
    // async rewrites() {
    //   const backend = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080';
    //   return [{ source: '/api/backend/:path*', destination: `${backend}/api/:path*` }];
    // },
};

module.exports = nextConfig;
