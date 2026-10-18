import { render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import MovementsPage from '@/pages/MovementsPage';
import { api } from '@/services/api';

vi.mock('@/services/api', () => ({
  api: {
    movements: { list: vi.fn(), confirmPassage: vi.fn() },
  },
  getApiErrorMessage: vi.fn(() => 'Erro detectado'),
}));

vi.mock('@/auth/auth-context', () => ({ useAuth: () => ({ hasPermission: () => false }) }));

const movementsApi = vi.mocked(api.movements);

beforeEach(() => {
  vi.clearAllMocks();
  movementsApi.list.mockResolvedValue({
    content: [
      { movementId: 1, plate: 'ABC1D23', direction: 'ENTRY', eventTime: '2026-09-17T10:00:00', decision: 'AUTHORIZED', status: 'AUTHORIZED', vehicleOwner: 'Joao', apartmentNumber: '101', parkingSpaceNumber: '5', vehicleId: 1, parkingSpaceId: 5 },
    ],
    totalPages: 1, number: 0, size: 30, totalElements: 1, first: true, last: true,
  });
});

describe('MovementsPage', () => {
  it('renders title and table', async () => {
    render(<MemoryRouter><MovementsPage /></MemoryRouter>);
    await waitFor(() => expect(screen.getByText(/Movimenta..es/)).toBeInTheDocument());
    expect(screen.getByText('ABC1D23')).toBeInTheDocument();
    expect(screen.getByText('Joao')).toBeInTheDocument();
  });

  it('shows empty state', async () => {
    movementsApi.list.mockResolvedValue({ content: [], totalPages: 0, number: 0, size: 30, totalElements: 0, first: true, last: true });
    render(<MemoryRouter><MovementsPage /></MemoryRouter>);
    await waitFor(() => expect(screen.getByText(/Nenhuma movimenta..o/)).toBeInTheDocument());
  });

  it('shows error on failure', async () => {
    movementsApi.list.mockRejectedValue(new Error('FAIL'));
    render(<MemoryRouter><MovementsPage /></MemoryRouter>);
    await waitFor(() => expect(screen.getByRole('alert')).toBeInTheDocument());
  });
});
