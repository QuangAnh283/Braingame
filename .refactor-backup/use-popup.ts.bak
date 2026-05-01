import { useState } from 'react';

type PopupState = {
    isVisible: boolean;
    type: string;
    title: string;
    message: string;
    showConfirm: boolean;
    onConfirm: (() => void) | null;
    onCancel: (() => void) | null;
    confirmText: string;
    cancelText: string;
};

type ShowPopupOptions = {
    type?: string;
    title: string;
    message: string;
    showConfirm?: boolean;
    onConfirm?: (() => void) | null;
    onCancel?: (() => void) | null;
    confirmText?: string;
    cancelText?: string;
};

export const usePopup = () => {
    const [popup, setPopup] = useState<PopupState>({
        isVisible: false,
        type: 'info',
        title: '',
        message: '',
        showConfirm: false,
        onConfirm: null,
        onCancel: null,
        confirmText: 'Xác nhận',
        cancelText: 'Hủy'
    });

    const showPopup = ({
        type = 'info',
        title,
        message,
        showConfirm = false,
        onConfirm,
        onCancel,
        confirmText = 'Xác nhận',
        cancelText = 'Hủy'
    }: ShowPopupOptions) => {
        setPopup({
            isVisible: true,
            type,
            title,
            message,
            showConfirm,
            onConfirm,
            onCancel,
            confirmText,
            cancelText
        });
    };

    const hidePopup = () => {
        setPopup(prev => ({ ...prev, isVisible: false }));
    };

    // Convenience methods
    const showSuccess = (message, title = 'Thành công') => {
        showPopup({ type: 'success', title, message });
    };

    const showError = (message, title = 'Lỗi') => {
        showPopup({ type: 'error', title, message });
    };

    const showWarning = (message, title = 'Cảnh báo') => {
        showPopup({ type: 'warning', title, message });
    };

    const showInfo = (message, title = 'Thông tin') => {
        showPopup({ type: 'info', title, message });
    };

    const showConfirm = (message, onConfirm, title = 'Xác nhận', confirmText = 'Xác nhận', cancelText = 'Hủy') => {
        showPopup({
            type: 'warning',
            title,
            message,
            showConfirm: true,
            onConfirm,
            confirmText,
            cancelText
        });
    };

    return {
        popup,
        showPopup,
        hidePopup,
        showSuccess,
        showError,
        showWarning,
        showInfo,
        showConfirm
    };
};