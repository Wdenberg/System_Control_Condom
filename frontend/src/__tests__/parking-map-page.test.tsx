import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import ParkingMapPage from '@/pages/ParkingMapPage';
import { api } from '@/services/api';

vi.mock('@/services/api', () => ({
  api: {
    dashboard: { occupancy: vi.fn() },
    apartments: { list: vi.fn() },
    vehicles: { list: vi.fn() },
  },
  getApiErrorMessage: vi.fn(() => 'Erro detectado'),
}));

const dashboardApi = vi.mocked(api.dashboard);
const vehiclesApi = vi.mocked(api.vehicles);

beforeEach(() => {
  vi.clearAllMocks();
  dashboardApi.occupancy.mockResolvedValue([
    { parkingSpaceId: 1, parkingSpaceNumber: '101', apartmentId: 1, apartmentNumber: '10', type: 'ASSIGNED', status: 'AVAILABLE', active: true, assignedPlate: null, vehicleInside: false },
    { parkingSpaceId: 2, parkingSpaceNumber: '102', apartmentId: 1, apartmentNumber: '10', type: 'ASSIGNED', status: 'OCCUPIED', active: true, assignedPlate: 'ABC1D23', vehicleInside: true },
    { parkingSpaceId: 3, parkingSpaceNumber: '103', apartmentId: 2, apartmentNumber: '20', type: 'VISITOR', status: 'BLOCKED', active: true, assignedPlate: null, vehicleInside: false },
  ]);
  vehiclesApi.list.mockResolvedValue([
    { id: 1, plate: 'ABC1D23', brand: 'Toyota', model: 'Corolla', color: 'Prata', parkingSpaceId: 2, parkingSpaceNumber: '102', apartmentId: 1, apartmentNumber: '10', residents: [{ id: 1, fullName: 'João Silva' }], active: true },
  ]);
});

describe('ParkingMapPage', () => {
  it('renders title and parking spaces', async () => {
    render(<MemoryRouter><ParkingMapPage /></MemoryRouter>);
    await waitFor(() => expect(screen.getByText('Mapa de Vagas')).toBeInTheDocument());
    expect(screen.getByText('101')).toBeInTheDocument();
    expect(screen.getByText('102')).toBeInTheDocument();
    expect(screen.getByText('103')).toBeInTheDocument();
  });

  it('displays resident name for occupied space', async () => {
    render(<MemoryRouter><ParkingMapPage /></MemoryRouter>);
    await waitFor(() => expect(screen.getByText('João Silva')).toBeInTheDocument());
  });

  it('filters by search query', async () => {
    const user = userEvent.setup();
    render(<MemoryRouter><ParkingMapPage /></MemoryRouter>);
    await waitFor(() => expect(screen.getByText('101')).toBeInTheDocument());

    await user.type(screen.getByLabelText('Buscar'), '102');
    expect(screen.queryByText('101')).not.toBeInTheDocument();
    expect(screen.getByText('102')).toBeInTheDocument();
  });

  it('filters by status', async () => {
    const user = userEvent.setup();
    render(<MemoryRouter><ParkingMapPage /></MemoryRouter>);
    await waitFor(() => expect(screen.getByText('101')).toBeInTheDocument());

    await user.selectOptions(screen.getByLabelText('Filtrar por status'), 'OCCUPIED');
    expect(screen.queryByText('101')).not.toBeInTheDocument();
    expect(screen.getByText('102')).toBeInTheDocument();
    expect(screen.queryByText('103')).not.toBeInTheDocument();
  });

  it('filters by type', async () => {
    const user = userEvent.setup();
    render(<MemoryRouter><ParkingMapPage /></MemoryRouter>);
    await waitFor(() => expect(screen.getByText('101')).toBeInTheDocument());

    await user.selectOptions(screen.getByLabelText('Filtrar por tipo'), 'VISITOR');
    expect(screen.queryByText('101')).not.toBeInTheDocument();
    expect(screen.queryByText('102')).not.toBeInTheDocument();
    expect(screen.getByText('103')).toBeInTheDocument();
  });

  it('clears filters when clear button is clicked', async () => {
    const user = userEvent.setup();
    render(<MemoryRouter><ParkingMapPage /></MemoryRouter>);
    await waitFor(() => expect(screen.getByText('101')).toBeInTheDocument());

    await user.selectOptions(screen.getByLabelText('Filtrar por status'), 'OCCUPIED');
    expect(screen.queryByText('101')).not.toBeInTheDocument();

    await user.click(screen.getByText('Limpar filtros'));
    expect(screen.getByText('101')).toBeInTheDocument();
    expect(screen.getByText('102')).toBeInTheDocument();
  });

  it('shows empty state when no results match filters', async () => {
    const user = userEvent.setup();
    render(<MemoryRouter><ParkingMapPage /></MemoryRouter>);
    await waitFor(() => expect(screen.getByText('101')).toBeInTheDocument());

    await user.type(screen.getByLabelText('Buscar'), '999');
    expect(await screen.findByText('Nenhuma vaga encontrada')).toBeInTheDocument();
  });

  it('shows error on failure', async () => {
    dashboardApi.occupancy.mockRejectedValue(new Error('FAIL'));
    render(<MemoryRouter><ParkingMapPage /></MemoryRouter>);
    await waitFor(() => expect(screen.getByRole('alert')).toBeInTheDocument());
  });
});
