import axios from 'axios';
import authStore from '../stores/auth-store';

const apiInstance = axios.create({
    baseURL: '/api/v1',
    headers: {
        'Content-Type': 'application/json',
    },
    withCredentials: true,
});

apiInstance.interceptors.response.use(
    (response) => response,
    async (error) => {
        const originalRequest = error.config;
        const url = originalRequest?.url || '';
        const isAuthEndpoint =
            url === '/auth/login' ||
            url === '/auth/register' ||
            url === '/auth/forgot-password' ||
            url === '/auth/reset-password' ||
            url === '/auth/reset-password/confirm' ||
            url.startsWith('/auth/verify-email');

        if (error.response?.status === 401 && originalRequest && !originalRequest._retry && url !== '/auth/refresh' && !isAuthEndpoint) {
            originalRequest._retry = true;
            try {
                await apiInstance.post('/auth/refresh', null);
                return apiInstance(originalRequest);
            } catch {
                authStore.getState().clearUser();
                window.dispatchEvent(new Event('auth:unauthorized'));
                return Promise.reject(new Error('Phiên đăng nhập hết hạn. Vui lòng đăng nhập lại.'));
            }
        }
        return Promise.reject(error);
    }
);

export default apiInstance;
