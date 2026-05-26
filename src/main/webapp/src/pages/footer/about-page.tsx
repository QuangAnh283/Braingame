import { motion } from "framer-motion";
import {
  FaBrain,
  FaChartLine,
  FaHistory,
  FaLightbulb,
  FaMedal,
  FaUsers,
} from "react-icons/fa";
import "../../styles/pages/footer/about-page.css";

import witchImg from "../../assets/images/Witches.png";
import grokImg from "../../assets/images/grok.png";
import claudeImg from "../../assets/images/claude.png";
import qwenImg from "../../assets/images/qwen.png";

const fadeUp = {
  hidden: { opacity: 0, y: 24 },
  show: { opacity: 1, y: 0, transition: { duration: 0.5, ease: "easeOut" as const } },
};

const stagger = {
  hidden: {},
  show: { transition: { staggerChildren: 0.12, delayChildren: 0.1 } },
};

const cardItem = {
  hidden: { opacity: 0, y: 20 },
  show: { opacity: 1, y: 0, transition: { type: "spring" as const, stiffness: 280, damping: 22 } },
};

const slideFromLeft = {
  hidden: { opacity: 0, x: -60 },
  show: { opacity: 1, x: 0, transition: { type: "spring" as const, stiffness: 220, damping: 26 } },
};

const slideFromRight = {
  hidden: { opacity: 0, x: 60 },
  show: { opacity: 1, x: 0, transition: { type: "spring" as const, stiffness: 220, damping: 26 } },
};

const viewportOnce = { once: true, amount: 0.2 };

const missionStats = [
  { number: "50+", label: "Trò chơi trí tuệ" },
  { number: "100K+", label: "Người dùng" },
  { number: "5M+", label: "Lượt chơi" },
];

const timelineEvents = [
  {
    year: "2020",
    icon: FaHistory,
    description:
      "BrainGame được thành lập với ý tưởng ban đầu về một nền tảng học tập thông qua trò chơi.",
  },
  {
    year: "2021",
    icon: FaLightbulb,
    description:
      "Ra mắt phiên bản đầu tiên với 10 trò chơi trí tuệ cơ bản và thu hút 10,000 người dùng đầu tiên.",
  },
  {
    year: "2022",
    icon: FaUsers,
    description:
      "Mở rộng đội ngũ và phát triển thêm 20 trò chơi mới, đạt mốc 50,000 người dùng.",
  },
  {
    year: "2023",
    icon: FaChartLine,
    description:
      "Phát triển tính năng cá nhân hóa và phân tích dữ liệu, giúp người dùng theo dõi tiến trình học tập hiệu quả hơn.",
  },
  {
    year: "2024",
    icon: FaMedal,
    description:
      "Trở thành nền tảng trò chơi trí não hàng đầu Việt Nam với hơn 100,000 người dùng thường xuyên.",
  },
];

const coreValues = [
  {
    icon: FaBrain,
    title: "Khoa học",
    description:
      "Mọi trò chơi đều được phát triển dựa trên nghiên cứu khoa học về não bộ và quá trình học tập.",
  },
  {
    icon: FaLightbulb,
    title: "Sáng tạo",
    description:
      "Luôn đổi mới và tìm kiếm những cách tiếp cận mới để phát triển trí tuệ.",
  },
  {
    icon: FaUsers,
    title: "Cộng đồng",
    description: "Xây dựng cộng đồng học tập và phát triển cùng nhau.",
  },
  {
    icon: FaChartLine,
    title: "Tiến bộ",
    description:
      "Cam kết giúp người dùng đạt được tiến bộ thực sự trong việc phát triển trí tuệ.",
  },
];

const teamMembers = [
  { photo: witchImg, name: "Phù Thủy cấp 1", title: "Nhà sáng lập & CEO" },
  {
    photo: grokImg,
    name: "Grok",
    title: "Bộ môn phòng chống nghệ thuật hắc ám",
  },
  { photo: claudeImg, name: "Claude", title: "Bác Sĩ chính" },
  { photo: qwenImg, name: "Qwen", title: "Chuyên viên hậu cần" },
];

