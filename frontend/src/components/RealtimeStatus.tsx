import { useRealtime } from '@/realtime/realtime-context';
import { labels } from '@/lib/presentation/labels';

const realtimeLabels = {
  CONNECTED: labels.realtime.connected,
  CONNECTING: labels.realtime.connecting,
  RECONNECTING: labels.realtime.reconnecting,
  DISCONNECTED: labels.realtime.disconnected,
} as const;

export default function RealtimeStatus() {
  const { status } = useRealtime();
  return (
    <div className={`realtime-status ${status.toLowerCase()}`} title={realtimeLabels[status]}>
      <span aria-hidden="true" />
      <span>{realtimeLabels[status]}</span>
    </div>
  );
}
