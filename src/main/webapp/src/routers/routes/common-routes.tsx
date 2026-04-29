// Routes dùng chung cho tất cả users (không yêu cầu role cụ thể)
// Lưu ý: Đã được bảo vệ tự động bởi <ProtectedRoute /> ở layer layout trong routers/index.tsx

import Profile from "../../pages/profile";

const commonRoutes = [
  {
    path: "profile",
    element: <Profile />,
  },
];

export default commonRoutes;
