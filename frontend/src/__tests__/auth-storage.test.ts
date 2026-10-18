import { afterEach, describe, expect, it } from 'vitest';
import { clearSession, readSession, saveSession, type AuthSession } from '@/auth/auth-storage';
import type { UserSummary } from '@/types/api';

const user: UserSummary = {
  id: 1,
  username: 'admin',
  fullName: 'Administrador',
  role: 'ADMIN',
  permissions: ['DASHBOARD_READ', 'VEHICLE_READ'],
};

const session: AuthSession = {
  accessToken: 'token',
  tokenType: 'Bearer',
  expiresAt: new Date(Date.now() + 60_000).toISOString(),
  user,
};

afterEach(() => clearSession());

describe('auth-storage', () => {
  it('saves and restores a valid session', () => {
    saveSession(session);
    expect(readSession()).toEqual(session);
  });

  it('clears expired sessions', () => {
    saveSession({ ...session, expiresAt: new Date(Date.now() - 1_000).toISOString() });
    expect(readSession()).toBeNull();
    expect(sessionStorage.getItem('garage-control.auth')).toBeNull();
  });

  it('rejects malformed expiry instead of accepting an unbounded session', () => {
    saveSession({ ...session, expiresAt: 'invalid-date' });
    expect(readSession()).toBeNull();
    expect(sessionStorage.getItem('garage-control.auth')).toBeNull();
  });

  it('clears corrupted stored JSON', () => {
    sessionStorage.setItem('garage-control.auth', '{invalid');
    expect(readSession()).toBeNull();
    expect(sessionStorage.getItem('garage-control.auth')).toBeNull();
  });
});
