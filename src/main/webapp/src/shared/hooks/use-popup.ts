import { useCallback, useState } from 'react';

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

    const showPopup = useCallback(({
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
            onConfirm: onConfirm ?? null,
            onCancel: onCancel ?? null,
            confirmText,
            cancelText
        });
    }, []);

    const hidePopup = useCallback(() => {
        setPopup(prev => ({ ...prev, isVisible: false }));
    }, []);

    // Convenience methods
    const showSuccess = useCallback((message: string, title = 'Thành công') => {
        showPopup({ type: 'success', title, message });
    }, [showPopup]);

    const showError = useCallback((message: string, title = 'Lỗi') => {
        showPopup({ type: 'error', title, message });
    }, [showPopup]);

    const showWarning = useCallback((message: string, title = 'Cảnh báo') => {
        showPopup({ type: 'warning', title, message });
    }, [showPopup]);

    const showInfo = useCallback((message: string, title = 'Thông tin') => {
        showPopup({ type: 'info', title, message });
    }, [showPopup]);

    const showConfirm = useCallback((
        message: string,
        onConfirm: () => void,
        title = 'Xác nhận',
        confirmText = 'Xác nhận',
        cancelText = 'Hủy'
    ) => {
        showPopup({
            type: 'warning',
            title,
            message,
            showConfirm: true,
            onConfirm,
            confirmText,
            cancelText
        });
    }, [showPopup]);

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