function AboutPage() {
  return (
    <div className="about-page">
      {/* Hero Section */}
      <div className="about-hero">
        <motion.div
          className="about-hero-content"
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.6, ease: "easeOut" }}
        >
          <h1>Về Chúng Tôi</h1>
          <p>Nền tảng trò chơi trí não hàng đầu Việt Nam</p>
        </motion.div>
      </div>

      {/* Main Content */}
      <div className="about-container">
        {/* Mission Section */}
        <motion.section
          className="about-mission"
          variants={stagger}
          initial="hidden"
          whileInView="show"
          viewport={viewportOnce}
        >
          <motion.h2 variants={fadeUp}>Sứ mệnh của chúng tôi</motion.h2>
          <motion.p className="mission-description" variants={fadeUp}>
            BrainGame được thành lập với sứ mệnh giúp mọi người phát triển trí
            tuệ thông qua các trò chơi trí não thú vị và bổ ích. Chúng tôi tin
            rằng việc rèn luyện não bộ có thể trở nên thú vị và hấp dẫn thông
            qua các trò chơi được thiết kế khoa học.
          </motion.p>
          <motion.div className="mission-stats" variants={stagger}>
            {missionStats.map((stat) => (
              <motion.div
                key={stat.label}
                className="stat-item"
                variants={cardItem}
              >
                <span className="stat-number">{stat.number}</span>
                <span className="stat-label">{stat.label}</span>
              </motion.div>
            ))}
          </motion.div>
        </motion.section>

        {/* Story Section — Centered Vertical Timeline */}
        <motion.section
          className="about-story"
          variants={stagger}
          initial="hidden"
          whileInView="show"
          viewport={viewportOnce}
        >
          <motion.h2 variants={fadeUp}>Câu chuyện của chúng tôi</motion.h2>
          <motion.div className="timeline" variants={stagger}>
            {timelineEvents.map((event, index) => {
              const Icon = event.icon;
              const isLeft = index % 2 === 0;
              return (
                <motion.div
                  key={event.year}
                  className={`timeline-item ${isLeft ? "left" : "right"}`}
                  variants={isLeft ? slideFromLeft : slideFromRight}
                >
                  <div className="timeline-marker">
                    <Icon />
                  </div>
                  <div className="timeline-content">
                    <h3>{event.year}</h3>
                    <p>{event.description}</p>
                  </div>
                </motion.div>
              );
            })}
          </motion.div>
        </motion.section>

        {/* Values Section */}
        <motion.section
          className="about-values"
          variants={stagger}
          initial="hidden"
          whileInView="show"
          viewport={viewportOnce}
        >
          <motion.h2 variants={fadeUp}>Giá trị cốt lõi</motion.h2>
          <motion.div className="values-grid" variants={stagger}>
            {coreValues.map((value) => {
              const Icon = value.icon;
              return (
                <motion.div
                  key={value.title}
                  className="value-card"
                  variants={cardItem}
                >
                  <div className="value-icon">
                    <Icon />
                  </div>
                  <h3>{value.title}</h3>
                  <p>{value.description}</p>
                </motion.div>
              );
            })}
          </motion.div>
        </motion.section>

        {/* Team Section */}
        <motion.section
          className="about-team"
          variants={stagger}
          initial="hidden"
          whileInView="show"
          viewport={viewportOnce}
        >
          <motion.h2 variants={fadeUp}>Đội ngũ của chúng tôi</motion.h2>
          <motion.p className="team-intro" variants={fadeUp}>
            Đội ngũ BrainGame bao gồm các chuyên gia trong lĩnh vực khoa học
            thần kinh, thiết kế trò chơi, và phát triển phần mềm. Chúng tôi làm
            việc với niềm đam mê và sự tận tâm để mang đến những trải nghiệm tốt
            nhất cho người dùng.
          </motion.p>
          <motion.div className="team-grid" variants={stagger}>
            {teamMembers.map((member) => (
              <motion.div
                key={member.name}
                className="team-member"
                variants={cardItem}
              >
                <div
                  className="member-photo"
                  style={{ backgroundImage: `url(${member.photo})` }}
                ></div>
                <h3>{member.name}</h3>
                <p className="member-title">{member.title}</p>
              </motion.div>
            ))}
          </motion.div>
        </motion.section>

        {/* CTA Section */}
        <motion.section
          className="about-cta"
          variants={fadeUp}
          initial="hidden"
          whileInView="show"
          viewport={viewportOnce}
        >
          <h2>Sẵn sàng rèn luyện trí não của bạn?</h2>
          <p>
            Tham gia cùng hơn 100,000 người dùng đang phát triển trí tuệ mỗi
            ngày
          </p>
          <div className="cta-buttons">
            <a href="/register" className="cta-button primary">
              Đăng ký ngay
            </a>
            <a href="/games" className="cta-button secondary">
              Khám phá trò chơi
            </a>
          </div>
        </motion.section>
      </div>
    </div>
  );
}

export default AboutPage;
