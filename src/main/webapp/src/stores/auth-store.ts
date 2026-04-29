import { create } from 'zustand';
import authApi from '../services/auth-api';

type UserRole = 'PLAYER' | 'TEACHER' | 'HOST' | 'ADMIN' | null;

type AuthUser = {
  id?: number | string;
  role?: Exclude<UserRole, null>;
  typeAccount?: Exclude<UserRole, null>;
  [key: string]: unknown;
} | null;

type AuthState = {
  user: AuthUser;
  isAuthenticated: boolean;
  isLoading: boolean;
  hasInitialized: boolean;
  error: string | null;
  typeAccount: UserRole;
  setUser: (_user: AuthUser) => void;
  clearUser: () => void;
  setLoading: (_isLoading: boolean) => void;
  setError: (_error: string | null) => void;
  initialize: () => Promise<void>;
  login: (_username: string, _password: string) => Promise<any>;
  logout: () => Promise<void>;
};

const toUiRole = (role?: string | null): UserRole => {
  if (!role) return null;
  if (role === 'HOST') return 'TEACHER';
  if (role === 'ADMIN' || role === 'PLAYER' || role === 'TEACHER') return role;
  return null;
};

const normalizeUser = (data: any): AuthUser => {
  if (!data) return null;
  const role = toUiRole(data.typeAccount || data.role);
  return { ...data, id: data.id ?? data.userId, role: role || undefined, typeAccount: role || undefined };
};

const errorMessage = (error: any, fallback: string) =>
  error?.response?.data?.message || error?.message || fallback;

const authStore = create<AuthState>((set, get) => ({
  user: null,
  isAuthenticated: false,
  isLoading: false,
  hasInitialized: false,
  error: null,
  typeAccount: null,

  setUser: (user) =>
      set(() => {
        const normalizedUser = normalizeUser(user);
        return {
          user: normalizedUser,
          isAuthenticated: !!normalizedUser,
          typeAccount: normalizedUser?.typeAccount || null,
          error: null,
        };
      }),

  clearUser: () =>
      set({
        user: null,
        isAuthenticated: false,
        typeAccount: null,
        error: null,
      }),

  setLoading: (isLoading) => set({ isLoading }),

  setError: (error) => set({ error }),

  initialize: async () => {
    if (get().hasInitialized || get().isLoading) return;
    set({ isLoading: true, error: null });
    try {
      let response;
      try {
        response = await authApi.getUser();
      } catch {
        await authApi.refreshToken();
        response = await authApi.getUser();
      }

      const user = normalizeUser(response.data);
      set({
        user,
        isAuthenticated: !!user,
        typeAccount: user?.typeAccount || null,
        isLoading: false,
        hasInitialized: true,
        error: null,
      });
    } catch {
      set({ user: null, isAuthenticated: false, typeAccount: null, isLoading: false, hasInitialized: true });
    }
  },

  login: async (username, password) => {
    try {
      set({ isLoading: true, error: null });
      const response = await authApi.login({ username, password });
      const user = normalizeUser(response.data);

      set({
        user,
        isAuthenticated: !!user,
        typeAccount: user?.typeAccount || null,
        isLoading: false,
      });
      return response;
    } catch (error) {
      const message = errorMessage(error, 'Đăng nhập thất bại');
      set({ isLoading: false, error: message });
      throw error;
    }
  },

  logout: async () => {
    try {
      set({ isLoading: true, error: null });
      await authApi.logout();
      set({
        user: null,
        isAuthenticated: false,
        typeAccount: null,
        isLoading: false,
      });
    } catch (error) {
      const message = errorMessage(error, 'Đăng xuất thất bại');
      set({ isLoading: false, error: message });
      throw error;
    }
  },
}));

export default authStore;
