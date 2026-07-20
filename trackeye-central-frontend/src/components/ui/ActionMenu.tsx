// src/components/ui/ActionMenu.tsx
'use client';

import { useEffect, useRef, useState } from 'react';
import { createPortal } from 'react-dom';
import { MoreVertical } from 'lucide-react';

export interface ActionMenuItem {
    label: string;
    icon?: React.ReactNode;
    onClick: () => void;
    danger?: boolean;
    disabled?: boolean;
}

/**
 * A "..." action menu that always renders correctly, even inside a scrolling
 * table (`overflow-x-auto`). Any `position: absolute` dropdown nested inside
 * such a container gets clipped, because setting overflow on one axis forces
 * the browser to clip the other axis too - that's exactly what was hiding
 * this menu. Rendering through a portal straight to <body>, positioned with
 * `fixed` coordinates taken from the trigger's real position on screen, sidesteps
 * the problem entirely: nothing can clip it.
 */
export default function ActionMenu({
    items,
    width = 208,
}: {
    items: ActionMenuItem[];
    width?: number;
}) {
    const [open, setOpen] = useState(false);
    const [pos, setPos] = useState({ top: 0, left: 0 });
    const anchorRef = useRef<HTMLButtonElement>(null);

    const toggle = () => {
        if (!open && anchorRef.current) {
            const rect = anchorRef.current.getBoundingClientRect();
            const left = Math.max(8, rect.right - width);
            const spaceBelow = window.innerHeight - rect.bottom;
            const menuHeight = items.length * 42 + 16;
            const top = spaceBelow < menuHeight ? rect.top - menuHeight - 6 : rect.bottom + 6;
            setPos({ top, left });
        }
        setOpen((o) => !o);
    };

    useEffect(() => {
        if (!open) return;
        const close = () => setOpen(false);
        const onKey = (e: KeyboardEvent) => { if (e.key === 'Escape') setOpen(false); };
        window.addEventListener('scroll', close, true);
        window.addEventListener('resize', close);
        window.addEventListener('keydown', onKey);
        return () => {
            window.removeEventListener('scroll', close, true);
            window.removeEventListener('resize', close);
            window.removeEventListener('keydown', onKey);
        };
    }, [open]);

    return (
        <>
            <button
                ref={anchorRef}
                onClick={toggle}
                className="p-2 hover:bg-dark-100 rounded-lg transition-colors"
                aria-label="Actions"
            >
                <MoreVertical className="w-4 h-4 text-dark-500" />
            </button>

            {open && typeof document !== 'undefined' && createPortal(
                <>
                    <div className="fixed inset-0 z-40" onClick={() => setOpen(false)} />
                    <div
                        className="fixed z-50 bg-white rounded-xl shadow-lg ring-1 ring-black/5 border border-dark-100 py-1.5 overflow-hidden"
                        style={{ top: pos.top, left: pos.left, width }}
                    >
                        {items.map((item, i) => (
                            <button
                                key={i}
                                disabled={item.disabled}
                                onClick={() => { setOpen(false); item.onClick(); }}
                                className={`w-full flex items-center gap-2.5 px-3.5 py-2.5 text-sm text-left transition-colors disabled:opacity-40 disabled:cursor-not-allowed ${
                                    item.danger
                                        ? 'text-red-600 hover:bg-red-50'
                                        : 'text-dark-700 hover:bg-dark-50'
                                } ${i > 0 && item.danger ? 'border-t border-dark-100 mt-1 pt-2.5' : ''}`}
                            >
                                {item.icon}
                                {item.label}
                            </button>
                        ))}
                    </div>
                </>,
                document.body
            )}
        </>
    );
}
