// src/components/ui/Card.tsx
import { ReactNode } from 'react';
import clsx from 'clsx';

interface CardProps {
    children: ReactNode;
    className?: string;
    onClick?: () => void;
}

export default function Card({ children, className, onClick }: CardProps) {
    return (
        <div
            onClick={onClick}
            className={clsx(
                'bg-white rounded-lg shadow-sm border border-dark-200',
                onClick && 'cursor-pointer hover:shadow-md transition-shadow',
                className
            )}
        >
            {children}
        </div>
    );
}