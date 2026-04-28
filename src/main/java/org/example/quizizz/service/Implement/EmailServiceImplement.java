package org.example.quizizz.service.Implement;

import org.example.quizizz.service.Interface.IEmailService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

/**
 * Implementation của Email Service
 * Sử dụng JavaMailSender và Thymeleaf template để gửi email
 */
@Service
public class EmailServiceImplement implements IEmailService {

    private final JavaMailSender javaMailSender;
    private final TemplateEngine emailTemplateEngine;
    private final String fromEmail;
    private final String companyName;
    private final String supportUrl;
    private final String frontendBaseUrl;

    public EmailServiceImplement(JavaMailSender javaMailSender,
                                 TemplateEngine templateEngine,
                                 @Qualifier("fromEmail") String fromEmail,
                                 @Qualifier("companyName") String companyName,
                                 @Qualifier("supportUrl") String supportUrl,
                                 @Value("${app.frontend.base-url:http://localhost:5173}") String frontendBaseUrl) {
        this.javaMailSender = javaMailSender;
        this.emailTemplateEngine = templateEngine;
        this.fromEmail = fromEmail;
        this.companyName = companyName;
        this.supportUrl = supportUrl;
        this.frontendBaseUrl = frontendBaseUrl;
    }

    /**
     * Gửi email reset mật khẩu cho người dùng.
     * @param toEmail Email người nhận
     * @param username Tên người dùng
     * @param resetToken Token reset mật khẩu
     * @return true nếu gửi thành công, false nếu lỗi
     */
    @Override
    public boolean sendPasswordResetEmail(String toEmail, String username, String resetToken) {
        try {

            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            // Thiết lập thông tin email
            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("Reset Mật Khẩu - " + companyName);

            // Tạo context cho Thymeleaf template
            String resetUrl = frontendBaseUrl + "/reset-password?token="
                    + URLEncoder.encode(resetToken, StandardCharsets.UTF_8);
            Context context = new Context();
            context.setVariable("username", username);
            context.setVariable("resetUrl", resetUrl);
            context.setVariable("companyName", companyName);
            context.setVariable("supportUrl", supportUrl);
            context.setVariable("year", LocalDateTime.now().getYear());

            // Render HTML template
            String htmlContent = emailTemplateEngine.process("reset-password", context);
            helper.setText(htmlContent, true);

            // Gửi email
            javaMailSender.send(message);


            return true;

        } catch (MessagingException e) {
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Gửi email xác thực tài khoản khi đăng ký.
     * @param toEmail Email người nhận
     * @param username Tên người dùng
     * @param verificationToken Token xác thực
     * @return true nếu gửi thành công, false nếu lỗi
     */
    @Override
    public boolean sendVerificationEmail(String toEmail, String username, String verificationToken) {
        try {
            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("Xác thực tài khoản - " + companyName);

            // Tạo URL xác thực (frontend URL)
            String verificationUrl = frontendBaseUrl + "/verify-email?token="
                    + URLEncoder.encode(verificationToken, StandardCharsets.UTF_8);

            // Tạo context cho Thymeleaf template
            Context context = new Context();
            context.setVariable("username", username);
            context.setVariable("verificationUrl", verificationUrl);
            context.setVariable("companyName", companyName);
            context.setVariable("supportUrl", supportUrl);
            context.setVariable("year", LocalDateTime.now().getYear());

            // Render HTML template
            String htmlContent = emailTemplateEngine.process("email-verification", context);
            helper.setText(htmlContent, true);

            // Gửi email
            javaMailSender.send(message);

            return true;

        } catch (MessagingException e) {
            return false;
        } catch (Exception e) {
            return false;
        }
    }
}
