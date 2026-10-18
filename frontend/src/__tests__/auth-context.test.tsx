import { act, cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { AuthProvider, useAuth } from '@/auth/auth-context';
import { clearSession, readSession, saveSession, type AuthSession } from '@/auth/auth-storage';
import type { UserSummary } from '@/types/api';

const apiMocks = vi.hoisted(() => ({ login: vi.fn(), me: vi.fn(), revoke: vi.fn() }));
vi.mock('@/services/api', () => ({ api: apiMocks }));

const user: UserSummary = {
  id: 1, username: 'admin', fullName: 'Administrador', role: 'ADMIN',
  permissions: ['DASHBOARD_READ', 'USER_WRITE'],
};

function session(expiresIn = 60_000): AuthSession {
  return { accessToken: 'session-token', tokenType: 'Bearer', expiresAt: new Date(Date.now() + expiresIn).toISOString(), user };
}

function Consumer() {
  const auth = useAuth();
  return <>
    <output data-testid="session">{auth.isAuthenticated ? auth.user?.username : 'anonymous'}</output>
    <output data-testid="loading">{String(auth.isLoading)}</output>
    <output data-testid="can-write">{String(auth.hasPermission('USER_WRITE'))}</output>
    <button onClick={() => { void auth.login('admin', 'secret'); }}>Login</button>
    <button onClick={auth.logout}>Logout</button>
    <button onClick={() => { void auth.refreshMe(); }}>Refresh</button>
  </>;
}

function renderSession() {
  return render(<AuthProvider><Consumer /></AuthProvider>);
}

beforeEach(() => {
  clearSession();
  vi.resetAllMocks();
  apiMocks.me.mockResolvedValue({ user });
  apiMocks.revoke.mockResolvedValue(undefined);
  apiMocks.login.mockImplementation(async () => session());
});

afterEach(() => {
  cleanup();
  clearSession();
  vi.useRealTimers();
});

describe('AuthProvider session lifecycle', () => {
  it('starts anonymous without requesting or revoking a missing session', () => {
    renderSession();
    expect(screen.getByTestId('session')).toHaveTextContent('anonymous');
    expect(screen.getByTestId('can-write')).toHaveTextContent('false');
    expect(apiMocks.me).not.toHaveBeenCalled();
    expect(apiMocks.revoke).not.toHaveBeenCalled();
  });

  it('revalidates a stored session before releasing the protected content', async () => {
    saveSession(session());
    let finish: ((value: { user: UserSummary }) => void) | undefined;
    apiMocks.me.mockReturnValue(new Promise<{ user: UserSummary }>((resolve) => { finish = resolve; }));
    renderSession();
    expect(screen.getByTestId('loading')).toHaveTextContent('true');
    await act(async () => { finish?.({ user: { ...user, permissions: ['DASHBOARD_READ'] } }); });
    expect(screen.getByTestId('loading')).toHaveTextContent('false');
    expect(screen.getByTestId('can-write')).toHaveTextContent('false');
    expect(readSession()?.user.permissions).toEqual(['DASHBOARD_READ']);
  });

  it('saves a successful login and clears it immediately on logout while revoking the token', async () => {
    renderSession();
    fireEvent.click(screen.getByRole('button', { name: 'Login' }));
    await waitFor(() => expect(screen.getByTestId('session')).toHaveTextContent('admin'));
    expect(apiMocks.login).toHaveBeenCalledWith('admin', 'secret');
    expect(readSession()?.accessToken).toBe('session-token');
    apiMocks.revoke.mockReturnValue(new Promise<void>(() => {}));
    fireEvent.click(screen.getByRole('button', { name: 'Logout' }));
    expect(screen.getByTestId('session')).toHaveTextContent('anonymous');
    expect(readSession()).toBeNull();
    expect(apiMocks.revoke).toHaveBeenCalledWith('session-token');
  });

  it('updates permissions on focus after an administrative change', async () => {
    saveSession(session());
    renderSession();
    await waitFor(() => expect(screen.getByTestId('loading')).toHaveTextContent('false'));
    expect(screen.getByTestId('can-write')).toHaveTextContent('true');
    apiMocks.me.mockResolvedValue({ user: { ...user, role: 'VIEWER', permissions: ['DASHBOARD_READ'] } });
    fireEvent.focus(window);
    await waitFor(() => expect(screen.getByTestId('can-write')).toHaveTextContent('false'));
    expect(readSession()?.user.role).toBe('VIEWER');
  });

  it('does not infer permissions from a SUPER_ADMIN role name', async () => {
    saveSession(session());
    apiMocks.me.mockResolvedValue({ user: { ...user, role: 'SUPER_ADMIN', permissions: [] } });
    renderSession();
    await waitFor(() => expect(screen.getByTestId('loading')).toHaveTextContent('false'));
    expect(screen.getByTestId('can-write')).toHaveTextContent('false');
  });

  it('logs out a session rejected by the backend', async () => {
    saveSession(session());
    apiMocks.me.mockRejectedValue(new Error('UNAUTHORIZED'));
    renderSession();
    await waitFor(() => expect(screen.getByTestId('session')).toHaveTextContent('anonymous'));
    expect(readSession()).toBeNull();
    expect(apiMocks.revoke).toHaveBeenCalledWith('session-token');
  });

  it('does not restore a session when an old refresh finishes after logout', async () => {
    saveSession(session());
    let finish: ((value: { user: UserSummary }) => void) | undefined;
    apiMocks.me.mockReturnValue(new Promise<{ user: UserSummary }>((resolve) => { finish = resolve; }));
    renderSession();
    fireEvent.click(screen.getByRole('button', { name: 'Logout' }));
    await act(async () => { finish?.({ user }); });
    expect(screen.getByTestId('session')).toHaveTextContent('anonymous');
    expect(readSession()).toBeNull();
  });

  it('ends an open session at its expiry without requiring another API call', async () => {
    vi.useFakeTimers();
    saveSession(session(5_000));
    renderSession();
    await act(async () => {});
    expect(screen.getByTestId('session')).toHaveTextContent('admin');
    await act(async () => { vi.advanceTimersByTime(5_000); });
    expect(screen.getByTestId('session')).toHaveTextContent('anonymous');
    expect(readSession()).toBeNull();
  });

  it('revalidates periodically and removes its timers on unmount', async () => {
    vi.useFakeTimers();
    saveSession(session(120_000));
    const view = renderSession();
    await act(async () => {});
    expect(apiMocks.me).toHaveBeenCalledTimes(1);
    apiMocks.me.mockResolvedValue({ user: { ...user, permissions: [] } });
    await act(async () => { vi.advanceTimersByTime(30_000); });
    expect(apiMocks.me).toHaveBeenCalledTimes(2);
    expect(screen.getByTestId('can-write')).toHaveTextContent('false');
    view.unmount();
    await act(async () => { vi.advanceTimersByTime(30_000); });
    expect(apiMocks.me).toHaveBeenCalledTimes(2);
  });
});
