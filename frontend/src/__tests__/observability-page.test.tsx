import { render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router';
import { describe, expect, it, vi, beforeEach } from 'vitest';
import ObservabilityPage from '@/pages/ObservabilityPage';
import { api } from '@/services/api';

vi.mock('@/services/api', async () => {
  const actual = await vi.importActual<typeof import('@/services/api')>('@/services/api');
  return {
    ...actual,
    api: {
      ...actual.api,
      system: {
        observability: vi.fn(),
      },
    },
  };
});

describe('ObservabilityPage', () => {
  beforeEach(() => vi.clearAllMocks());

  it('renders operational status and realtime clients', async () => {
    vi.mocked(api.system.observability).mockResolvedValue({
      application: 'garage-control-backend',
      status: 'UP',
      metricsEndpoint: '/actuator/prometheus',
      healthEndpoint: '/actuator/health',
      realtimeConnectedClients: 3,
    });

    render(<MemoryRouter><ObservabilityPage /></MemoryRouter>);

    await waitFor(() => expect(screen.getByText('garage-control-backend')).toBeInTheDocument());
    expect(screen.getByText('UP')).toBeInTheDocument();
    expect(screen.getByText('3')).toBeInTheDocument();
    expect(screen.getByText('Prometheus')).toBeInTheDocument();
    expect(screen.getByText('Grafana')).toBeInTheDocument();
  });
});
