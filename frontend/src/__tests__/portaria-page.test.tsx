import { render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import PortariaPage from '@/pages/PortariaPage';
import { api } from '@/services/api';
import { useAuth } from '@/auth/auth-context';
import { useRealtime } from '@/realtime/realtime-context';

vi.mock('@/services/api', () => ({
  api: {
    dashboard: { summary: vi.fn(), activity: vi.fn(), occupancy: vi.fn(), cameras: vi.fn(), alerts: vi.fn() },
    gates: { status: vi.fn(), test: vi.fn() },
    movements: { createManual: vi.fn() },
  },
  getApiErrorMessage: vi.fn(() => 'Erro detectado'),
}));

vi.mock('@/auth/auth-context', () => ({
  useAuth: vi.fn(),
}));

vi.mock('@/realtime/realtime-context', () => ({
  useRealtime: vi.fn(),
}));

const dashboardApi = vi.mocked(api.dashboard);
const gatesApi = vi.mocked(api.gates);
const authMock = vi.mocked(useAuth);
const realtimeMock = vi.mocked(useRealtime);

beforeEach(() => {
  vi.clearAllMocks();
  authMock.mockReturnValue({
    hasPermission: vi.fn().mockReturnValue(true),
    user: { id: 1, username: 'admin', fullName: 'Admin', role: 'ADMIN', permissions: [] },
    logout: vi.fn(), isAuthenticated: true, isLoading: false,
    login: vi.fn().mockResolvedValue(undefined), refreshMe: vi.fn().mockResolvedValue(undefined),
  });
  realtimeMock.mockReturnValue({ status: 'DISCONNECTED', lastEvent: null, events: [], connected: false });
  dashboardApi.summary.mockResolvedValue({ activeApartments: 10, activeResidents: 20, activeVehicles: 8, vehiclesInside: 3, vehiclesOutside: 5, totalParkingSpaces: 10, freeParkingSpaces: 7, occupiedParkingSpaces: 3, blockedParkingSpaces: 0, movementsToday: 5, entriesToday: 3, exitsToday: 2, authorizedToday: 4, deniedToday: 1, pendingGateOperations: 0, exhaustedGateOperations: 0, generatedAt: new Date().toISOString() });
  dashboardApi.activity.mockResolvedValue([]);
  dashboardApi.occupancy.mockResolvedValue([]);
  dashboardApi.cameras.mockResolvedValue([]);
  dashboardApi.alerts.mockResolvedValue([]);
  gatesApi.status.mockResolvedValue({ enabled: true, driver: 'SIMULATED', entryState: 'READY', exitState: 'READY', entryMessage: '', exitMessage: '', entryConfigured: true, exitConfigured: true, safetyMode: '' } as never);
});

describe('PortariaPage', () => {
  it('renders title', async () => {
    render(<MemoryRouter><PortariaPage /></MemoryRouter>);
    await waitFor(() => expect(screen.getByRole('heading', { name: /Portaria/ })).toBeInTheDocument());
  });

  it('shows KPIs', async () => {
    render(<MemoryRouter><PortariaPage /></MemoryRouter>);
    await waitFor(() => expect(screen.getByText(/dentro/)).toBeInTheDocument());
  });

  it('shows waiting state when no movements', async () => {
    render(<MemoryRouter><PortariaPage /></MemoryRouter>);
    await waitFor(() => expect(screen.getByText(/Aguardando leitura/)).toBeInTheDocument());
  });

  it('hides manual access and gate test forms without their permissions', async () => {
    authMock.mockReturnValue({ ...authMock(), hasPermission: (permission) => permission === 'MOVEMENT_READ' });
    render(<MemoryRouter><PortariaPage /></MemoryRouter>);
    await waitFor(() => expect(screen.getByText(/Aguardando leitura/)).toBeInTheDocument());
    expect(screen.queryByRole('textbox', { name: 'Placa' })).not.toBeInTheDocument();
    expect(screen.queryByRole('textbox', { name: 'Justificativa do teste' })).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /Testar cancela/i })).not.toBeInTheDocument();
    expect(gatesApi.test).not.toHaveBeenCalled();
    expect(api.movements.createManual).not.toHaveBeenCalled();
  });
});
