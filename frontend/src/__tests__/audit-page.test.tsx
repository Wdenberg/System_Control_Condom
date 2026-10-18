import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router';
import AuditPage from '@/pages/AuditPage';
import { api } from '@/services/api';

vi.mock('@/services/api', async () => {
  const actual = await vi.importActual<typeof import('@/services/api')>('@/services/api');
  return {
    ...actual,
    api: {
      ...actual.api,
      audit: {
        list: vi.fn(),
        csv: vi.fn(),
      },
    },
  };
});

describe('AuditPage', () => {
  it('renders filtered audit results', async () => {
    vi.mocked(api.audit.list).mockResolvedValue({
      content: [{
        id: 1, userId: 7, username: 'admin', action: 'MOVEMENT_OVERRIDE', entityType: 'MOVEMENT', entityId: '42',
        justification: 'Autorizado pela portaria', details: 'plate=SOO3H61', outcome: 'SUCCESS', ipAddress: '192.168.15.10',
        userAgent: 'Vitest', correlationId: 'corr-1', requestMethod: 'POST', requestPath: '/api/v1/movements/42/override', createdAt: '2026-09-13T12:00:00Z',
      }],
      number: 0, size: 30, totalElements: 1, totalPages: 1, first: true, last: true,
    });

    render(<MemoryRouter><AuditPage /></MemoryRouter>);

    await waitFor(() => expect(screen.getByText('MOVEMENT_OVERRIDE')).toBeInTheDocument());
    expect(screen.getByText('admin')).toBeInTheDocument();
    expect(screen.getByText('192.168.15.10')).toBeInTheDocument();
    expect(screen.getByText('/api/v1/movements/42/override')).toBeInTheDocument();

    await userEvent.type(screen.getByPlaceholderText('MOVEMENT_OVERRIDE'), 'GATE_TEST');
    await waitFor(() => expect(api.audit.list).toHaveBeenLastCalledWith(expect.objectContaining({ action: 'GATE_TEST' }), 0, 30));
  });
});
