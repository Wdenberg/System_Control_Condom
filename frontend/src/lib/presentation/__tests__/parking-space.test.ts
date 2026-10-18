import { describe, expect, it } from 'vitest';
import {
  parkingSpaceTypeLabels,
  parkingStatusLabels,
  getParkingTypeLabel,
  getParkingStatusLabel,
  getParkingStatusClass,
  getParkingCardClass,
} from '@/lib/presentation/parking-space';

describe('parking-space presentation', () => {
  it('parkingSpaceTypeLabels maps all types to pt-BR', () => {
    expect(parkingSpaceTypeLabels.ASSIGNED).toBe('Própria');
    expect(parkingSpaceTypeLabels.RENTED).toBe('Alugada');
    expect(parkingSpaceTypeLabels.EXTRA).toBe('Extra');
    expect(parkingSpaceTypeLabels.VISITOR).toBe('Visitante');
  });

  it('parkingStatusLabels maps all statuses to pt-BR', () => {
    expect(parkingStatusLabels.AVAILABLE).toBe('Disponível');
    expect(parkingStatusLabels.OCCUPIED).toBe('Ocupada');
    expect(parkingStatusLabels.BLOCKED).toBe('Bloqueada');
  });

  it('getParkingTypeLabel returns correct label', () => {
    expect(getParkingTypeLabel('ASSIGNED')).toBe('Própria');
    expect(getParkingTypeLabel('VISITOR')).toBe('Visitante');
  });

  it('getParkingTypeLabel falls back to raw value for unknown type', () => {
    expect(getParkingTypeLabel('UNKNOWN' as any)).toBe('UNKNOWN');
  });

  it('getParkingStatusLabel returns correct label', () => {
    expect(getParkingStatusLabel('AVAILABLE')).toBe('Disponível');
    expect(getParkingStatusLabel('BLOCKED')).toBe('Bloqueada');
  });

  it('getParkingStatusLabel falls back to raw value', () => {
    expect(getParkingStatusLabel('X' as any)).toBe('X');
  });

  it('getParkingStatusClass returns box class', () => {
    expect(getParkingStatusClass('AVAILABLE')).toBe('available');
    expect(getParkingStatusClass('OCCUPIED')).toBe('occupied');
    expect(getParkingStatusClass('BLOCKED')).toBe('blocked');
  });

  it('getParkingStatusClass falls back to empty string', () => {
    expect(getParkingStatusClass('X' as any)).toBe('');
  });

  it('getParkingCardClass returns card class', () => {
    expect(getParkingCardClass('AVAILABLE')).toBe('available');
    expect(getParkingCardClass('OCCUPIED')).toBe('occupied');
  });

  it('getParkingCardClass falls back to empty string', () => {
    expect(getParkingCardClass('X' as any)).toBe('');
  });
});
