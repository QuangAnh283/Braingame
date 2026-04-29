import { ReactNode } from "react";
import { Navigate } from "react-router-dom";
import authStore from "../../stores/auth-store";

type RoleBasedRouteProps = {
  children: ReactNode;
  allowedRoles: string[];
  redirectTo?: string;
};

const RoleBasedRoute = ({ children, allowedRoles, redirectTo = "/dashboard" }: RoleBasedRouteProps) => {
  const role = authStore((state) => state.user?.role);

  if (!role || !allowedRoles.includes(role)) {
    return <Navigate to={redirectTo} replace />;
  }

  return <>{children}</>;
};

export default RoleBasedRoute;
