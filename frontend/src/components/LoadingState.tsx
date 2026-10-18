import { labels } from '@/lib/presentation/labels';

export default function LoadingState({ label = labels.common.loading }: { label?: string }) {
  return <div className="loading-state" role="status" aria-live="polite"><span className="spinner" /> {label}</div>;
}
