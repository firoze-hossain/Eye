// src/app/(auth)/layout.tsx
export default function AuthLayout({
                                       children,
                                   }: {
    children: React.ReactNode;
}) {
    return (
        <div className="min-h-screen bg-gradient-to-br from-dark-800 to-dark-900">
            {children}
        </div>
    );
}