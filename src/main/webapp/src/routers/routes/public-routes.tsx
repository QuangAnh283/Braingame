// Routes công khai (không cần đăng nhập)

import AboutPage from "../../pages/footer/about-page";
import BlogPage from "../../pages/footer/blog-page";
import ContactPage from "../../pages/footer/contact-page";
import FaqPage from "../../pages/footer/faqpage";
import FeedbackPage from "../../pages/footer/feedback-page";
import HelpPage from "../../pages/footer/help-page";
import PrivacyPage from "../../pages/footer/privacy-page";
import TermsPage from "../../pages/footer/terms-page";
import ForgotPassword from "../../pages/auth/forgot-password";
import Login from "../../pages/auth/login";
import Register from "../../pages/auth/register";
import ResetPassword from "../../pages/auth/reset-password";
import VerifyEmail from "../../pages/auth/verify-email";
import FutureGamesPage from "../../pages/future-games-page";
import Welcome from "../../pages/welcome";

const publicRoutes = [
  { index: true, element: <Welcome /> },
  { path: "login", element: <Login /> },
  { path: "register", element: <Register /> },
  { path: "forgot-password", element: <ForgotPassword /> },
  { path: "reset-password", element: <ResetPassword /> },
  { path: "verify-email", element: <VerifyEmail /> },
  { path: "about", element: <AboutPage /> },
  { path: "blog", element: <BlogPage /> },
  { path: "contact", element: <ContactPage /> },
  { path: "help", element: <HelpPage /> },
  { path: "faq", element: <FaqPage /> },
  { path: "privacy", element: <PrivacyPage /> },
  { path: "terms", element: <TermsPage /> },
  { path: "feedback", element: <FeedbackPage /> },
  { path: "games", element: <FutureGamesPage /> },
];

export default publicRoutes;
