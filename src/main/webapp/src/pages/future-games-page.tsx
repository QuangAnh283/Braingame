import { Link } from "react-router-dom";
import { motion } from "framer-motion";
import {
  FaBook,
  FaBrain,
  FaCalculator,
  FaClock,
  FaPuzzlePiece,
  FaRocket,
} from "react-icons/fa";
import "../styles/pages/future-games-page.css";

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
  show: { transition: { staggerChildren: 0.12, delayChildren: 0.1 } },
};

const cardItem = {
  hidden: { opacity: 0, y: 24, scale: 0.96 },
  show: {
    opacity: 1,
    y: 0,
    scale: 1,
    transition: { type: "spring" as const, stiffness: 260, damping: 22 },
  },
};

const viewportOnce = { once: true, amount: 0.2 };

const gameCategories = [
  {
    icon: FaCalculator,
    title: "Toán học",
    description:
      "Rèn luyện tư duy logic qua các bài toán đa dạng từ cơ bản đến nâng cao.",
    accent: "math",
  },
  {
    icon: FaBook,
    title: "Ngôn ngữ",
    description:
      "Mở rộng vốn từ và rèn kỹ năng ngôn ngữ qua các trò chơi sáng tạo.",
    accent: "language",
  },
  {
    icon: FaPuzzlePiece,
    title: "Logic",
    description:
      "Giải đố thông minh giúp phát triển khả năng suy luận và phân tích.",
    accent: "logic",
  },
  {
    icon: FaBrain,
    title: "Trí nhớ",
    description:
      "Tăng cường khả năng ghi nhớ và tốc độ xử lý thông tin của não bộ.",
    accent: "memory",
  },
];

function FutureGamesPage() {
  return (
    <div className="future-games-page">
      {/* Hero — Coming Soon */}
      <motion.section
        className="games-hero"
        initial="hidden"
        animate="show"
        variants={stagger}
      >
        <motion.span className="games-badge" variants={fadeUp}>
          <FaClock />
          <span>Coming Soon</span>
        </motion.span>
        <motion.h1 className="games-hero-title" variants={fadeUp}>
          Thế giới trò chơi trí tuệ đang được khởi tạo
        </motion.h1>
        <motion.p className="games-hero-subtitle" variants={fadeUp}>
          Chúng tôi đang chuẩn bị những trải nghiệm trò chơi độc đáo, được thiết
          kế khoa học để giúp bạn rèn luyện não bộ mỗi ngày. Hãy đón chờ!
        </motion.p>
      </motion.section>

      {/* Categories Preview */}
      <motion.section
        className="games-categories"
        variants={stagger}
        initial="hidden"
        whileInView="show"
        viewport={viewportOnce}
      >
        <motion.h2 className="games-section-title" variants={fadeUp}>
          Các thể loại đang được phát triển
        </motion.h2>
        <motion.p className="games-section-intro" variants={fadeUp}>
          Bốn dòng game được thiết kế để rèn luyện 4 nhóm năng lực cốt lõi của
          não bộ.
        </motion.p>
        <motion.div className="games-categories-grid" variants={stagger}>
          {gameCategories.map((cat) => {
            const Icon = cat.icon;
            return (
              <motion.div
                key={cat.title}
                className={`category-card category-${cat.accent}`}
                variants={cardItem}
                whileHover={{ scale: 1.05 }}
                whileTap={{ scale: 0.98 }}
              >
                <div className="category-icon">
                  <Icon />
                </div>
                <h3>{cat.title}</h3>
                <p>{cat.description}</p>
                <span className="category-tag">Sắp ra mắt</span>
              </motion.div>
            );
          })}
        </motion.div>
      </motion.section>

      {/* CTA */}
      <motion.section
        className="games-cta"
        variants={fadeUp}
        initial="hidden"
        whileInView="show"
        viewport={viewportOnce}
      >
        <FaRocket className="games-cta-icon" />
        <h2>Đừng bỏ lỡ ngày ra mắt!</h2>
        <p>
          Trong thời gian chờ đợi, hãy khám phá những trang khác của BrainGame
          để hiểu về sứ mệnh và đội ngũ đứng sau dự án.
        </p>
        <div className="games-cta-buttons">
          <Link to="/" className="games-cta-button primary">
            Trở về trang chủ
          </Link>
          <Link to="/about" className="games-cta-button secondary">
            Tìm hiểu về chúng tôi
          </Link>
        </div>
      </motion.section>
    </div>
  );
}

export default FutureGamesPage;
