import { render, screen, within } from '@testing-library/react';
import { MemoryRouter } from 'react-router';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import AppLayout from '@/layout/AppLayout';
import { useAuth } from '@/auth/auth-context';
import type { Permission } from '@/types/api';

vi.mock('@/auth/auth-context', () => ({ useAuth: vi.fn() }));
vi.mock('@/realtime/realtime-context', () => ({
  useRealtime: () => ({ status: 'DISCONNECTED', connected: false, lastEvent: null, events: [] }),
}));
vi.mock('@/lib/theme', () => ({ useTheme: () => ({ theme: 'light', setTheme: vi.fn() }) }));

const authMock = vi.mocked(useAuth);

function permissions(values: Permission[]) {
  authMock.mockReturnValue({
    user: { id: 1, username: 'viewer', fullName: 'Leitor', role: 'VIEWER', permissions: values },
    isAuthenticated: true, isLoading: false,
    hasPermission: (permission: Permission) => values.includes(permission),
    login: vi.fn(), logout: vi.fn(), refreshMe: vi.fn(),
  });
}

beforeEach(() => { vi.clearAllMocks(); permissions([]); });

describe('AppLayout permission-based menus', () => {
  it('shows no business navigation for an authenticated user with no permissions', () => {
    render(<MemoryRouter><AppLayout /></MemoryRouter>);
    expect(within(screen.getByRole('navigation')).queryAllByRole('link')).toHaveLength(0);
  });

  it.each([
    ['USER_READ', '/users'],
    ['VEHICLE_READ', '/vehicles'],
    ['MOVEMENT_READ', '/portaria'],
    ['APARTMENT_WRITE', '/unified-registration'],
    ['AUDIT_READ', '/audit'],
  ] as const)('shows %s navigation only while permission remains granted', (permission, path) => {
    permissions([permission]);
    const view = render(<MemoryRouter><AppLayout /></MemoryRouter>);
    const nav = screen.getByRole('navigation');
    expect(within(nav).getAllByRole('link').some((link) => link.getAttribute('href') === path)).toBe(true);

    permissions([]);
    view.rerender(<MemoryRouter><AppLayout /></MemoryRouter>);
    expect(within(screen.getByRole('navigation')).queryAllByRole('link')).toHaveLength(0);
  });
});
