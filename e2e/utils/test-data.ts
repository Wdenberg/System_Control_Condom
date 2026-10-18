import { randomUUID } from 'node:crypto';

export function uniqueSuffix(): string {
  return randomUUID().replaceAll('-', '').slice(0, 8).toUpperCase();
}

export function uniquePlate(): string {
  const suffix = randomUUID().replaceAll('-', '').toUpperCase();
  return `E2E${suffix.slice(0, 4)}`;
}

export function uniqueApartment(): string {
  const suffix = Date.now().toString().slice(-4);
  return `E2E-${suffix}`;
}

export function e2eUserPassword(): string {
  return 'E2E@GarageControl#2026';
}
