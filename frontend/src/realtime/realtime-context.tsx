import { createContext, useContext, useEffect, useMemo, useRef, useState, type ReactNode } from 'react';
import { useAuth } from '@/auth/auth-context';
import { consumeRealtimeStream, type RealtimeMessage, type RealtimeStatus } from './realtime-client';

interface RealtimeContextValue {
  status: RealtimeStatus;
  lastEvent: RealtimeMessage | null;
  events: RealtimeMessage[];
  connected: boolean;
}

const RealtimeContext = createContext<RealtimeContextValue | undefined>(undefined);

const MAX_EVENTS = 50;

export function RealtimeProvider({ children }: { children: ReactNode }) {
  const { isAuthenticated } = useAuth();
  const [status, setStatus] = useState<RealtimeStatus>('DISCONNECTED');
  const [events, setEvents] = useState<RealtimeMessage[]>([]);
  const retryTimer = useRef<number | null>(null);

  useEffect(() => {
    if (!isAuthenticated) {
      setStatus('DISCONNECTED');
      setEvents([]);
      return;
    }

    let stopped = false;
    let controller: AbortController | null = null;
    let retry = 1000;

    const connect = async () => {
      if (stopped) return;
      controller = new AbortController();
      try {
        await consumeRealtimeStream(
          controller.signal,
          (event) => {
            setEvents((current) => [event, ...current].slice(0, MAX_EVENTS));
          },
          setStatus,
        );
        if (stopped) return;
        setStatus('RECONNECTING');
      } catch (error) {
        if (stopped || (error instanceof Error && error.message === 'UNAUTHORIZED')) return;
        setStatus('RECONNECTING');
      }

      if (stopped) return;
      const delay = retry;
      retry = Math.min(retry * 2, 15000);
      retryTimer.current = window.setTimeout(connect, delay);
    };

    void connect();

    return () => {
      stopped = true;
      controller?.abort();
      if (retryTimer.current) window.clearTimeout(retryTimer.current);
    };
  }, [isAuthenticated]);

  const lastEvent = events[0] ?? null;
  const value = useMemo(() => ({ status, lastEvent, events, connected: status === 'CONNECTED' }), [events, lastEvent, status]);
  return <RealtimeContext.Provider value={value}>{children}</RealtimeContext.Provider>;
}

export function useRealtime(): RealtimeContextValue {
  const context = useContext(RealtimeContext);
  if (!context) throw new Error('useRealtime must be used inside RealtimeProvider');
  return context;
}
