import { beforeEach, describe, expect, it, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router';
import UsersPage from '@/pages/UsersPage';

const { list, get, create, update, resetPassword } = vi.hoisted(() => ({
  list: vi.fn(),
  get: vi.fn(),
  create: vi.fn(),
  update: vi.fn(),
  resetPassword: vi.fn(),
}));

vi.mock('@/services/api', async () => {
  const actual =
    await vi.importActual<typeof import('@/services/api')>('@/services/api');

  return {
    ...actual,
    api: {
      ...actual.api,
      users: {
        list,
        get,
        create,
        update,
        resetPassword,
      },
    },
  };
});

vi.mock('@/auth/auth-context', () => ({
  useAuth: () => ({
    user: {
      id: 1,
      fullName: 'Admin',
      role: 'ADMIN',
      permissions: ['USER_READ', 'USER_WRITE'],
    },
    hasPermission: (permission: string) =>
      permission === 'USER_READ' || permission === 'USER_WRITE',
  }),
}));

describe('UsersPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();

    list.mockResolvedValue([
      {
        id: 1,
        username: 'admin',
        fullName: 'Administrador',
        role: 'ADMIN',
        permissions: ['USER_READ', 'USER_WRITE'],
        active: true,
        createdAt: '2026-01-01T00:00:00Z',
        updatedAt: '2026-01-01T00:00:00Z',
      },
    ]);
  });

  it('renders users from backend', async () => {
    render(
      <MemoryRouter>
        <UsersPage />
      </MemoryRouter>,
    );

    expect(await screen.findByText('admin')).toBeInTheDocument();

    expect(
      screen.getByText('Administrador', { selector: '.inline-flex' }),
    ).toBeInTheDocument();
  });
});