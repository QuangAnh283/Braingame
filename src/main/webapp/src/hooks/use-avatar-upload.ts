import { useState, useEffect, useCallback } from 'react';
import authApi from '../services/auth-api';
import apiInstance from '../services/api-instance';
import authStore from '../stores/auth-store';

export const useAvatarUpload = (profileData) => {
    const { isAuthenticated } = authStore();

    // State cho upload avatar
    const [uploadingAvatar, setUploadingAvatar] = useState(false);
    const [uploadError, setUploadError] = useState('');
    const [successMessage, setSuccessMessage] = useState('');
    const [showSuccessModal, setShowSuccessModal] = useState(false);

    // State cho avatar
    const [avatarUrl, setAvatarUrl] = useState(null);
    const [avatarLoading, setAvatarLoading] = useState(false);

    // Hàm xử lý avatar URL
    const getAvatarUrl = useCallback((avatarURL) => {
        if (!avatarURL) return '';
        if (avatarURL.startsWith('http://') || avatarURL.startsWith('https://')) {
            return avatarURL;
        }
        return '';
    }, []);

    // Hàm fetch avatar
    const fetchAvatar = useCallback(async () => {
        if (avatarUrl || avatarLoading || !profileData?.avatarURL) return;

        try {
            setAvatarLoading(true);

            const response = await authApi.getAvatar();

            if (response.isSuccess && response.data) {
                setAvatarUrl(response.data);
                return;
            }

            if (response.status === 200 && response.data) {
                try {
                    new URL(response.data);
                    setAvatarUrl(response.data);
                } catch {
                    setAvatarUrl(null);
                }
            } else {
                setAvatarUrl(null);
            }
        } catch {
            setAvatarUrl(null);
        } finally {
            setAvatarLoading(false);
        }
    }, [avatarUrl, avatarLoading, profileData?.avatarURL]);

    // Hàm refresh avatar (luôn fetch lại từ API)
    const refreshAvatar = useCallback(async () => {
        try {
            setAvatarLoading(true);

            const response = await authApi.getAvatar();

            if (response.isSuccess && response.data) {
                setAvatarUrl(response.data);
                return response.data;
            }

            if (response.status === 200 && response.data) {
                try {
                    new URL(response.data);
                    setAvatarUrl(response.data);
                    return response.data;
                } catch {
                    setAvatarUrl(null);
                    return null;
                }
            } else {
                setAvatarUrl(null);
                return null;
            }
        } catch {
            setAvatarUrl(null);
            return null;
        } finally {
            setAvatarLoading(false);
        }
    }, []);

    // Fetch avatar khi có profileData
    useEffect(() => {
        if (profileData?.avatarURL && !avatarUrl && !avatarLoading) {
            fetchAvatar();
        }
    }, [profileData?.avatarURL, avatarUrl, avatarLoading, fetchAvatar]);

    // Hàm validate file ảnh
    const validateImageFile = useCallback((file) => {
        const maxSize = 5 * 1024 * 1024; // 5MB

        if (!file || !file.type || !file.type.startsWith('image/')) {
            throw new Error('Chỉ chấp nhận file ảnh (image/*)');
        }

        if (file.size > maxSize) {
            throw new Error('File ảnh không được vượt quá 5MB');
        }

        return true;
    }, []);

    // Hàm xử lý chọn file ảnh và upload trực tiếp
    const handleFileSelect = useCallback(async (event, setUser) => {
        const file = event.target.files[0];
        if (!file) return;

        try {
            // Validate file
            validateImageFile(file);

            // Set loading state
            setUploadingAvatar(true);
            setUploadError('');
            setSuccessMessage('');

            // Tạo FormData
            const formData = new FormData();
            formData.append('file', file, 'avatar.jpg');

            // Upload trực tiếp
            const response = await apiInstance.post('/profile/avatar', formData, {
                headers: {
                    'Content-Type': 'multipart/form-data',
                },
            });

            // Kiểm tra response - điều chỉnh logic dựa trên backend response
            if (response.status === 200 || response.data?.success || response.data?.data?.avatarURL || response.data?.data?.avatarUrl || response.data?.avatarURL || response.data?.avatarUrl) {

                // Lấy avatarUrl từ response (có thể ở nhiều vị trí khác nhau)
                const newAvatarUrl = response.data?.data?.avatarURL || response.data?.data?.avatarUrl || response.data?.avatarURL || response.data?.avatarUrl || response.data?.url;

                if (newAvatarUrl) {
                    setAvatarUrl(newAvatarUrl);

                    if (typeof setUser === 'function') {
                        setUser(prev => ({ ...prev, avatarURL: newAvatarUrl }));
                    }
                }

                // Luôn refresh avatar từ API để đảm bảo có URL mới nhất
                await refreshAvatar();

                setSuccessMessage('Avatar đã được cập nhật thành công!');
                setShowSuccessModal(true);

                // Trigger Header to refresh avatar immediately
                window.dispatchEvent(new CustomEvent('avatarUpdated'));

                setTimeout(() => {
                    setSuccessMessage('');
                    setShowSuccessModal(false);
                }, 3000);
            } else {
                throw new Error(response.data?.message || 'Upload failed');
            }

        } catch (error) {
            setUploadError(error?.response?.data?.message || error?.message || 'Lỗi khi tải avatar');
        } finally {
            setUploadingAvatar(false);
            event.target.value = ''; // Reset input
        }
    }, [refreshAvatar, validateImageFile]);

    // Hàm đóng success modal
    const closeSuccessModal = useCallback(() => {
        setShowSuccessModal(false);
        setSuccessMessage('');
    }, []);
    useEffect(() => {
        if (!isAuthenticated || !profileData?.avatarURL) return;

        const refreshInterval = setInterval(() => {
            refreshAvatar();
        }, 45 * 60 * 1000);

        return () => clearInterval(refreshInterval);
    }, [isAuthenticated, profileData?.avatarURL, refreshAvatar]);

    return {
        // Avatar display
        avatarUrl,
        avatarLoading,
        getAvatarUrl,

        // Upload states
        uploadingAvatar,
        uploadError,
        setUploadError,
        successMessage,
        setSuccessMessage,
        showSuccessModal,
        closeSuccessModal,

        // Handlers
        handleFileSelect,
        refreshAvatar
    };
};
