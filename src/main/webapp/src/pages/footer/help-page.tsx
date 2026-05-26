import { Link } from "react-router-dom";
import { motion } from "framer-motion";
import {
  FaBook,
  FaHeadset,
  FaQuestionCircle,
  FaSearch,
  FaTools,
  FaUserShield,
} from "react-icons/fa";
import "../../styles/pages/footer/help-page.css";

const fadeUp = {
  hidden: { opacity: 0, y: 24 },
  show: {
    opacity: 1,
    y: 0,
    transition: { duration: 0.5, ease: "easeOut" as const },
  },
};

const stagger = {
  hidden: {},
  show: { transition: { staggerChildren: 0.1, delayChildren: 0.1 } },
};

const cardItem = {
  hidden: { opacity: 0, y: 20, scale: 0.96 },
  show: {
    opacity: 1,
    y: 0,
    scale: 1,
    transition: { type: "spring" as const, stiffness: 280, damping: 22 },
  },
};

const searchScale = {
  hidden: { opacity: 0, scale: 0.94, y: 12 },
  show: {
    opacity: 1,
    scale: 1,
    y: 0,
    transition: { type: "spring" as const, stiffness: 240, damping: 24 },
  },
};

const viewportOnce = { once: true, amount: 0.2 };

const popularQuestions = [
  {
    id: 1,
    question: "Làm thế nào để tạo tài khoản mới?",
    answer:
      'Để tạo tài khoản mới, hãy nhấp vào nút "Đăng ký" ở góc trên bên phải của trang web. Sau đó, điền thông tin cá nhân của bạn và làm theo hướng dẫn để hoàn tất quá trình đăng ký.',
    category: "Tài khoản",
  },
  {
    id: 2,
    question: "Tôi quên mật khẩu, làm cách nào để đặt lại?",
    answer:
      'Nếu bạn quên mật khẩu, hãy nhấp vào liên kết "Quên mật khẩu" trên trang đăng nhập. Nhập địa chỉ email đã đăng ký của bạn và chúng tôi sẽ gửi cho bạn hướng dẫn đặt lại mật khẩu.',
    category: "Tài khoản",
  },
  {
    id: 3,
    question: "Làm thế nào để bắt đầu chơi trò chơi?",
    answer:
      'Để bắt đầu chơi trò chơi, hãy đăng nhập vào tài khoản của bạn, sau đó truy cập vào trang "Trò chơi". Chọn trò chơi bạn muốn chơi và nhấp vào nút "Chơi ngay".',
    category: "Trò chơi",
  },
  {
    id: 4,
    question: "Làm thế nào để theo dõi tiến trình của tôi?",
    answer:
      'Bạn có thể theo dõi tiến trình của mình bằng cách truy cập vào trang "Hồ sơ" sau khi đăng nhập. Tại đây, bạn sẽ thấy thống kê về các trò chơi đã chơi, điểm số và tiến trình học tập của mình.',
    category: "Tiến trình",
  },
  {
    id: 5,
    question: "Các trò chơi có hoạt động trên điện thoại di động không?",
    answer:
      "Có, tất cả các trò chơi của chúng tôi đều được thiết kế để hoạt động trên cả máy tính và thiết bị di động. Bạn có thể truy cập trang web của chúng tôi từ trình duyệt di động hoặc tải xuống ứng dụng di động của chúng tôi.",
    category: "Kỹ thuật",
  },
  {
    id: 6,
    question: "Làm thế nào để báo cáo lỗi hoặc vấn đề?",
    answer:
      'Để báo cáo lỗi hoặc vấn đề, hãy truy cập trang "Liên hệ" và điền vào biểu mẫu liên hệ với chi tiết về vấn đề bạn đang gặp phải. Bạn cũng có thể gửi email trực tiếp đến support@braingame.vn.',
    category: "Hỗ trợ",
  },
];

