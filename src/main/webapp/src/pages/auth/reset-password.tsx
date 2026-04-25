import { useCallback, useMemo, useState } from 'react';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import { FaBrain, FaExclamationCircle, FaEye, FaEyeSlash, FaLock } from 'react-icons/fa';
import { IoArrowBackSharp } from 'react-icons/io5';
import { CheckCircle } from 'lucide-react';
import authApi from '../../services/auth-api';
import { usePopup } from '@shared/hooks/use-popup';
import PopupNotification from '@shared/components/PopupNotification';
import Decoration from '../../shared/components/Decoration';
import '../../styles/pages/auth/forgot-password.css';

function ResetPassword() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const token = useMemo(() => searchParams.get('token') || '', [searchParams]);
  const [formData, setFormData] = useState({ newPassword: '', confirmPassword: '' });
  const [error, setError] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [isComplete, setIsComplete] = useState(false);
  const [showPassword, setShowPassword] = useState(false);
  const { popup, showSuccess, showError, hidePopup } = usePopup();

  const handleChange = useCallback((event) => {
    const { name, value } = event.target;
    setFormData((prev) => ({ ...prev, [name]: value }));
    setError('');
  }, []);

  const validateForm = useCallback(() => {
    if (!token) {
      setError('Token đặt lại mật khẩu không hợp lệ');
      return false;
    }
    if (formData.newPassword.length < 8) {
      setError('Mật khẩu phải có ít nhất 8 ký tự');
      return false;
    }
    if (formData.newPassword !== formData.confirmPassword) {
      setError('Mật khẩu xác nhận không khớp');
      return false;
    }
    return true;
  }, [formData.confirmPassword, formData.newPassword, token]);

  const handleSubmit = useCallback(
    async (event) => {
      event.preventDefault();
      if (!validateForm()) return;

      setIsSubmitting(true);
      setError('');

      try {
        await authApi.confirmResetPassword({
          token,
          newPassword: formData.newPassword,
          confirmPassword: formData.confirmPassword,
        });
        setIsComplete(true);
        showSuccess('Mật khẩu đã được đặt lại thành công.');
      } catch (err) {
        const errorMessage = err.message || 'Không thể đặt lại mật khẩu';
        setError(errorMessage);
        showError(errorMessage);
      } finally {
        setIsSubmitting(false);
      }
    },
    [formData.confirmPassword, formData.newPassword, showError, showSuccess, token, validateForm]
  );

  return (
    <div className="fp-container">
      <Decoration />
      <PopupNotification
        type={popup.type}
        title={popup.title}
        message={popup.message}
        isVisible={popup.isVisible}
        onClose={hidePopup}
        showConfirm={popup.showConfirm}
        onConfirm={popup.onConfirm}
        onCancel={popup.onCancel}
        confirmText={popup.confirmText}
        cancelText={popup.cancelText}
      />
      <div className="fp-card">
        {!isComplete && (
          <Link to="/login" className="fp-back-button" aria-label="Quay lại trang đăng nhập">
            <IoArrowBackSharp size={20} />
          </Link>
        )}

        <div className="fp-header">
          <div className="fp-logo">
            <div className="fp-logo-icon">
              <FaBrain size={40} color="#dd797a" />
            </div>
            <span className="fp-logo-text">BrainGame</span>
          </div>
          <h1 className="fp-title">{isComplete ? 'Đã đặt lại mật khẩu' : 'Đặt lại mật khẩu'}</h1>
        </div>

        {isComplete ? (
          <div className="fp-success-container">
            <div className="fp-success-message">
              <span className="fp-success-icon"><CheckCircle size={18} /></span>
              <span>Mật khẩu mới đã sẵn sàng để đăng nhập.</span>
            </div>
            <div className="fp-success-actions">
              <button type="button" className="fp-submit-button" onClick={() => navigate('/login')}>
                Đăng nhập
              </button>
            </div>
          </div>
        ) : (
          <form className="fp-form" onSubmit={handleSubmit}>
            <div className="fp-form-group">
              <label className="fp-form-label" htmlFor="newPassword">
                <FaLock className="fp-input-icon" />
                <span>Mật khẩu mới</span>
              </label>
              <div className="fp-input-wrapper">
                <input
                  type={showPassword ? 'text' : 'password'}
                  id="newPassword"
                  name="newPassword"
                  className={`fp-form-input ${error ? 'fp-input-error' : ''}`}
                  value={formData.newPassword}
                  onChange={handleChange}
                  disabled={isSubmitting}
                  autoComplete="new-password"
                />
              </div>
            </div>

            <div className="fp-form-group">
              <label className="fp-form-label" htmlFor="confirmPassword">
                <FaLock className="fp-input-icon" />
                <span>Xác nhận mật khẩu</span>
              </label>
              <div className="fp-input-wrapper">
                <input
                  type={showPassword ? 'text' : 'password'}
                  id="confirmPassword"
                  name="confirmPassword"
                  className={`fp-form-input ${error ? 'fp-input-error' : ''}`}
                  value={formData.confirmPassword}
                  onChange={handleChange}
                  disabled={isSubmitting}
                  autoComplete="new-password"
                />
                <button
                  type="button"
                  className="fp-back-button"
                  style={{ position: 'absolute', right: 8, top: 8 }}
                  onClick={() => setShowPassword((value) => !value)}
                  aria-label={showPassword ? 'Ẩn mật khẩu' : 'Hiện mật khẩu'}
                >
                  {showPassword ? <FaEyeSlash size={16} /> : <FaEye size={16} />}
                </button>
                {error && (
                  <div className="fp-error-message">
                    <FaExclamationCircle size={14} />
                    <span>{error}</span>
                  </div>
                )}
              </div>
            </div>

            <button
              className={`fp-submit-button ${isSubmitting ? 'loading' : ''}`}
              type="submit"
              disabled={isSubmitting}
            >
              {isSubmitting ? 'Đang đặt lại...' : 'Đặt lại mật khẩu'}
            </button>
          </form>
        )}
      </div>
    </div>
  );
}

export default ResetPassword;
