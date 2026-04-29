import { useEffect } from "react";
import { Navigate, Outlet } from "react-router-dom";
import authStore from "../../stores/auth-store";

const ProtectedRoute = () => {
  const isAuthenticated = authStore((state) => state.isAuthenticated);
  const hasInitialized = authStore((state) => state.hasInitialized);

  useEffect(() => {
    if (!authStore.getState().hasInitialized) {
      authStore.getState().initialize();
    }
  }, []);

  if (!hasInitialized) {
    return (
      <div
        style={{
          display: "flex",
          justifyContent: "center",
          alignItems: "center",
          height: "100vh",
          fontSize: "1.2rem",
          color: "#666",
        }}
      >
        Đang tải...
      </div>
    );
  }

  if (!isAuthenticated) {
    return <Navigate to="/" replace />;
  }

  return <Outlet />;
};

export default ProtectedRoute;
