import api from './api-instance';

const login = async (payload) => {
  try {
    if (!payload.username || !payload.password) {
      throw new Error('Vui lòng cung cấp đầy đủ thông tin đăng nhập');
    }
    const res = await api.post('/auth/login', payload);
    const user = res.data?.data;
    if (!user || Object.keys(user).length === 0) {
      throw new Error('Đăng nhập thất bại: thông tin người dùng không hợp lệ');
    }

    return {
      status: res.status,
      message: res.data.message || 'Đăng nhập thành công',
      data: user,
      isSuccess: res.data.status === 200,
      timestamp: res.data.timestamp || new Date().toISOString(),
    };
  } catch (error) {
    const errorMessage =
        error.response?.data?.message || error.message || 'Đăng nhập thất bại';
    throw new Error(errorMessage);
  }
};

const register = async (payload) => {
  try {
    const { username, password, email } = payload;
    if (!username || !password || !email) {
      throw new Error('Vui lòng cung cấp đầy đủ thông tin đăng ký');
    }
    if (username.length < 3) {
      throw new Error('Tên đăng nhập phải có ít nhất 3 ký tự');
    }
    if (password.length < 6) {
      throw new Error('Mật khẩu phải có ít nhất 6 ký tự');
    }
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
      throw new Error('Email không hợp lệ');
    }
    const res = await api.post('/auth/register', payload);
    return {
      status: res.status,
      message: res.data.message || 'Đăng ký thành công',
      data: res.data.data,
      isSuccess: res.data.isSuccess,
      timestamp: res.data.timestamp || new Date().toISOString(),
    };
  } catch (error) {
    const errorMessage =
        error.response?.data?.message || error.message || 'Đăng ký thất bại';
    throw new Error(errorMessage);
  }
};

const logout = async () => {
  try {
    await api.post('/auth/logout');
  } catch (error) {
    throw new Error(error.response?.data?.message || 'Đăng xuất thất bại');
  }
};

const forgotPassword = async (email) => {
  try {
    if (!email || !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
      throw new Error('Email không hợp lệ');
    }
    const res = await api.post('/auth/forgot-password', { email });
    return {
      status: res.status,
      message: res.data.message || 'Link đặt lại mật khẩu đã được gửi đến email của bạn',
      data: res.data.data,
      isSuccess: res.status === 200,
      timestamp: res.data.timestamp || new Date().toISOString(),
    };
  } catch (error) {
    const errorMessage =
        error.response?.data?.message ||
        error.message ||
        'Email không tồn tại hoặc không thể gửi email';
    throw new Error(errorMessage);
  }
};

const confirmResetPassword = async ({ token, newPassword, confirmPassword }) => {
  try {
    if (!token || !newPassword || !confirmPassword) {
      throw new Error('Vui lòng cung cấp đầy đủ thông tin đặt lại mật khẩu');
    }
    if (newPassword.length < 8) {
      throw new Error('Mật khẩu phải có ít nhất 8 ký tự');
    }
    if (newPassword !== confirmPassword) {
      throw new Error('Mật khẩu xác nhận không khớp');
    }
    const res = await api.post('/auth/reset-password/confirm', { token, newPassword, confirmPassword });
    return {
      status: res.status,
      message: res.data.message || 'Đặt lại mật khẩu thành công',
      data: res.data.data,
      isSuccess: res.status === 200,
      timestamp: res.data.timestamp || new Date().toISOString(),
    };
  } catch (error) {
    const errorMessage =
        error.response?.data?.message ||
        error.message ||
        'Không thể đặt lại mật khẩu';
    throw new Error(errorMessage);
  }
};

const getUser = async () => {
  try {
    const res = await api.get('/profile');
    return {
      status: res.status,
      message: res.data.message || 'Lấy thông tin người dùng thành công',
      data: res.data.data,
      isSuccess: res.status === 200 && res.data.data,
      timestamp: res.data.timestamp || new Date().toISOString(),
    };
  } catch (error) {
    throw new Error(error.response?.data?.message || 'Không thể lấy thông tin người dùng');
  }
};

const refreshToken = async () => {
  try {
    const res = await api.post('/auth/refresh', null);
    return {
      status: res.status,
      message: res.data.message || 'Refresh token thành công',
      data: res.data.data,
      isSuccess: res.status === 200 && res.data.data,
      timestamp: res.data.timestamp || new Date().toISOString(),
    };
  } catch (error) {
    throw new Error(error.response?.data?.message || 'Không thể refresh token');
  }
};

const getAvatar = async () => {
  try {
    const res = await api.get('/profile/avatar');
    return {
      status: res.status,
      message: res.data.message || 'Lấy avatar thành công',
      data: res.data.data,
      isSuccess: res.status === 200 && res.data.data,
      timestamp: res.data.timestamp || new Date().toISOString(),
    };
  } catch (error) {
    throw new Error(error.response?.data?.message || 'Không thể lấy avatar');
  }
};

const verifyEmail = async (token) => {
  try {
    const res = await api.get(`/auth/verify-email?token=${token}`);
    return {
      status: res.status,
      message: res.data.message || 'Xác thực email thành công',
      data: res.data.data,
      isSuccess: res.status === 200,
      timestamp: res.data.timestamp || new Date().toISOString(),
    };
  } catch (error) {
    throw new Error(error.response?.data?.message || 'Xác thực email thất bại');
  }
};

export default {
  login,
  register,
  logout,
  forgotPassword,
  confirmResetPassword,
  getUser,
  refreshToken,
  getAvatar,
  verifyEmail,
};
