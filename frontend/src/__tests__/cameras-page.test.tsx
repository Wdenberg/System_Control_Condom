import { render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import CamerasPage from '@/pages/CamerasPage';
import { api } from '@/services/api';

vi.mock('@/services/api', () => ({
  api: {
    cameras: { list: vi.fn() },
  },
  getApiErrorMessage: vi.fn(() => 'Erro detectado'),
}));

const camerasApi = vi.mocked(api.cameras);

beforeEach(() => {
  vi.clearAllMocks();
  camerasApi.list.mockResolvedValue([
    { id: 1, name: 'Entrada Principal', direction: 'ENTRY', ipAddress: '192.168.1.10', model: 'ANPR-200', channelId: 1, vendor: 'Hikvision', enabled: true },
  ]);
});

describe('CamerasPage', () => {
  it('renders title', async () => {
    render(<MemoryRouter><CamerasPage /></MemoryRouter>);
    await waitFor(() => expect(screen.getByRole('heading', { name: /C.meras/ })).toBeInTheDocument());
  });

  it('renders camera name', async () => {
    render(<MemoryRouter><CamerasPage /></MemoryRouter>);
    await waitFor(() => expect(screen.getByText('Entrada Principal')).toBeInTheDocument());
  });

  it('shows online status', async () => {
    render(<MemoryRouter><CamerasPage /></MemoryRouter>);
    await waitFor(() => expect(screen.getByText('CONECTADA')).toBeInTheDocument());
  });

  it('shows empty state', async () => {
    camerasApi.list.mockResolvedValue([]);
    render(<MemoryRouter><CamerasPage /></MemoryRouter>);
    await waitFor(() => expect(screen.getByText(/Nenhuma c.mera/)).toBeInTheDocument());
  });

  it('shows error on failure', async () => {
    camerasApi.list.mockRejectedValue(new Error('FAIL'));
    render(<MemoryRouter><CamerasPage /></MemoryRouter>);
    await waitFor(() => expect(screen.getByRole('alert')).toBeInTheDocument());
  });
});
