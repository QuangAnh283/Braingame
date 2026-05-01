import { ReactNode, useEffect, useState } from "react";
import { Navigate } from "react-router-dom";
import authStore from "../../stores/auth-store";

/**
 * ProtectedRoute - Component bảo vệ route yêu cầu xác thực
 * Chỉ cho phép truy cập nếu user đã đăng nhập
 */
type ProtectedRouteProps = {
  children: ReactNode;
};

const ProtectedRoute = ({ children }: ProtectedRouteProps) => {
  const isAuthenticated = authStore((state) => state.isAuthenticated);
  const isLoading = authStore((state) => state.isLoading);
  const [isInitialized, setIsInitialized] = useState(false);

  useEffect(() => {
    const initializeAuth = async () => {
      try {
        await authStore.getState().initialize();
      } catch (error) {
        console.error("Auth initialization error:", error);
      } finally {
        setIsInitialized(true);
      }
    };

    initializeAuth();
  }, []);

  // Hiển thị loading khi đang khởi tạo hoặc đang tải
  if (!isInitialized || isLoading) {
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

  // Redirect về trang chủ nếu chưa đăng nhập
  if (!isAuthenticated) {
    return <Navigate to="/" replace />;
  }

  return children;
};

export default ProtectedRoute;
