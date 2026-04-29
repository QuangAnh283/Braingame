import { Navigate } from "react-router-dom";
import authStore from "../../stores/auth-store";

const RoleBasedRedirect = () => {
  const typeAccount = authStore((state) => state.typeAccount);

  if (typeAccount === "ADMIN") return <Navigate to="/admin/dashboard" replace />;
  if (typeAccount === "TEACHER") return <Navigate to="/teacher/dashboard" replace />;
  if (typeAccount === "PLAYER") return <Navigate to="/dashboard" replace />;
  return <Navigate to="/" replace />;
};

export default RoleBasedRedirect;
