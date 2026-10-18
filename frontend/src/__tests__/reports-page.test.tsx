import { beforeEach, describe, expect, it, vi } from 'vitest';
import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router';
import ReportsPage from '@/pages/ReportsPage';

const { reportsMock } = vi.hoisted(() => ({
  reportsMock: {
    access: vi.fn(),
    accessSummary: vi.fn(),
    exportAccessCsv: vi.fn(),
    occupancy: vi.fn(),
    gates: vi.fn(),
  },
}));

vi.mock('@/services/api', () => ({
  api: {
    reports: reportsMock,
  },
  getApiErrorMessage: (
    error: unknown,
    fallback: string,
  ) => (error instanceof Error ? error.message : fallback),
}));

describe('ReportsPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();

    reportsMock.access.mockResolvedValue({
      content: [],
      number: 0,
      size: 30,
      totalElements: 0,
      totalPages: 0,
      first: true,
      last: true,
    });

    reportsMock.accessSummary.mockResolvedValue({
      from: '2026-09-13T00:00:00-03:00',
      to: '2026-09-13T23:59:59-03:00',
      total: 2,
      entries: 1,
      exits: 1,
      authorized: 1,
      denied: 1,
      overridden: 0,
      decisions: {
        UNKNOWN_VEHICLE: 1,
      },
    });

    reportsMock.occupancy.mockResolvedValue([]);

    reportsMock.gates.mockResolvedValue({
      content: [],
      number: 0,
      size: 30,
      totalElements: 0,
      totalPages: 0,
      first: true,
      last: true,
    });

    reportsMock.exportAccessCsv.mockResolvedValue(
      new Blob(['id;placa\n1;ABC1D23']),
    );
  });

  it('renders report summary and access filters', async () => {
    render(
      <MemoryRouter>
        <ReportsPage />
      </MemoryRouter>,
    );

    await waitFor(() => {
      expect(reportsMock.access).toHaveBeenCalled();
    });

    expect(screen.getByText('Relatório de acessos')).toBeInTheDocument();
    expect(screen.getByText('Autorizados')).toBeInTheDocument();
    expect(screen.getByText('Negados')).toBeInTheDocument();
    expect(screen.getByPlaceholderText('ABC1D23')).toBeInTheDocument();
  });

  it('switches to occupancy report', async () => {
    render(
      <MemoryRouter>
        <ReportsPage />
      </MemoryRouter>,
    );

    fireEvent.click(
      screen.getByRole('tab', { name: 'Ocupação' }),
    );

    await waitFor(() => {
      expect(reportsMock.occupancy).toHaveBeenCalled();
    });

    expect(screen.getByText('Captura de ocupação')).toBeInTheDocument();
  });

  it('loads gate report after switching tab', async () => {
    render(
      <MemoryRouter>
        <ReportsPage />
      </MemoryRouter>,
    );

    fireEvent.click(
      screen.getByRole('tab', { name: 'Cancela' }),
    );

    await waitFor(() => {
      expect(reportsMock.gates).toHaveBeenCalled();
    });

    expect(screen.getByText('Operações de cancela')).toBeInTheDocument();
  });
});