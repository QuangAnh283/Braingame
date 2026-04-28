import { useEffect, useState, useCallback, useRef } from 'react';
import { FiCheckCircle, FiLoader, FiXCircle } from 'react-icons/fi';
import { useNavigate, useSearchParams } from 'react-router-dom';
import authApi from '../../services/auth-api';
import '../../styles/pages/verify-email.css';

const VerifyEmail = () => {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const [status, setStatus] = useState('loading');
  const [message, setMessage] = useState('');
  const processedTokenRef = useRef<string | null>(null);

  const handleVerifyEmail = useCallback(async (token) => {
    try {
      const response = await authApi.verifyEmail(token);

      if (response.isSuccess) {
        setStatus('success');
        setMessage('Email đã được xác thực thành công!');

        setTimeout(() => {
          navigate('/login');
        }, 3000);
      } else {
        setStatus('error');
        setMessage(response.message || 'Xác thực email thất bại.');
      }
    } catch (error) {
      setStatus('error');
      setMessage(error.message || 'Có lỗi xảy ra khi xác thực email.');
    }
  }, [navigate]);

  useEffect(() => {
    const token = searchParams.get('token');

    if (!token) {
      setStatus('error');
      setMessage('Token xác thực không hợp lệ.');
      return;
    }

    // Prevent duplicate verification calls in development StrictMode.
    if (processedTokenRef.current === token) {
      return;
    }
    processedTokenRef.current = token;

    handleVerifyEmail(token);
  }, [handleVerifyEmail, searchParams]);

  const handleGoToLogin = () => {
    navigate('/login');
  };

  const handleResendEmail = async () => {
    setStatus('loading');
    setMessage('Đang gửi lại email...');
    setTimeout(() => {
      setStatus('error');
      setMessage('Vui lòng liên hệ hỗ trợ để gửi lại email xác thực.');
    }, 1000);
  };

  return (
    <div className="verify-email-container">
      <div className="verify-email-card">
        {status === 'loading' && (
          <div className="verify-content loading">
            <FiLoader className="icon spin" />
            <h2>Đang xác thực email...</h2>
            <p>Vui lòng đợi trong giây lát.</p>
          </div>
        )}

        {status === 'success' && (
          <div className="verify-content success">
            <FiCheckCircle className="icon" />
            <h2>Xác thực thành công!</h2>
            <p>{message}</p>
            <p className="redirect-message">Bạn sẽ được chuyển đến trang đăng nhập trong vài giây...</p>
            <button onClick={handleGoToLogin} className="btn-primary">
              Đăng nhập ngay
            </button>
          </div>
        )}

        {status === 'error' && (
          <div className="verify-content error">
            <FiXCircle className="icon" />
            <h2>Xác thực thất bại</h2>
            <p>{message}</p>
            <div className="error-actions">
              <button onClick={handleGoToLogin} className="btn-primary">
                Đăng nhập
              </button>
              <button onClick={handleResendEmail} className="btn-secondary">
                Gửi lại email
              </button>
            </div>
          </div>
        )}
      </div>
    </div>
  );
};

export default VerifyEmail;

