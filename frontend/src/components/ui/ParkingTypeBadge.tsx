import Badge from './Badge';
import { getParkingTypeLabel } from '@/lib/presentation/parking-space';
import type { ParkingSpaceType } from '@/types/api';

const typeVariant: Record<ParkingSpaceType, 'info' | 'neutral' | 'warning' | 'exit'> = {
  ASSIGNED: 'info',
  RENTED: 'exit',
  EXTRA: 'warning',
  VISITOR: 'neutral',
};

export default function ParkingTypeBadge({ type }: { type: ParkingSpaceType }) {
  return (
    <Badge variant={typeVariant[type]} aria-label={`Tipo: ${getParkingTypeLabel(type)}`}>
      {getParkingTypeLabel(type)}
    </Badge>
  );
}
