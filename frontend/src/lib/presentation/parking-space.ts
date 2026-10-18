import type { ParkingSpaceType, ParkingSpaceStatus } from '@/types/api';

export const parkingSpaceTypeLabels: Record<ParkingSpaceType, string> = {
  ASSIGNED: 'Própria',
  RENTED: 'Alugada',
  EXTRA: 'Extra',
  VISITOR: 'Visitante',
};

export const parkingStatusLabels: Record<ParkingSpaceStatus, string> = {
  AVAILABLE: 'Disponível',
  OCCUPIED: 'Ocupada',
  BLOCKED: 'Bloqueada',
};

export const parkingStatusClasses: Record<ParkingSpaceStatus, { box: string; card: string }> = {
  AVAILABLE: { box: 'available', card: 'available' },
  OCCUPIED: { box: 'occupied', card: 'occupied' },
  BLOCKED: { box: 'blocked', card: 'blocked' },
};

export const parkingStatusFilterOptions = [
  { value: 'ALL', label: 'Todos os status' },
  { value: 'AVAILABLE', label: 'Disponível' },
  { value: 'OCCUPIED', label: 'Ocupada' },
  { value: 'BLOCKED', label: 'Bloqueada' },
] as const;

export const parkingTypeFilterOptions = [
  { value: 'ALL', label: 'Todos os tipos' },
  { value: 'ASSIGNED', label: 'Própria' },
  { value: 'RENTED', label: 'Alugada' },
  { value: 'EXTRA', label: 'Extra' },
  { value: 'VISITOR', label: 'Visitante' },
] as const;

export function getParkingTypeLabel(type: ParkingSpaceType): string {
  return parkingSpaceTypeLabels[type] ?? type;
}

export function getParkingStatusLabel(status: ParkingSpaceStatus): string {
  return parkingStatusLabels[status] ?? status;
}

export function getParkingStatusClass(status: ParkingSpaceStatus): string {
  return parkingStatusClasses[status]?.box ?? '';
}

export function getParkingCardClass(status: ParkingSpaceStatus): string {
  return parkingStatusClasses[status]?.card ?? '';
}
