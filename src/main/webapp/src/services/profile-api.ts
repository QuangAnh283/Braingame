import apiInstance from './api-instance';
import authStore from '../stores/auth-store';

const validateEmail = (email) => /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email);
const validatePhone = (phone) => /^\+?\d{10,15}$/.test(phone);
const validateFullName = (fullName) => fullName?.trim().length >= 2;

const profileApi = {
    getMyProfile: async () => {
        try {
            const response = await apiInstance.get('/profile', {
                headers: {
                    'Cache-Control': 'max-age=300',
                },
            });
            const data = response.data;

            if (response.status === 200 && data.data) {
                authStore.getState().setUser(data.data);
            }

            return {
                status: response.status,
                message: data.message || 'Success',
                data: data.data,
                isSuccess: response.status === 200 && data.data,
                timestamp: data.timestamp || new Date().toISOString(),
            };
        } catch (error) {
            const errorMessage =
                error.response?.data?.message || error.message || 'Lỗi khi tải thông tin hồ sơ';
            return {
                status: error.response?.status || 500,
                message: errorMessage,
                data: null,
                isSuccess: false,
                timestamp: new Date().toISOString(),
            };
        }
    },

    searchUser: async (username, limit = 10) => {
        if (!username?.trim()) {
            return {
                status: 400,
                message: 'Vui lòng nhập từ khóa tìm kiếm',
                data: [],
                isSuccess: false,
                timestamp: new Date().toISOString(),
            };
        }
        try {
            const response = await apiInstance.get(`/profile/search/${encodeURIComponent(username)}?limit=${limit}`);
            return {
                status: response.status,
                message: response.data.message || 'Success',
                data: response.data.data || [],
                isSuccess: response.data.isSuccess,
                timestamp: response.data.timestamp || new Date().toISOString(),
            };
        } catch (error) {
            const errorMessage =
                error.response?.data?.message || error.message || 'Lỗi khi tìm kiếm người dùng';
            return {
                status: error.response?.status || 500,
                message: errorMessage,
                data: [],
                isSuccess: false,
                timestamp: new Date().toISOString(),
            };
        }
    },

    getUserProfile: async (userId) => {
        if (!userId) {
            return {
                status: 400,
                message: 'Vui lòng cung cấp userId',
                data: null,
                isSuccess: false,
                timestamp: new Date().toISOString(),
            };
        }
        try {
            const response = await apiInstance.get(`/profile/${userId}`);
            return {
                status: response.status,
                message: response.data.message || 'Success',
                data: response.data.data,
                isSuccess: response.data.isSuccess,
                timestamp: response.data.timestamp || new Date().toISOString(),
            };
        } catch (error) {
            const errorMessage =
                error.response?.data?.message || error.message || 'Lỗi khi tải thông tin hồ sơ người dùng';
            return {
                status: error.response?.status || 500,
                message: errorMessage,
                data: null,
                isSuccess: false,
                timestamp: new Date().toISOString(),
            };
        }
    },

    updateProfile: async (profileData) => {
        try {
            const { fullName, email, phoneNumber, address, dob } = profileData;
            if (fullName && !validateFullName(fullName)) {
                throw new Error('Họ tên phải có ít nhất 2 ký tự');
            }
            if (email && !validateEmail(email)) {
                throw new Error('Email không hợp lệ');
            }
            if (phoneNumber && !validatePhone(phoneNumber)) {
                throw new Error('Số điện thoại không hợp lệ');
            }
            const requestData = {
                fullName: fullName?.trim(),
                email: email?.trim(),
                phoneNumber: phoneNumber?.trim(),
                address: address?.trim(),
                dob: dob, // Thêm trường dob
            };

            // Sửa endpoint từ /profile/update thành /profile theo API spec
            const response = await apiInstance.put('/profile', requestData, {
                headers: {
                    'Content-Type': 'application/json',
                },
            });


            const data = response.data;

            // Kiểm tra response theo format mới
            if (response.status === 200 && data.status === 200) {
                // Cập nhật user state
                if (data.data) {
                    authStore.getState().setUser(data.data);
                }

                return {
                    status: response.status,
                    message: data.message || 'Cập nhật hồ sơ thành công',
                    data: data.data,
                    isSuccess: true,
                    timestamp: new Date().toISOString(),
                };
            } else {
                throw new Error(data.message || 'Cập nhật thất bại');
            }
        } catch (error) {
            const errorMessage =
                error.response?.data?.message || error.message || 'Lỗi khi cập nhật thông tin hồ sơ';
            return {
                status: error.response?.status || 500,
                message: errorMessage,
                data: null,
                isSuccess: false,
                timestamp: new Date().toISOString(),
            };
        }
    },

    changePassword: async (passwordData) => {
        try {
            const { currentPassword, newPassword } = passwordData;
            if (!currentPassword || !newPassword) {
                throw new Error('Vui lòng cung cấp đầy đủ mật khẩu');
            }
            const requestData = { currentPassword, newPassword };
            const response = await apiInstance.put('/profile/password', requestData, {
                headers: {
                    'Content-Type': 'application/json',
                },
            });
            return {
                status: response.status,
                message: response.data.message || 'Đổi mật khẩu thành công',
                data: response.data.data,
                isSuccess: response.data.isSuccess,
                timestamp: response.data.timestamp || new Date().toISOString(),
            };
        } catch (error) {
            const errorMessage =
                error.response?.data?.message || error.message || 'Lỗi khi đổi mật khẩu';
            return {
                status: error.response?.status || 500,
                message: errorMessage,
                data: null,
                isSuccess: false,
                timestamp: new Date().toISOString(),
            };
        }
    },

    getAvatar: async () => {
        try {
            const response = await apiInstance.get('/profile/avatar');
            return {
                status: response.status,
                message: 'Avatar loaded successfully',
                data: response.data?.data || response.data,
                isSuccess: response.status === 200,
                timestamp: new Date().toISOString(),
            };
        } catch (error) {
            const errorMessage =
                error.response?.data?.message || error.message || 'Lỗi khi tải avatar';
            return {
                status: error.response?.status || 500,
                message: errorMessage,
                data: null,
                isSuccess: false,
                timestamp: new Date().toISOString(),
            };
        }
    },
};

export default profileApi;
