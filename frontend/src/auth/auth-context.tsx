import { createContext, useCallback, useContext, useEffect, useMemo, useRef, useState, type ReactNode } from 'react';
import type { Permission, UserSummary } from '@/types/api';
import { clearSession, readSession, saveSession, type AuthSession } from './auth-storage';
import { api } from '@/services/api';
interface AuthContextValue {
  user: UserSummary | null; isAuthenticated: boolean; isLoading: boolean;
  login: (username: string, password: string) => Promise<void>;
  logout: () => void; hasPermission: (permission: Permission) => boolean; refreshMe: () => Promise<void>;
}
const AuthContext = createContext<AuthContextValue | undefined>(undefined);
export function AuthProvider({ children }: { children: ReactNode }) {
  const [session, setSession] = useState<AuthSession | null>(() => readSession());
  const current = useRef(session);
  const [isLoading, setIsLoading] = useState(Boolean(session));
  const replace = useCallback((next: AuthSession | null) => { current.current = next; setSession(next); }, []);
  const logout = useCallback(() => {
    const token = current.current?.accessToken;
    clearSession(); replace(null); setIsLoading(false);
    if (token) void api.revoke(token).catch(() => { /* Local logout is immediate; the token still has a fixed expiry. */ });
  }, [replace]);
  const login = useCallback(async (username: string, password: string) => {
    const result = await api.login(username, password);
    const next: AuthSession = { accessToken: result.accessToken, tokenType: result.tokenType, expiresAt: result.expiresAt, user: result.user };
    saveSession(next); replace(next); setIsLoading(false);
  }, [replace]);
  const refreshMe = useCallback(async () => {
    const previous = current.current;
    if (!previous) return;
    try {
      const result = await api.me();
      if (current.current?.accessToken !== previous.accessToken) return;
      const next = { ...previous, user: result.user };
      saveSession(next); replace(next);
    } catch (error) {
      if (error instanceof Error && error.message === 'UNAUTHORIZED') logout();
    } finally { setIsLoading(false); }
  }, [logout, replace]);
  useEffect(() => {
    void refreshMe();
    const onFocus = () => { void refreshMe(); };
    const onVisible = () => { if (document.visibilityState === 'visible') void refreshMe(); };
    window.addEventListener('focus', onFocus);
    document.addEventListener('visibilitychange', onVisible);
    const timer = window.setInterval(onFocus, 30_000);
    return () => { window.clearInterval(timer); window.removeEventListener('focus', onFocus); document.removeEventListener('visibilitychange', onVisible); };
  }, [refreshMe]);
  useEffect(() => {
    if (!session) return;
    let timer: number;
    const check = () => {
      const remaining = Date.parse(session.expiresAt) - Date.now();
      if (!Number.isFinite(remaining) || remaining <= 0) logout();
      else timer = window.setTimeout(check, Math.min(remaining, 2_147_483_647));
    };
    check(); return () => window.clearTimeout(timer);
  }, [session, logout]);
  const hasPermission = useCallback((permission: Permission) => Boolean(session?.user.permissions.includes(permission)), [session]);
  const value = useMemo(() => ({ user: session?.user ?? null, isAuthenticated: Boolean(session), isLoading, login, logout, hasPermission, refreshMe }), [session, isLoading, login, logout, hasPermission, refreshMe]);
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}
export function useAuth(): AuthContextValue {
  const context = useContext(AuthContext);
  if (!context) throw new Error('useAuth must be used inside AuthProvider');
  return context;
}
