import { describe, expect, it, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import RealtimeStatus from '@/components/RealtimeStatus';

const useRealtimeMock = vi.hoisted(() => vi.fn());
vi.mock('@/realtime/realtime-context', () => ({
  useRealtime: useRealtimeMock,
}));

describe('RealtimeStatus', () => {
  it('shows connected status', () => {
    useRealtimeMock.mockReturnValue({ status: 'CONNECTED' });
    render(<RealtimeStatus />);
    expect(screen.getByText('Tempo real conectado')).toBeInTheDocument();
  });

  it('shows reconnecting status', () => {
    useRealtimeMock.mockReturnValue({ status: 'RECONNECTING' });
    render(<RealtimeStatus />);
    expect(screen.getByText('Reconectando tempo real...')).toBeInTheDocument();
  });
});
