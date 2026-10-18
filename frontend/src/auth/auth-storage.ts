import type { UserSummary } from '@/types/api';

const STORAGE_KEY = 'garage-control.auth';

export interface AuthSession {
  accessToken: string;
  tokenType: string;
  expiresAt: string;
  user: UserSummary;
}

export function readSession(): AuthSession | null {
  const raw = sessionStorage.getItem(STORAGE_KEY);
  if (!raw) return null;

  try {
    const parsed = JSON.parse(raw) as AuthSession;
    if (!parsed.accessToken || !parsed.user || !Array.isArray(parsed.user.permissions) || !Number.isFinite(Date.parse(parsed.expiresAt)) || Date.parse(parsed.expiresAt) <= Date.now()) {
      clearSession();
      return null;
    }
    return parsed;
  } catch {
    clearSession();
    return null;
  }
}

export function saveSession(session: AuthSession): void {
  sessionStorage.setItem(STORAGE_KEY, JSON.stringify(session));
}

export function clearSession(): void {
  sessionStorage.removeItem(STORAGE_KEY);
}
