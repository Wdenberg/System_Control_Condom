import { describe, expect, it } from 'vitest';
import { SseParser } from '@/realtime/realtime-client';

describe('SseParser', () => {
  it('parses JSON events and keeps partial chunks until completed', () => {
    const parser = new SseParser();

    expect(parser.push('id: 1\nevent: movement.created\ndata: {"movementId":42,')).toEqual([]);
    expect(parser.push('"plate":"ABC1D23"}\n\n')).toEqual([
      {
        id: '1',
        type: 'movement.created',
        data: { movementId: 42, plate: 'ABC1D23' },
      },
    ]);
  });

  it('supports plain text SSE data', () => {
    const parser = new SseParser();
    expect(parser.push('event: heartbeat\ndata: UP\n\n')).toEqual([
      {
        id: expect.any(String),
        type: 'heartbeat',
        data: 'UP',
      },
    ]);
  });

  it('joins multiple data lines according to SSE rules', () => {
    const parser = new SseParser();
    const result = parser.push('event: message\ndata: {"a":1}\ndata: {"b":2}\n\n')[0];
    expect(result.type).toBe('message');
    expect(result.data).toBe('{"a":1}\n{"b":2}');
  });
});
