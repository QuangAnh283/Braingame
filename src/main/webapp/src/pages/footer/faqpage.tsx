import { useState } from "react";
import { Link } from "react-router-dom";
import { motion, AnimatePresence } from "framer-motion";
import {
  FaChevronDown,
  FaChevronUp,
  FaCreditCard,
  FaGamepad,
  FaHeadset,
  FaSearch,
  FaShieldAlt,
  FaTools,
  FaUser,
} from "react-icons/fa";
import "../../styles/pages/footer/faqpage.css";

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
  show: { transition: { staggerChildren: 0.08, delayChildren: 0.08 } },
};

const cardItem = {
  hidden: { opacity: 0, y: 18, scale: 0.97 },
  show: {
    opacity: 1,
    y: 0,
    scale: 1,
    transition: { type: "spring" as const, stiffness: 280, damping: 24 },
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

const viewportOnce = { once: true, amount: 0.15 };

function FAQPage() {
  // State cho tìm kiếm và danh mục đang chọn
  const [searchTerm, setSearchTerm] = useState("");
  const [activeCategory, setActiveCategory] = useState(null);
  const [openQuestions, setOpenQuestions] = useState({});

  // Dữ liệu mẫu cho các câu hỏi thường gặp
  const faqCategories = [
    {
      id: 1,
      name: "Tài khoản & Đăng nhập",
      icon: <FaUser />,
      faqs: [
        {
          id: 101,
          question: "Làm thế nào để tạo tài khoản mới?",
          answer:
            'Để tạo tài khoản mới, hãy nhấp vào nút "Đăng ký" ở góc trên bên phải của trang web. Sau đó, điền thông tin cá nhân của bạn và làm theo hướng dẫn để hoàn tất quá trình đăng ký. Quá trình này chỉ mất khoảng 2 phút và bạn sẽ nhận được email xác nhận ngay sau khi hoàn tất.',
        },
        {
          id: 102,
          question: "Tôi quên mật khẩu, làm cách nào để đặt lại?",
          answer:
            'Nếu bạn quên mật khẩu, hãy nhấp vào liên kết "Quên mật khẩu" trên trang đăng nhập. Nhập địa chỉ email đã đăng ký của bạn và chúng tôi sẽ gửi cho bạn hướng dẫn đặt lại mật khẩu. Đường link đặt lại mật khẩu có hiệu lực trong vòng 24 giờ.',
        },
        {
          id: 103,
          question: "Làm thế nào để thay đổi thông tin cá nhân?",
          answer:
            'Để thay đổi thông tin cá nhân, hãy đăng nhập vào tài khoản của bạn, sau đó truy cập vào trang "Hồ sơ". Tại đây, bạn có thể chỉnh sửa thông tin cá nhân của mình như tên, ảnh đại diện, ngày sinh và các thông tin khác. Nhớ nhấn nút "Lưu thay đổi" sau khi hoàn tất.',
        },
        {
          id: 104,
          question: "Làm thế nào để xóa tài khoản của tôi?",
          answer:
            'Để xóa tài khoản, vui lòng đăng nhập và truy cập vào trang "Cài đặt tài khoản". Tại đây, bạn sẽ tìm thấy tùy chọn "Xóa tài khoản" ở cuối trang. Lưu ý rằng hành động này không thể hoàn tác và tất cả dữ liệu của bạn sẽ bị xóa vĩnh viễn, bao gồm tiến trình học tập, điểm số và lịch sử thanh toán.',
        },
      ],
    },
    {
      id: 2,
      name: "Trò chơi & Tính năng",
      icon: <FaGamepad />,
      faqs: [
        {
          id: 201,
          question: "Làm thế nào để bắt đầu chơi trò chơi?",
          answer:
            'Để bắt đầu chơi trò chơi, hãy đăng nhập vào tài khoản của bạn, sau đó truy cập vào trang "Trò chơi". Tại đây, bạn sẽ thấy danh sách các trò chơi có sẵn. Chọn trò chơi bạn muốn chơi và nhấp vào nút "Chơi ngay". Mỗi trò chơi đều có hướng dẫn chi tiết trước khi bắt đầu.',
        },
        {
          id: 202,
          question: "Các trò chơi có miễn phí không?",
          answer:
            "Chúng tôi cung cấp cả trò chơi miễn phí và trò chơi trả phí. Bạn có thể trải nghiệm nhiều trò chơi miễn phí trước khi quyết định nâng cấp lên gói trả phí để mở khóa tất cả các tính năng và trò chơi nâng cao. Các gói trả phí của chúng tôi bắt đầu từ 99.000đ/tháng với nhiều ưu đãi khi đăng ký gói dài hạn.",
        },
        {
          id: 203,
          question: "Làm thế nào để theo dõi tiến trình của tôi?",
          answer:
            'Bạn có thể theo dõi tiến trình của mình bằng cách truy cập vào trang "Hồ sơ" sau khi đăng nhập. Tại đây, bạn sẽ thấy thống kê chi tiết về các trò chơi đã chơi, điểm số, thời gian chơi và tiến trình học tập của mình. Chúng tôi cũng cung cấp biểu đồ trực quan để bạn có thể theo dõi sự tiến bộ theo thời gian.',
        },
        {
          id: 204,
          question: "Có thể chơi trò chơi mà không cần đăng nhập không?",
          answer:
            "Có, bạn có thể chơi một số trò chơi miễn phí mà không cần đăng nhập. Tuy nhiên, để lưu tiến trình, điểm số và mở khóa tất cả các tính năng, chúng tôi khuyên bạn nên tạo tài khoản và đăng nhập. Việc đăng ký tài khoản hoàn toàn miễn phí và chỉ mất vài phút.",
        },
        {
          id: 205,
          question: "Làm thế nào để tham gia thách đấu với người chơi khác?",
          answer:
            'Để tham gia thách đấu với người chơi khác, hãy truy cập vào trang "Thách đấu" sau khi đăng nhập. Tại đây, bạn có thể tạo phòng mới, tham gia phòng có sẵn hoặc mời bạn bè tham gia. Bạn cũng có thể tham gia các giải đấu hàng tuần để cạnh tranh với người chơi từ khắp nơi trên thế giới.',
        },
      ],
    },
    {
      id: 3,
      name: "Thanh toán & Gói dịch vụ",
      icon: <FaCreditCard />,
      faqs: [
        {
          id: 301,
          question: "Các phương thức thanh toán nào được chấp nhận?",
          answer:
            "Chúng tôi chấp nhận nhiều phương thức thanh toán khác nhau, bao gồm thẻ tín dụng/ghi nợ (Visa, MasterCard, JCB), ví điện tử (MoMo, ZaloPay, VNPay), và chuyển khoản ngân hàng. Tất cả các giao dịch đều được bảo mật bằng công nghệ mã hóa SSL 256-bit.",
        },
        {
          id: 302,
          question: "Làm thế nào để nâng cấp lên gói trả phí?",
          answer:
            'Để nâng cấp lên gói trả phí, hãy đăng nhập vào tài khoản của bạn và truy cập vào trang "Gói dịch vụ". Tại đây, bạn có thể xem các gói dịch vụ có sẵn, so sánh tính năng và chọn gói phù hợp với nhu cầu của mình. Sau khi chọn gói, làm theo hướng dẫn để hoàn tất quá trình thanh toán.',
        },
        {
          id: 303,
          question: "Tôi có thể hủy đăng ký gói trả phí không?",
          answer:
            'Có, bạn có thể hủy đăng ký gói trả phí bất kỳ lúc nào. Để hủy đăng ký, hãy đăng nhập vào tài khoản của bạn, truy cập vào trang "Cài đặt tài khoản" và chọn "Quản lý đăng ký". Nhấp vào "Hủy đăng ký" và làm theo hướng dẫn. Bạn sẽ vẫn có quyền truy cập vào các tính năng trả phí cho đến khi kết thúc chu kỳ thanh toán hiện tại.',
        },
        {
          id: 304,
          question: "Có chính sách hoàn tiền không?",
          answer:
            'Có, chúng tôi có chính sách hoàn tiền trong vòng 7 ngày kể từ ngày thanh toán nếu bạn không hài lòng với dịch vụ. Để yêu cầu hoàn tiền, vui lòng liên hệ với đội ngũ hỗ trợ của chúng tôi qua email support@braingame.vn với tiêu đề "Yêu cầu hoàn tiền" và cung cấp thông tin về tài khoản và lý do hoàn tiền của bạn.',
        },
        {
          id: 305,
          question: "Các gói dịch vụ có những tính năng gì?",
          answer:
            "Chúng tôi cung cấp nhiều gói dịch vụ khác nhau để phù hợp với nhu cầu của bạn. Gói Cơ bản (miễn phí) cho phép bạn truy cập vào các trò chơi cơ bản. Gói Premium (99.000đ/tháng) mở khóa tất cả các trò chơi, không có quảng cáo và báo cáo tiến trình chi tiết. Gói Pro (199.000đ/tháng) bao gồm tất cả tính năng Premium cộng với huấn luyện cá nhân và các bài tập tùy chỉnh.",
        },
      ],
    },
    {
      id: 4,
      name: "Kỹ thuật & Khắc phục sự cố",
      icon: <FaTools />,
      faqs: [
        {
          id: 401,
          question: "Các trò chơi có hoạt động trên điện thoại di động không?",
          answer:
            "Có, tất cả các trò chơi của chúng tôi đều được thiết kế để hoạt động trên cả máy tính và thiết bị di động. Bạn có thể truy cập trang web của chúng tôi từ trình duyệt di động hoặc tải xuống ứng dụng di động của chúng tôi từ App Store hoặc Google Play để có trải nghiệm tốt nhất trên thiết bị di động.",
        },
        {
          id: 402,
          question: "Làm thế nào để báo cáo lỗi hoặc vấn đề?",
          answer:
            'Để báo cáo lỗi hoặc vấn đề, hãy truy cập trang "Liên hệ" và điền vào biểu mẫu liên hệ với chi tiết về vấn đề bạn đang gặp phải. Vui lòng cung cấp càng nhiều thông tin càng tốt, bao gồm loại thiết bị, trình duyệt, và các bước để tái hiện lỗi. Bạn cũng có thể gửi email trực tiếp đến support@braingame.vn.',
        },
        {
          id: 403,
          question: "Tôi gặp vấn đề khi tải trò chơi, phải làm sao?",
          answer:
            "Nếu bạn gặp vấn đề khi tải trò chơi, hãy thử các giải pháp sau: 1) Làm mới trang web, 2) Xóa bộ nhớ cache của trình duyệt, 3) Kiểm tra kết nối internet của bạn, 4) Thử sử dụng trình duyệt khác, 5) Tắt các tiện ích mở rộng hoặc trình chặn quảng cáo. Nếu vấn đề vẫn tiếp diễn, hãy liên hệ với đội ngũ hỗ trợ của chúng tôi.",
        },
        {
          id: 404,
          question: "Yêu cầu hệ thống tối thiểu là gì?",
          answer:
            "Để có trải nghiệm tốt nhất, chúng tôi khuyên bạn nên sử dụng trình duyệt web hiện đại như Chrome, Firefox, Safari hoặc Edge phiên bản mới nhất. Đối với thiết bị di động, hãy đảm bảo rằng bạn đang sử dụng hệ điều hành iOS 12+ hoặc Android 8.0+ trở lên. Bạn cũng cần có kết nối internet ổn định với tốc độ tối thiểu 1Mbps.",
        },
        {
          id: 405,
          question: "Tôi không thể đăng nhập vào tài khoản của mình, phải làm sao?",
          answer:
            'Nếu bạn không thể đăng nhập, hãy thử: 1) Kiểm tra lại tên người dùng và mật khẩu, 2) Đặt lại mật khẩu bằng tính năng "Quên mật khẩu", 3) Xóa cookie và cache của trình duyệt, 4) Đảm bảo rằng bạn đã xác minh email của mình, 5) Kiểm tra xem tài khoản của bạn có bị khóa không. Nếu vẫn không đăng nhập được, vui lòng liên hệ với bộ phận hỗ trợ của chúng tôi.',
        },
      ],
    },
    {
      id: 5,
      name: "Bảo mật & Quyền riêng tư",
      icon: <FaShieldAlt />,
      faqs: [
        {
          id: 501,
          question: "Dữ liệu của tôi được bảo vệ như thế nào?",
          answer:
            "Chúng tôi sử dụng các biện pháp bảo mật tiên tiến để bảo vệ dữ liệu của bạn, bao gồm mã hóa SSL 256-bit, xác thực hai yếu tố, tường lửa tiên tiến và giám sát bảo mật 24/7. Tất cả dữ liệu người dùng được lưu trữ trên các máy chủ an toàn với các biện pháp bảo vệ vật lý và kỹ thuật nghiêm ngặt. Chúng tôi cam kết bảo vệ thông tin cá nhân của bạn và tuân thủ các quy định về bảo vệ dữ liệu.",
        },
        {
          id: 502,
          question: "BrainGame có thu thập dữ liệu người dùng không?",
          answer:
            "Có, chúng tôi thu thập một số dữ liệu người dùng để cải thiện dịch vụ của mình và cung cấp trải nghiệm cá nhân hóa. Dữ liệu này bao gồm thông tin cá nhân cơ bản, dữ liệu sử dụng và tiến trình học tập. Tuy nhiên, chúng tôi chỉ thu thập những thông tin cần thiết và không bao giờ chia sẻ thông tin cá nhân của bạn với bên thứ ba mà không có sự đồng ý rõ ràng của bạn. Bạn có thể tìm hiểu thêm trong Chính sách Quyền riêng tư của chúng tôi.",
        },
        {
          id: 503,
          question: "Làm thế nào để thay đổi cài đặt quyền riêng tư?",
          answer:
            'Để thay đổi cài đặt quyền riêng tư, hãy đăng nhập vào tài khoản của bạn và truy cập vào trang "Cài đặt tài khoản" > "Quyền riêng tư". Tại đây, bạn có thể điều chỉnh các tùy chọn quyền riêng tư, bao gồm những thông tin bạn muốn chia sẻ công khai, cài đặt thông báo, và cách chúng tôi sử dụng dữ liệu của bạn để cá nhân hóa trải nghiệm.',
        },
        {
          id: 504,
          question: "Tôi có thể yêu cầu xóa dữ liệu của mình không?",
          answer:
            'Có, theo quy định về bảo vệ dữ liệu, bạn có quyền yêu cầu xóa dữ liệu cá nhân của mình. Để thực hiện điều này, vui lòng liên hệ với đội ngũ hỗ trợ của chúng tôi qua email privacy@braingame.vn với tiêu đề "Yêu cầu xóa dữ liệu". Chúng tôi sẽ xác minh danh tính của bạn và xử lý yêu cầu trong vòng 30 ngày. Lưu ý rằng việc xóa dữ liệu sẽ dẫn đến việc mất tất cả tiến trình và thành tích của bạn.',
        },
        {
          id: 505,
          question: "BrainGame có tuân thủ các quy định về bảo vệ dữ liệu không?",
          answer:
            "Có, BrainGame tuân thủ nghiêm ngặt các quy định về bảo vệ dữ liệu, bao gồm Luật Bảo vệ Dữ liệu Cá nhân của Việt Nam và các tiêu chuẩn quốc tế như GDPR (Quy định Bảo vệ Dữ liệu Chung của EU). Chúng tôi thường xuyên cập nhật các chính sách và quy trình của mình để đảm bảo tuân thủ các quy định mới nhất và áp dụng các biện pháp bảo vệ dữ liệu tốt nhất.",
        },
      ],
    },
  ];

  // Hàm để chuyển đổi trạng thái mở/đóng của câu hỏi
  const toggleQuestion = (questionId) => {
    setOpenQuestions((prev) => ({
      ...prev,
      [questionId]: !prev[questionId],
    }));
  };

  // Hàm xử lý tìm kiếm
  const handleSearch = (e) => {
    e.preventDefault();
    // Đã xử lý tìm kiếm thông qua state searchTerm
  };

  // Lọc câu hỏi theo tìm kiếm
  const getFilteredCategories = () => {
    if (!searchTerm.trim()) {
      return activeCategory
        ? faqCategories.filter((cat) => cat.id === activeCategory)
        : faqCategories;
    }

    return faqCategories
      .map((category) => {
        const filteredFaqs = category.faqs.filter(
          (faq) =>
            faq.question.toLowerCase().includes(searchTerm.toLowerCase()) ||
            faq.answer.toLowerCase().includes(searchTerm.toLowerCase())
        );

        return { ...category, faqs: filteredFaqs };
      })
      .filter((category) => category.faqs.length > 0);
  };

  const filteredCategories = getFilteredCategories();

  return (
    <div className="faq-page">
      {/* Hero Section */}
      <div className="faq-hero">
        <motion.div
          className="faq-hero-content"
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.6, ease: "easeOut" }}
        >
          <h1>Câu Hỏi Thường Gặp</h1>
          <p>Tìm câu trả lời nhanh chóng cho những thắc mắc của bạn</p>
        </motion.div>
      </div>

      <div className="faq-container">
        {/* Search Section */}
        <motion.div
          className="faq-search-section"
          variants={searchScale}
          initial="hidden"
          animate="show"
        >
          <form onSubmit={handleSearch}>
            <div className="search-input-container">
              <FaSearch className="search-icon" />
              <input
                type="text"
                placeholder="Tìm kiếm câu hỏi..."
                value={searchTerm}
                onChange={(e) => setSearchTerm(e.target.value)}
              />
            </div>
            <button type="submit" className="search-btn">
              Tìm kiếm
            </button>
          </form>
        </motion.div>

        {/* Category Tabs */}
        <motion.div
          className="faq-category-tabs"
          variants={stagger}
          initial="hidden"
          whileInView="show"
          viewport={viewportOnce}
        >
          <motion.button
            type="button"
            className={`category-tab ${activeCategory === null ? "active" : ""}`}
            onClick={() => setActiveCategory(null)}
            variants={cardItem}
          >
            {activeCategory === null && (
              <motion.span
                layoutId="activeFaqTab"
                className="tab-indicator"
                transition={{ type: "spring", stiffness: 380, damping: 30 }}
              />
            )}
            <span className="tab-content">Tất cả</span>
          </motion.button>
          {faqCategories.map((category) => (
            <motion.button
              type="button"
              key={category.id}
              className={`category-tab ${activeCategory === category.id ? "active" : ""}`}
              onClick={() => setActiveCategory(category.id)}
              variants={cardItem}
            >
              {activeCategory === category.id && (
                <motion.span
                  layoutId="activeFaqTab"
                  className="tab-indicator"
                  transition={{ type: "spring", stiffness: 380, damping: 30 }}
                />
              )}
              <span className="tab-content">
                {category.icon}
                <span>{category.name}</span>
              </span>
            </motion.button>
          ))}
        </motion.div>

        {/* FAQ Content */}
        <div className="faq-content">
          <motion.aside
            className="faq-sidebar"
            variants={fadeUp}
            initial="hidden"
            whileInView="show"
            viewport={viewportOnce}
          >
            <h3>Danh mục</h3>
            <div className="category-list">
              <motion.div
                className={`category-item ${activeCategory === null ? "active" : ""}`}
                onClick={() => {
                  setActiveCategory(null);
                  setSearchTerm("");
                }}
                whileHover={activeCategory === null ? undefined : { x: 5 }}
                transition={{ type: "spring", stiffness: 300, damping: 22 }}
              >
                {activeCategory === null && (
                  <motion.span
                    layoutId="activeCategoryBox"
                    className="active-category-bg"
                    transition={{ type: "spring", stiffness: 380, damping: 30 }}
                  />
                )}
                <span className="category-item-content">
                  <span>Tất cả câu hỏi</span>
                </span>
              </motion.div>
              {faqCategories.map((category) => {
                const active = activeCategory === category.id;
                return (
                  <motion.div
                    key={category.id}
                    className={`category-item ${active ? "active" : ""}`}
                    onClick={() => {
                      setActiveCategory(category.id);
                      setSearchTerm("");
                    }}
                    whileHover={active ? undefined : { x: 5 }}
                    transition={{ type: "spring", stiffness: 300, damping: 22 }}
                  >
                    {active && (
                      <motion.span
                        layoutId="activeCategoryBox"
                        className="active-category-bg"
                        transition={{ type: "spring", stiffness: 380, damping: 30 }}
                      />
                    )}
                    <span className="category-item-content">
                      {category.icon}
                      <span>{category.name}</span>
                    </span>
                  </motion.div>
                );
              })}
            </div>
            <div className="need-help">
              <h3>Vẫn cần trợ giúp?</h3>
              <p>Không tìm thấy câu trả lời bạn đang tìm kiếm?</p>
              <Link to="/contact" className="contact-btn">
                <FaHeadset className="contact-icon" />
                <span>Liên hệ hỗ trợ</span>
              </Link>
            </div>
          </motion.aside>

          <div className="faq-main">
            {filteredCategories.length === 0 ? (
              <motion.div
                className="no-results"
                initial={{ opacity: 0, y: 20 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ duration: 0.4 }}
              >
                <h3>Không tìm thấy kết quả</h3>
                <p>
                  Không tìm thấy câu hỏi nào phù hợp với tìm kiếm của bạn. Vui
                  lòng thử lại với từ khóa khác hoặc liên hệ với chúng tôi để
                  được hỗ trợ.
                </p>
                <Link to="/contact" className="contact-link">
                  Liên hệ hỗ trợ
                </Link>
              </motion.div>
            ) : (
              filteredCategories.map((category) => (
                <motion.div
                  className="faq-category"
                  key={category.id}
                  id={`category-${category.id}`}
                  variants={stagger}
                  initial="hidden"
                  whileInView="show"
                  viewport={viewportOnce}
                >
                  <motion.div className="category-header" variants={fadeUp}>
                    <div className="category-icon">{category.icon}</div>
                    <h2>{category.name}</h2>
                  </motion.div>
                  <motion.div className="faq-list" variants={stagger}>
                    {category.faqs.length === 0 ? (
                      <p className="no-questions">
                        Không tìm thấy câu hỏi nào trong danh mục này.
                      </p>
                    ) : (
                      category.faqs.map((faq) => {
                        const isOpen = !!openQuestions[faq.id];
                        return (
                          <motion.div
                            className={`faq-item ${isOpen ? "open" : ""}`}
                            key={faq.id}
                            variants={cardItem}
                            onClick={() => toggleQuestion(faq.id)}
                          >
                            <div className="faq-question">
                              <h3>{faq.question}</h3>
                              {isOpen ? <FaChevronUp /> : <FaChevronDown />}
                            </div>
                            <AnimatePresence initial={false}>
                              {isOpen && (
                                <motion.div
                                  key="answer"
                                  initial={{ height: 0, opacity: 0 }}
                                  animate={{ height: "auto", opacity: 1 }}
                                  exit={{ height: 0, opacity: 0 }}
                                  transition={{ duration: 0.3, ease: "easeOut" }}
                                  style={{ overflow: "hidden" }}
                                >
                                  <div className="faq-answer">
                                    <p>{faq.answer}</p>
                                  </div>
                                </motion.div>
                              )}
                            </AnimatePresence>
                          </motion.div>
                        );
                      })
                    )}
                  </motion.div>
                </motion.div>
              ))
            )}
          </div>
        </div>

        {/* FAQ Footer */}
        <motion.div
          className="faq-footer"
          variants={fadeUp}
          initial="hidden"
          whileInView="show"
          viewport={viewportOnce}
        >
          <h2>Không tìm thấy câu trả lời bạn cần?</h2>
          <p>Đội ngũ hỗ trợ của chúng tôi luôn sẵn sàng giúp đỡ bạn</p>
          <div className="footer-actions">
            <Link to="/contact" className="footer-btn primary">
              <FaHeadset className="btn-icon" />
              <span>Liên hệ hỗ trợ</span>
            </Link>
            <Link to="/feedback" className="footer-btn secondary">
              <span>Gửi phản hồi</span>
            </Link>
          </div>
        </motion.div>
      </div>
    </div>
  );
}

export default FAQPage;
