import { createBrowserRouter } from "react-router-dom";
import App from "../App";
import ErrorPage from "../pages/error-page";
import ProtectedRoute from "./guards/protected-route";
import RoleBasedRedirect from "./guards/role-based-redirect";
import {
  publicRoutes,
  commonRoutes,
  playerRoutes,
  teacherRoutes,
  adminRoutes,
} from "./routes";

/**
 * Router Configuration
 *
 * Cấu trúc:
 * - Public Routes: Không cần đăng nhập (/, /login, /register, ...)
 * - Private subtree: Bọc trong <ProtectedRoute /> như một layout route -> chỉ mount 1 lần
 *   trong toàn bộ phiên private, không re-mount khi chuyển giữa các route con.
 *   - Common Routes: Cần đăng nhập, không phân biệt role (/profile)
 *   - Player Routes: Dành cho PLAYER (/dashboard, /rooms, /game, ...)
 *   - Teacher Routes: Dành cho TEACHER (/teacher/dashboard, /teacher/topics, ...)
 *   - Admin Routes: Dành cho ADMIN (/admin/dashboard, /admin/users, ...)
 */
const router = createBrowserRouter([
  {
    path: "/",
    element: <App />,
    children: [
      // Public routes - không cần đăng nhập
      ...publicRoutes,

      // Private subtree - bảo vệ một lần qua ProtectedRoute layout route
      {
        element: <ProtectedRoute />,
        children: [
          { path: "auto-redirect", element: <RoleBasedRedirect /> },
          ...commonRoutes,
          ...playerRoutes,
          ...teacherRoutes,
          ...adminRoutes,
        ],
      },

      // Error page
      { path: "*", element: <ErrorPage /> },
    ],
  },
]);

export { router };
