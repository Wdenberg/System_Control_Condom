import type { ReactNode } from 'react';
import { labels } from '@/lib/presentation/labels';

export default function FilterBar({ children, count, countLabel = labels.filterBar.defaultCountLabel }: { children: ReactNode; count?: number; countLabel?: string }) {
  return (
    <div className="toolbar">
      {children}
      {count !== undefined && <span>{count} {countLabel}</span>}
    </div>
  );
}
