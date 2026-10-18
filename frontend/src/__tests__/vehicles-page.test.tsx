import { render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import VehiclesPage from '@/pages/VehiclesPage';
import { api } from '@/services/api';
import { useAuth } from '@/auth/auth-context';

vi.mock('@/services/api', () => ({
  api: {
    vehicles: { list: vi.fn(), create: vi.fn(), update: vi.fn() },
    apartments: { list: vi.fn(), residents: vi.fn() },
    parking: { list: vi.fn() },
  },
  getApiErrorMessage: vi.fn(() => 'Erro detectado'),
}));

vi.mock('@/auth/auth-context', () => ({
  useAuth: vi.fn(),
}));

const vehiclesApi = vi.mocked(api.vehicles);
const authMock = vi.mocked(useAuth);

beforeEach(() => {
  vi.clearAllMocks();
  authMock.mockReturnValue({
    hasPermission: vi.fn().mockReturnValue(true),
    user: { id: 1, username: 'admin', fullName: 'Admin', role: 'ADMIN', permissions: [] },
    logout: vi.fn(), isAuthenticated: true, isLoading: false,
    login: vi.fn().mockResolvedValue(undefined), refreshMe: vi.fn().mockResolvedValue(undefined),
  });
  vehiclesApi.list.mockResolvedValue([
    { id: 1, plate: 'ABC1D23', brand: 'Fiat', model: 'Uno', color: 'Branco', active: true, apartmentNumber: '10', parkingSpaceNumber: '5', residents: [{ id: 1, fullName: 'Joao' }], apartmentId: 1, parkingSpaceId: 5 },
  ]);
  vi.mocked(api.apartments.list).mockResolvedValue([]);
  vi.mocked(api.parking.list).mockResolvedValue([]);
});

describe('VehiclesPage', () => {
  it('renders title', async () => {
    render(<MemoryRouter><VehiclesPage /></MemoryRouter>);
    await waitFor(() => expect(screen.getByRole('heading', { name: /Ve/ })).toBeInTheDocument());
  });

  it('shows vehicle plate', async () => {
    render(<MemoryRouter><VehiclesPage /></MemoryRouter>);
    await waitFor(() => expect(screen.getByText('ABC1D23')).toBeInTheDocument());
  });

  it('shows empty state', async () => {
    vehiclesApi.list.mockResolvedValue([]);
    render(<MemoryRouter><VehiclesPage /></MemoryRouter>);
    await waitFor(() => expect(screen.getByText(/Nenhum/)).toBeInTheDocument());
  });

  it('shows error on failure', async () => {
    vehiclesApi.list.mockRejectedValue(new Error('FAIL'));
    render(<MemoryRouter><VehiclesPage /></MemoryRouter>);
    await waitFor(() => expect(screen.getByRole('alert')).toBeInTheDocument());
  });

  it('hides creation and editing actions without VEHICLE_WRITE', async () => {
    authMock.mockReturnValue({ ...authMock(), hasPermission: (permission) => permission === 'VEHICLE_READ' });
    render(<MemoryRouter><VehiclesPage /></MemoryRouter>);
    expect(await screen.findByText('ABC1D23')).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /Novo veículo/i })).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /Editar/i })).not.toBeInTheDocument();
    expect(vehiclesApi.create).not.toHaveBeenCalled();
    expect(vehiclesApi.update).not.toHaveBeenCalled();
  });
});
