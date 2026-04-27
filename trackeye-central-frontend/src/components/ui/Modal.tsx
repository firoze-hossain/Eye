// src/components/ui/Modal.tsx
import { ReactNode } from 'react';
import { X } from 'lucide-react';

interface ModalProps {
    isOpen: boolean;
    onClose: () => void;
    title: string;
    children: ReactNode;
}

export default function Modal({ isOpen, onClose, title, children }: ModalProps) {
    if (!isOpen) return null;

    return (
        <div className="fixed inset-0 z-50 overflow-y-auto">
            <div className="flex items-center justify-center min-h-screen px-4">
                <div className="fixed inset-0 bg-black/50" onClick={onClose} />

                <div className="relative bg-white rounded-lg shadow-xl max-w-4xl w-full max-h-[90vh] overflow-auto">
                    <div className="flex items-center justify-between p-4 border-b border-dark-200">
                        <h3 className="text-lg font-semibold text-dark-900">{title}</h3>
                        <button
                            onClick={onClose}
                            className="p-1 hover:bg-dark-100 rounded-lg transition-colors"
                        >
                            <X className="w-5 h-5 text-dark-500" />
                        </button>
                    </div>
                    <div className="p-6">
                        {children}
                    </div>
                </div>
            </div>
        </div>
    );
}