const helpCategories = [
  {
    id: 1,
    title: "Hướng dẫn sử dụng",
    icon: FaBook,
    description:
      "Hướng dẫn chi tiết về cách sử dụng các tính năng của BrainGame",
    link: "/help/guides",
  },
  {
    id: 2,
    title: "Câu hỏi thường gặp",
    icon: FaQuestionCircle,
    description: "Danh sách các câu hỏi và câu trả lời phổ biến",
    link: "/faq",
  },
  {
    id: 3,
    title: "Hỗ trợ trực tiếp",
    icon: FaHeadset,
    description: "Liên hệ với đội ngũ hỗ trợ của chúng tôi",
    link: "/contact",
  },
  {
    id: 4,
    title: "Xử lý sự cố",
    icon: FaTools,
    description: "Hướng dẫn khắc phục các vấn đề kỹ thuật thường gặp",
    link: "/help/troubleshooting",
  },
  {
    id: 5,
    title: "Bảo mật & Quyền riêng tư",
    icon: FaUserShield,
    description: "Thông tin về cách chúng tôi bảo vệ dữ liệu của bạn",
    link: "/privacy",
  },
];

function HelpPage() {
  return (
    <div className="help-container">
      {/* Hero — Gradient title + Search */}
      <motion.div
        className="help-header"
        initial="hidden"
        animate="show"
        variants={stagger}
      >
        <motion.h1 variants={fadeUp}>Trung Tâm Trợ Giúp</motion.h1>
        <motion.p variants={fadeUp}>Chúng tôi luôn sẵn sàng hỗ trợ bạn</motion.p>

        <motion.div className="help-search" variants={searchScale}>
          <div className="search-box">
            <div className="search-input-wrapper">
              <FaSearch className="search-icon" />
              <input
                type="text"
                className="search-input"
                placeholder="Tìm kiếm câu hỏi hoặc chủ đề..."
              />
            </div>
            <button className="search-btn">Tìm kiếm</button>
          </div>
        </motion.div>
      </motion.div>

      {/* Help Categories */}
      <motion.div
        className="help-categories"
        variants={stagger}
        initial="hidden"
        whileInView="show"
        viewport={viewportOnce}
      >
        {helpCategories.map((category) => {
          const Icon = category.icon;
          return (
            <motion.div key={category.id} variants={cardItem}>
              <Link to={category.link} className="category-card">
                <div className="category-icon">
                  <Icon />
                </div>
                <h3>{category.title}</h3>
                <p>{category.description}</p>
              </Link>
            </motion.div>
          );
        })}
      </motion.div>

      {/* Popular Questions */}
      <motion.div
        className="popular-questions"
        variants={stagger}
        initial="hidden"
        whileInView="show"
        viewport={viewportOnce}
      >
        <motion.h2 variants={fadeUp}>Câu Hỏi Phổ Biến</motion.h2>
        <motion.div className="questions-list" variants={stagger}>
          {popularQuestions.map((item) => (
            <motion.div
              className="question-item"
              key={item.id}
              variants={cardItem}
            >
              <div className="question-header">
                <h3>{item.question}</h3>
                <span className="question-category">{item.category}</span>
              </div>
              <div className="question-answer">
                <p>{item.answer}</p>
              </div>
            </motion.div>
          ))}
        </motion.div>
      </motion.div>

      {/* Contact CTA */}
      <motion.div
        className="help-contact"
        variants={fadeUp}
        initial="hidden"
        whileInView="show"
        viewport={viewportOnce}
      >
        <h2>Vẫn Cần Trợ Giúp?</h2>
        <p>
          Nếu bạn không tìm thấy câu trả lời cho câu hỏi của mình, hãy liên hệ
          với đội ngũ hỗ trợ của chúng tôi.
        </p>
        <Link to="/contact" className="contact-btn">
          Liên Hệ Hỗ Trợ
        </Link>
      </motion.div>
    </div>
  );
}

export default HelpPage;
