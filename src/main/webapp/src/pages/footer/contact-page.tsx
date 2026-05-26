import { useState } from 'react';
import { Link } from 'react-router-dom';
import { motion, AnimatePresence } from 'framer-motion';
import {
    FaCheckCircle,
    FaChevronDown,
    FaChevronUp,
    FaClock,
    FaEnvelope,
    FaFacebook,
    FaInstagram,
    FaLinkedin,
    FaMapMarkerAlt,
    FaPhoneAlt,
    FaTwitter
} from 'react-icons/fa';
import '../../styles/pages/footer/contact-page.css';

const fadeUp = {
    hidden: { opacity: 0, y: 24 },
    show: { opacity: 1, y: 0, transition: { duration: 0.5, ease: 'easeOut' as const } },
};

const stagger = {
    hidden: {},
    show: { transition: { staggerChildren: 0.1, delayChildren: 0.1 } },
};

const cardItem = {
    hidden: { opacity: 0, y: 20 },
    show: { opacity: 1, y: 0, transition: { type: 'spring' as const, stiffness: 280, damping: 22 } },
};

const viewportOnce = { once: true, amount: 0.2 };

function ContactPage() {
    const [formStatus, setFormStatus] = useState({
        submitted: false, error: false, message: ''
    });

    const [expandedFaq, setExpandedFaq] = useState<number | null>(null);

    const toggleFaq = (index: number) => {
        if (expandedFaq === index) {
            setExpandedFaq(null);
        } else {
            setExpandedFaq(index);
        }
    };

    const handleSubmit = (e) => {
        e.preventDefault();
        // Giả lập gửi form thành công
        setFormStatus({
            submitted: true,
            error: false,
            message: 'Cảm ơn bạn! Tin nhắn của bạn đã được gửi thành công. Chúng tôi sẽ phản hồi trong thời gian sớm nhất.'
        });

        // Reset form sau 5 giây
        setTimeout(() => {
            setFormStatus({
                submitted: false, error: false, message: ''
            });
            e.target.reset();
        }, 5000);
    };

    // Dữ liệu FAQ
    const faqItems = [{
        question: 'Làm thế nào để tạo tài khoản?',
        answer: 'Bạn có thể tạo tài khoản bằng cách nhấp vào nút "Đăng ký" ở góc trên bên phải của trang web và làm theo hướng dẫn. Quá trình đăng ký chỉ mất khoảng 2 phút và bạn sẽ nhận được email xác nhận ngay sau khi hoàn tất.'
    }, {
        question: 'Tôi quên mật khẩu thì phải làm sao?',
        answer: 'Bạn có thể đặt lại mật khẩu bằng cách nhấp vào liên kết "Quên mật khẩu" trên trang đăng nhập. Nhập địa chỉ email đã đăng ký và chúng tôi sẽ gửi cho bạn hướng dẫn đặt lại mật khẩu qua email.'
    }, {
        question: 'Các trò chơi có miễn phí không?',
        answer: 'Chúng tôi cung cấp cả trò chơi miễn phí và trò chơi trả phí. Bạn có thể trải nghiệm nhiều trò chơi miễn phí trước khi quyết định nâng cấp lên gói trả phí để mở khóa tất cả các tính năng và trò chơi nâng cao.'
    }, {
        question: 'Làm thế nào để báo cáo lỗi?',
        answer: 'Bạn có thể báo cáo lỗi bằng cách sử dụng biểu mẫu liên hệ trên trang này hoặc gửi email trực tiếp đến support@braingame.vn. Vui lòng cung cấp càng nhiều thông tin càng tốt về lỗi bạn gặp phải để chúng tôi có thể hỗ trợ hiệu quả.'
    }, {
        question: 'Tôi có thể sử dụng BrainGame trên thiết bị di động không?',
        answer: 'Có, BrainGame được thiết kế để hoạt động trên mọi thiết bị, bao gồm điện thoại di động và máy tính bảng. Bạn có thể truy cập trang web của chúng tôi từ trình duyệt di động hoặc tải xuống ứng dụng di động của chúng tôi từ App Store hoặc Google Play.'
    }];

    return (<div className="contact-page">
        {/* Hero Section */}
        <div className="contact-hero">
            <motion.div
                className="contact-hero-content"
                initial={{ opacity: 0, y: 20 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ duration: 0.6, ease: 'easeOut' }}
            >
                <h1>Liên Hệ Với Chúng Tôi</h1>
                <p>Chúng tôi luôn sẵn sàng lắng nghe và hỗ trợ bạn</p>
            </motion.div>
        </div>

        <div className="contact-container">
            {/* Contact Info Cards */}
            <motion.div
                className="contact-info-grid"
                variants={stagger}
                initial="hidden"
                whileInView="show"
                viewport={viewportOnce}
            >
                <motion.div className="contact-info-card" variants={cardItem}>
                    <div className="contact-info-icon">
                        <FaEnvelope/>
                    </div>
                    <h3>Email</h3>
                    <p>contact@braingame.vn</p>
                    <p>support@braingame.vn</p>
                    <a href="mailto:contact@braingame.vn" className="contact-link">Gửi email</a>
                </motion.div>

                <motion.div className="contact-info-card" variants={cardItem}>
                    <div className="contact-info-icon">
                        <FaPhoneAlt/>
                    </div>
                    <h3>Điện Thoại</h3>
                    <p>0123 456 789</p>
                    <p>0987 654 321</p>
                    <a href="tel:0123456789" className="contact-link">Gọi ngay</a>
                </motion.div>

                <motion.div className="contact-info-card" variants={cardItem}>
                    <div className="contact-info-icon">
                        <FaMapMarkerAlt/>
                    </div>
                    <h3>Địa Chỉ</h3>
                    <p>123 Đường ABC, Quận XYZ</p>
                    <p>Hà Nội, Việt Nam</p>
                    <a href="https://maps.google.com" target="_blank" rel="noopener noreferrer"
                       className="contact-link">Xem bản đồ</a>
                </motion.div>

                <motion.div className="contact-info-card" variants={cardItem}>
                    <div className="contact-info-icon">
                        <FaClock/>
                    </div>
                    <h3>Giờ Làm Việc</h3>
                    <p>Thứ Hai - Thứ Sáu: 8:00 - 17:30</p>
                    <p>Thứ Bảy: 8:00 - 12:00</p>
                    <p>Chủ Nhật: Đóng cửa</p>
                </motion.div>
            </motion.div>

            {/* Contact Form and Map Section */}
            <div className="contact-main-section">
                <motion.div
                    className="contact-form-section"
                    variants={fadeUp}
                    initial="hidden"
                    whileInView="show"
                    viewport={viewportOnce}
                >
                    <div className="section-header">
                        <h2>Gửi Tin Nhắn</h2>
                        <p>Hãy cho chúng tôi biết bạn cần hỗ trợ gì, chúng tôi sẽ phản hồi trong thời gian sớm
                            nhất</p>
                    </div>

                    {formStatus.submitted ? (<div className="form-success-message">
                        <FaCheckCircle className="success-icon"/>
                        <p>{formStatus.message}</p>
                    </div>) : (<form className="contact-form" onSubmit={handleSubmit}>
                        <div className="form-row">
                            <div className="form-group">
                                <label htmlFor="name">Họ và tên</label>
                                <input type="text" id="name" name="name" placeholder="Nhập họ và tên của bạn"
                                       required/>
                            </div>

                            <div className="form-group">
                                <label htmlFor="email">Email</label>
                                <input type="email" id="email" name="email"
                                       placeholder="Nhập địa chỉ email của bạn" required/>
                            </div>
                        </div>

                        <div className="form-row">
                            <div className="form-group">
                                <label htmlFor="phone">Số điện thoại</label>
                                <input type="tel" id="phone" name="phone"
                                       placeholder="Nhập số điện thoại của bạn"/>
                            </div>

                            <div className="form-group">
                                <label htmlFor="subject">Chủ đề</label>
                                <input type="text" id="subject" name="subject" placeholder="Nhập chủ đề"
                                       required/>
                            </div>
                        </div>

                        <div className="form-group">
                            <label htmlFor="message">Nội dung</label>
                            <textarea id="message" name="message" rows={5}
                                      placeholder="Nhập nội dung tin nhắn của bạn" required></textarea>
                        </div>

                        <button type="submit" className="submit-btn">Gửi Tin Nhắn</button>
                    </form>)}
                </motion.div>

                <motion.div
                    className="contact-map-section"
                    variants={fadeUp}
                    initial="hidden"
                    whileInView="show"
                    viewport={viewportOnce}
                >
                    <div className="section-header">
                        <h2>Vị Trí Của Chúng Tôi</h2>
                        <p>Ghé thăm văn phòng của chúng tôi</p>
                    </div>
                    <div className="map-container">
                        {/* Đây là nơi để nhúng Google Maps hoặc bản đồ khác */}
                        <div className="map-placeholder">
                            <div className="map-overlay">
                                <FaMapMarkerAlt className="map-marker"/>
                                <h3>BrainGame Headquarters</h3>
                                <p>123 Đường ABC, Quận XYZ, Hà Nội, Việt Nam</p>
                            </div>
                        </div>
                    </div>
                </motion.div>
            </div>

            {/* FAQ Section */}
            <motion.div
                className="contact-faq-section"
                variants={fadeUp}
                initial="hidden"
                whileInView="show"
                viewport={viewportOnce}
            >
                <div className="section-header center">
                    <h2>Câu Hỏi Thường Gặp</h2>
                    <p>Tìm câu trả lời nhanh cho những câu hỏi phổ biến</p>
                </div>

                <div className="faq-container">
                    {faqItems.map((item, index) => (<div
                        className={`faq-item ${expandedFaq === index ? 'expanded' : ''}`}
                        key={index}
                        onClick={() => toggleFaq(index)}
                    >
                        <div className="faq-question">
                            <h3>{item.question}</h3>
                            {expandedFaq === index ? <FaChevronUp/> : <FaChevronDown/>}
                        </div>
                        <AnimatePresence initial={false}>
                            {expandedFaq === index && (
                                <motion.div
                                    key="answer"
                                    initial={{ height: 0, opacity: 0 }}
                                    animate={{ height: 'auto', opacity: 1 }}
                                    exit={{ height: 0, opacity: 0 }}
                                    transition={{ duration: 0.3, ease: 'easeOut' }}
                                    style={{ overflow: 'hidden' }}
                                >
                                    <div className="faq-answer">
                                        <p>{item.answer}</p>
                                    </div>
                                </motion.div>
                            )}
                        </AnimatePresence>
                    </div>))}
                </div>

                <div className="faq-more">
                    <p>Không tìm thấy câu trả lời bạn cần?</p>
                    <Link to="/faq" className="faq-link">Xem tất cả câu hỏi thường gặp</Link>
                </div>
            </motion.div>

            {/* Connect Section */}
            <motion.div
                className="contact-connect-section"
                variants={fadeUp}
                initial="hidden"
                whileInView="show"
                viewport={viewportOnce}
            >
                <div className="section-header center">
                    <h2>Kết Nối Với Chúng Tôi</h2>
                    <p>Theo dõi chúng tôi trên mạng xã hội để cập nhật tin tức và sự kiện mới nhất</p>
                </div>

                <motion.div
                    className="contact-social-links"
                    variants={stagger}
                    initial="hidden"
                    whileInView="show"
                    viewport={viewportOnce}
                >
                    <motion.a
                        href="https://facebook.com" target="_blank" rel="noopener noreferrer"
                        className="contact-social-link facebook"
                        variants={cardItem}
                    >
                        <FaFacebook/>
                        <span>Facebook</span>
                    </motion.a>
                    <motion.a
                        href="https://twitter.com" target="_blank" rel="noopener noreferrer"
                        className="contact-social-link twitter"
                        variants={cardItem}
                    >
                        <FaTwitter/>
                        <span>Twitter</span>
                    </motion.a>
                    <motion.a
                        href="https://instagram.com" target="_blank" rel="noopener noreferrer"
                        className="contact-social-link instagram"
                        variants={cardItem}
                    >
                        <FaInstagram/>
                        <span>Instagram</span>
                    </motion.a>
                    <motion.a
                        href="https://linkedin.com" target="_blank" rel="noopener noreferrer"
                        className="contact-social-link linkedin"
                        variants={cardItem}
                    >
                        <FaLinkedin/>
                        <span>LinkedIn</span>
                    </motion.a>
                </motion.div>
            </motion.div>

            {/* CTA Section */}
            <motion.div
                className="contact-cta"
                variants={fadeUp}
                initial="hidden"
                whileInView="show"
                viewport={viewportOnce}
            >
                <h2>Sẵn sàng bắt đầu hành trình phát triển trí não?</h2>
                <p>Đăng ký ngay hôm nay để trải nghiệm các trò chơi trí não thú vị và bổ ích</p>
                <div className="cta-buttons">
                    <Link to="/register" className="cta-button primary">Đăng ký ngay</Link>
                    <Link to="/games" className="cta-button secondary">Khám phá trò chơi</Link>
                </div>
            </motion.div>
        </div>
    </div>);
}

export default ContactPage;
