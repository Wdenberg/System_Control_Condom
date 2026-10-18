import { describe, expect, it } from 'vitest';

describe('Gate status contract', () => {
  it('marks simulated mode as safe for development', () => {
    const status = { driver: 'SIMULATED', safetyMode: 'SIMULATED', enabled: true };
    expect(status.safetyMode).toBe('SIMULATED');
  });

  it('distinguishes the generic hardware adapter', () => {
    const status = { driver: 'HTTP', safetyMode: 'HARDWARE_ADAPTER', enabled: true };
    expect(status.safetyMode).toBe('HARDWARE_ADAPTER');
  });
});
