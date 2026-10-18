import Badge from './Badge';
import { getActiveLabel } from '@/lib/presentation/common';

export default function ActiveStatusBadge({ active }: { active: boolean }) {
  return (
    <Badge variant={active ? 'success' : 'danger'} aria-label={`Status: ${getActiveLabel(active)}`}>
      {getActiveLabel(active)}
    </Badge>
  );
}
