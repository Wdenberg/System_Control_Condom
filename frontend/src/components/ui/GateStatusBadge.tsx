import Badge from './Badge';
import { getGateOperationStatusLabel, getGateOperationStatusClass } from '@/lib/presentation/gate';

export default function GateStatusBadge({ status }: { status: string }) {
  const variant = getGateOperationStatusClass(status) as 'authorized' | 'denied' | 'pending' | 'overridden';
  const badgeVariant = variant === 'authorized' ? 'success' : variant === 'denied' ? 'danger' : variant === 'overridden' ? 'info' : 'warning';

  return (
    <Badge variant={badgeVariant} aria-label={`Situação: ${getGateOperationStatusLabel(status)}`}>
      {getGateOperationStatusLabel(status)}
    </Badge>
  );
}
