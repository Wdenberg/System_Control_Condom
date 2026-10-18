import Badge from './Badge';
import { getParkingStatusLabel } from '@/lib/presentation/parking-space';
import type { ParkingSpaceStatus } from '@/types/api';

const statusVariant: Record<ParkingSpaceStatus, 'success' | 'danger' | 'warning'> = {
  AVAILABLE: 'success',
  OCCUPIED: 'danger',
  BLOCKED: 'warning',
};

export default function ParkingStatusBadge({ status }: { status: ParkingSpaceStatus }) {
  return (
    <Badge variant={statusVariant[status]} aria-label={`Status: ${getParkingStatusLabel(status)}`}>
      {getParkingStatusLabel(status)}
    </Badge>
  );
}
