import Badge from './Badge';
import { getAccessDecisionLabel, getMovementStatusClass } from '@/lib/presentation/movement';

export default function MovementDecisionBadge({ decision, status }: { decision: string; status?: string }) {
  const variant = getMovementStatusClass(status ?? decision) as 'authorized' | 'denied' | 'pending' | 'overridden';
  const badgeVariant = variant === 'authorized' ? 'success' : variant === 'denied' ? 'danger' : variant === 'overridden' ? 'info' : 'warning';

  return (
    <Badge variant={badgeVariant} aria-label={`Decisão: ${getAccessDecisionLabel(decision)}`}>
      {getAccessDecisionLabel(decision)}
    </Badge>
  );
}
