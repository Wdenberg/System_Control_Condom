import { render, screen } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { MemoryRouter } from 'react-router';
import ProtectedRoute from '@/auth/ProtectedRoute';

vi.mock('@/auth/auth-context', () => ({
  useAuth: () => ({
    isAuthenticated: authState.isAuthenticated,
    isLoading: authState.isLoading,
    hasPermission: (p: string) => authState.permissions.includes(p),
  }),
}));

vi.mock('react-router', async () => {
  const actual = await vi.importActual<typeof import('react-router')>('react-router');
  return {
    ...actual,
    Navigate: ({ to }: { to: string }) => <span data-testid="navigate" data-to={to} />,
  };
});

const authState = { isAuthenticated: false, isLoading: false, permissions: [] as string[] };

function setAuth(overrides: Partial<typeof authState>) {
  Object.assign(authState, overrides);
}

beforeEach(() => setAuth({ isAuthenticated: false, isLoading: false, permissions: [] }));

describe('ProtectedRoute', () => {
  it('waits for session validation before showing protected content or redirecting', () => {
    setAuth({ isAuthenticated: true, isLoading: true, permissions: ['DASHBOARD_READ'] });
    const { unmount } = render(<MemoryRouter><ProtectedRoute permission="DASHBOARD_READ"><div>Protected content</div></ProtectedRoute></MemoryRouter>);
    expect(screen.getByText('Validando sessão…')).toBeInTheDocument();
    expect(screen.queryByText('Protected content')).not.toBeInTheDocument();
    expect(screen.queryByTestId('navigate')).not.toBeInTheDocument();
    unmount();
    setAuth({ isLoading: false });
  });

  it('redirects to /login when not authenticated', () => {
    setAuth({ isAuthenticated: false });
    render(
      <MemoryRouter initialEntries={['/dashboard']}>
        <ProtectedRoute>
          <div>Protected content</div>
        </ProtectedRoute>
      </MemoryRouter>
    );
    expect(screen.queryByText('Protected content')).not.toBeInTheDocument();
    expect(screen.getByTestId('navigate')).toHaveAttribute('data-to', '/login');
  });

  it('renders children when authenticated and no permission required', () => {
    setAuth({ isAuthenticated: true, permissions: [] });
    render(
      <MemoryRouter>
        <ProtectedRoute>
          <div>Protected content</div>
        </ProtectedRoute>
      </MemoryRouter>
    );
    expect(screen.getByText('Protected content')).toBeInTheDocument();
  });

  it('shows access denied when missing required permission', () => {
    setAuth({ isAuthenticated: true, permissions: [] });
    render(
      <MemoryRouter>
        <ProtectedRoute permission="DASHBOARD_READ">
          <div>Protected content</div>
        </ProtectedRoute>
      </MemoryRouter>
    );
    expect(screen.queryByText('Protected content')).not.toBeInTheDocument();
    expect(screen.getByText(/Acesso negado/)).toBeInTheDocument();
  });

  it('renders children when authenticated and has required permission', () => {
    setAuth({ isAuthenticated: true, permissions: ['DASHBOARD_READ'] });
    render(
      <MemoryRouter>
        <ProtectedRoute permission="DASHBOARD_READ">
          <div>Protected content</div>
        </ProtectedRoute>
      </MemoryRouter>
    );
    expect(screen.getByText('Protected content')).toBeInTheDocument();
  });
});
