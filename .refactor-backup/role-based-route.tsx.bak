import { ReactNode } from "react";
import { Navigate } from "react-router-dom";
import authStore from "../../stores/auth-store";
import ProtectedRoute from "./protected-route";

/**
 * RoleBasedRoute - Component bảo vệ route dựa trên vai trò
 * @param {string[]} allowedRoles - Danh sách các role được phép truy cập
 * @param {string} redirectTo - Đường dẫn redirect nếu không có quyền (mặc định: /dashboard)
 */
type RoleBasedRouteProps = {
  children: ReactNode;
  allowedRoles: string[];
  redirectTo?: string;
};

const RoleBasedRoute = ({ children, allowedRoles, redirectTo = "/dashboard" }: RoleBasedRouteProps) => {
  const user = authStore((state) => state.user);

  return (
    <ProtectedRoute>
      {allowedRoles.includes(user?.role) ? (
        children
      ) : (
        <Navigate to={redirectTo} replace />
      )}
    </ProtectedRoute>
  );
};

export default RoleBasedRoute;
