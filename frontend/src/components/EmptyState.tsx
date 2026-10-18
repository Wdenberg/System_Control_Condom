import { Inbox } from 'lucide-react';

export default function EmptyState({ title, description }: { title: string; description: string }) {
  return (
    <div className="empty-state" role="status" aria-live="polite">
      <div className="empty-icon"><Inbox className="h-5 w-5" aria-hidden="true" /></div>
      <strong>{title}</strong>
      <span>{description}</span>
    </div>
  );
}
