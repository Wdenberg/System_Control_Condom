import { beforeEach, describe, expect, it, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router';
import RolesPage from '@/pages/RolesPage';

const { list } = vi.hoisted(() => ({
  list: vi.fn(),
}));

vi.mock('@/services/api', async () => {
  const actual =
    await vi.importActual<typeof import('@/services/api')>('@/services/api');

  return {
    ...actual,
    api: {
      ...actual.api,
      roles: {
        list,
      },
    },
  };
});

describe('RolesPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();

    list.mockResolvedValue([
      {
        id: 1,
        code: 'ADMIN',
        active: true,
        permissions: ['USER_READ', 'USER_WRITE'],
      },
      {
        id: 2,
        code: 'VIEWER',
        active: true,
        permissions: ['DASHBOARD_READ'],
      },
    ]);
  });

  it('renders role catalog and permissions', async () => {
    render(
      <MemoryRouter>
        <RolesPage />
      </MemoryRouter>,
    );

    expect(
    await screen.findByRole('button', { name: /Administrador/i }),
    ).toBeInTheDocument();

    expect(screen.getByText('Usuários')).toBeInTheDocument();
  });
});