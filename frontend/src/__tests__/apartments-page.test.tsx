import { render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import ApartmentsPage from '@/pages/ApartmentsPage';
import { api } from '@/services/api';
import { useAuth } from '@/auth/auth-context';

vi.mock('@/services/api', () => ({
  api: {
    apartments: { list: vi.fn(), create: vi.fn(), update: vi.fn(), residents: vi.fn() },
  },
  getApiErrorMessage: vi.fn(() => 'Erro detectado'),
}));

vi.mock('@/auth/auth-context', () => ({
  useAuth: vi.fn(),
}));

const apartmentsApi = vi.mocked(api.apartments);
const authMock = vi.mocked(useAuth);

beforeEach(() => {
  vi.clearAllMocks();
  authMock.mockReturnValue({ hasPermission: vi.fn().mockReturnValue(true), user: { role: 'ADMIN' }, logout: vi.fn(), isAuthenticated: true, login: vi.fn() } as never);
  apartmentsApi.list.mockResolvedValue([
    { id: 1, number: '10', active: true },
  ]);
});

describe('ApartmentsPage', () => {
  it('renders title', async () => {
    render(<MemoryRouter><ApartmentsPage /></MemoryRouter>);
    await waitFor(() => expect(screen.getByText('Apartamentos')).toBeInTheDocument());
  });

  it('shows apartments', async () => {
    render(<MemoryRouter><ApartmentsPage /></MemoryRouter>);
    await waitFor(() => expect(screen.getByText('Apartamento 10')).toBeInTheDocument());
  });

  it('shows empty state', async () => {
    apartmentsApi.list.mockResolvedValue([]);
    render(<MemoryRouter><ApartmentsPage /></MemoryRouter>);
    await waitFor(() => expect(screen.getByText('Nenhum apartamento')).toBeInTheDocument());
  });

  it('shows error on failure', async () => {
    apartmentsApi.list.mockRejectedValue(new Error('FAIL'));
    render(<MemoryRouter><ApartmentsPage /></MemoryRouter>);
    await waitFor(() => expect(screen.getByRole('alert')).toBeInTheDocument());
  });
});
