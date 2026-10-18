import Badge from './Badge';
import { getMovementDirectionLabel } from '@/lib/presentation/movement';
import type { MovementDirection } from '@/types/api';
import { LogIn, LogOut } from 'lucide-react';

const directionConfig: Record<MovementDirection, { variant: 'entry' | 'exit'; icon: typeof LogIn }> = {
  ENTRY: { variant: 'entry', icon: LogIn },
  EXIT: { variant: 'exit', icon: LogOut },
};

export default function MovementDirectionBadge({ direction }: { direction: MovementDirection }) {
  const { variant, icon: Icon } = directionConfig[direction];
  return (
    <Badge variant={variant} icon={<Icon className="h-3 w-3" />} aria-label={`Direção: ${getMovementDirectionLabel(direction)}`}>
      {getMovementDirectionLabel(direction)}
    </Badge>
  );
}
