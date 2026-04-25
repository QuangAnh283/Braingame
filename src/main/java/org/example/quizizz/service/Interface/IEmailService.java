package org.example.quizizz.service.Interface;

/**
 * Interface cho Email Service
 * Xử lý việc gửi email cho các tính năng khác nhau
 */
public interface IEmailService {

    /**
     * Gửi email reset password với link/token một lần
     *
     * @param toEmail email người nhận
     * @param username tên người dùng
     * @param resetToken token reset mật khẩu
     * @return true nếu gửi thành công, false nếu thất bại
     */
    boolean sendPasswordResetEmail(String toEmail, String username, String resetToken);

    /**
     * Gửi email xác thực tài khoản khi đăng ký
     *
     * @param toEmail email người nhận
     * @param username tên người dùng
     * @param verificationToken token xác thực
     * @return true nếu gửi thành công, false nếu thất bại
     */
    boolean sendVerificationEmail(String toEmail, String username, String verificationToken);
}
