import { render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import DashboardPage from '@/pages/DashboardPage';
import { api } from '@/services/api';
import { useRealtime } from '@/realtime/realtime-context';

vi.mock('@/services/api', () => ({
  api: {
    dashboard: {
      summary: vi.fn(),
      activity: vi.fn(),
      occupancy: vi.fn(),
      cameras: vi.fn(),
      alerts: vi.fn(),
    },
  },
  getApiErrorMessage: (error: unknown) => error instanceof Error ? error.message : 'Falha ao carregar a operação.',
}));

const dashboardApi = vi.mocked(api.dashboard);

vi.mock('@/realtime/realtime-context', () => ({
  useRealtime: vi.fn(),
}));

const realtimeMock = vi.mocked(useRealtime);

beforeEach(() => {
  vi.clearAllMocks();
  realtimeMock.mockReturnValue({ status: 'DISCONNECTED', lastEvent: null, events: [], connected: false });
  dashboardApi.summary.mockResolvedValue({ activeApartments: 20, activeResidents: 30, activeVehicles: 12, vehiclesInside: 5, vehiclesOutside: 7, totalParkingSpaces: 20, freeParkingSpaces: 15, occupiedParkingSpaces: 5, blockedParkingSpaces: 0, movementsToday: 10, entriesToday: 5, exitsToday: 5, authorizedToday: 9, deniedToday: 1, pendingGateOperations: 0, exhaustedGateOperations: 0, generatedAt: new Date().toISOString() });
  dashboardApi.activity.mockResolvedValue([]);
  dashboardApi.occupancy.mockResolvedValue([]);
  dashboardApi.cameras.mockResolvedValue([]);
  dashboardApi.alerts.mockResolvedValue([]);
});

describe('DashboardPage', () => {
  it('renders KPIs from backend', async () => {
    render(<MemoryRouter><DashboardPage /></MemoryRouter>);
    await waitFor(() => expect(screen.getByText('Vagas livres')).toBeInTheDocument());
    expect(screen.getByText('15')).toBeInTheDocument();
    expect(screen.getByText('Veículos dentro')).toBeInTheDocument();
    expect(screen.getAllByText('5')).toHaveLength(2);
  });
});
