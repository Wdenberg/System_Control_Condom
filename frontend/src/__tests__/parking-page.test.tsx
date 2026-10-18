import { render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import ParkingPage from '@/pages/ParkingPage';
import { api } from '@/services/api';
import { useAuth } from '@/auth/auth-context';

vi.mock('@/services/api', () => ({
  api: {
    parking: { list: vi.fn(), create: vi.fn(), update: vi.fn() },
    apartments: { list: vi.fn() },
  },
  getApiErrorMessage: vi.fn(() => 'Erro detectado'),
}));

vi.mock('@/auth/auth-context', () => ({
  useAuth: vi.fn(),
}));

const parkingApi = vi.mocked(api.parking);
const apartmentsApi = vi.mocked(api.apartments);
const authMock = vi.mocked(useAuth);

beforeEach(() => {
  vi.clearAllMocks();
  authMock.mockReturnValue({ hasPermission: vi.fn().mockReturnValue(true), user: { role: 'ADMIN' }, logout: vi.fn(), isAuthenticated: true, login: vi.fn() } as never);
  parkingApi.list.mockResolvedValue([
    { id: 1, number: '101', apartmentId: 1, apartmentNumber: '10', type: 'ASSIGNED', status: 'AVAILABLE', active: true },
  ]);
  apartmentsApi.list.mockResolvedValue([{ id: 1, number: '10', active: true }]);
});

describe('ParkingPage', () => {
  it('renders title and spaces', async () => {
    render(<MemoryRouter><ParkingPage /></MemoryRouter>);
    await waitFor(() => expect(screen.getByText('Vagas')).toBeInTheDocument());
    expect(screen.getByText('101')).toBeInTheDocument();
  });

  it('shows empty state', async () => {
    parkingApi.list.mockResolvedValue([]);
    render(<MemoryRouter><ParkingPage /></MemoryRouter>);
    await waitFor(() => expect(screen.getByText('Nenhuma vaga')).toBeInTheDocument());
  });

  it('shows error on failure', async () => {
    parkingApi.list.mockRejectedValue(new Error('FAIL'));
    render(<MemoryRouter><ParkingPage /></MemoryRouter>);
    await waitFor(() => expect(screen.getByRole('alert')).toBeInTheDocument());
  });
});
