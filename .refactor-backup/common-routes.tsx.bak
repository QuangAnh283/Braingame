// Routes dùng chung cho tất cả users (không yêu cầu role cụ thể)

import Profile from "../../pages/profile";
import ProtectedRoute from "../guards/protected-route";

const commonRoutes = [
  {
    path: "profile",
    element: (
      <ProtectedRoute>
        <Profile />
      </ProtectedRoute>
    ),
  },
];

export default commonRoutes;
