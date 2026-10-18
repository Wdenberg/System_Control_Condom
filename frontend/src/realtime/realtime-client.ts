import { clearSession, readSession } from '@/auth/auth-storage';
import { env } from '@/config/env';

export interface RealtimeMessage {
  id: string;
  type: string;
  data: unknown;
}

export type RealtimeStatus = 'DISCONNECTED' | 'CONNECTING' | 'CONNECTED' | 'RECONNECTING';

export class SseParser {
  private buffer = '';
  private eventName = 'message';
  private eventId = '';
  private dataLines: string[] = [];

  push(chunk: string): RealtimeMessage[] {
    this.buffer += chunk.replace(/\r\n/g, '\n').replace(/\r/g, '\n');
    const messages: RealtimeMessage[] = [];

    let separatorIndex = this.buffer.indexOf('\n\n');
    while (separatorIndex >= 0) {
      const block = this.buffer.slice(0, separatorIndex);
      this.buffer = this.buffer.slice(separatorIndex + 2);
      const message = this.parseBlock(block);
      if (message) messages.push(message);
      separatorIndex = this.buffer.indexOf('\n\n');
    }

    return messages;
  }

  finish(): RealtimeMessage[] {
    if (!this.buffer.trim()) return [];
    const message = this.parseBlock(this.buffer);
    this.buffer = '';
    return message ? [message] : [];
  }

  private parseBlock(block: string): RealtimeMessage | null {
    const lines = block.split('\n');
    for (const line of lines) {
      if (!line || line.startsWith(':')) continue;
      const colon = line.indexOf(':');
      const field = colon === -1 ? line : line.slice(0, colon);
      let value = colon === -1 ? '' : line.slice(colon + 1);
      if (value.startsWith(' ')) value = value.slice(1);
      if (field === 'event') this.eventName = value || 'message';
      else if (field === 'id') this.eventId = value;
      else if (field === 'data') this.dataLines.push(value);
    }

    if (this.dataLines.length === 0) {
      this.resetEvent();
      return null;
    }

    const rawData = this.dataLines.join('\n');
    let data: unknown = rawData;
    try {
      data = JSON.parse(rawData);
    } catch {
      // SSE also supports plain text payloads.
    }
    const message: RealtimeMessage = { id: this.eventId || crypto.randomUUID(), type: this.eventName, data };
    this.resetEvent();
    return message;
  }

  private resetEvent() {
    this.eventName = 'message';
    this.eventId = '';
    this.dataLines = [];
  }
}

export async function consumeRealtimeStream(
  signal: AbortSignal,
  onEvent: (event: RealtimeMessage) => void,
  onStatus: (status: RealtimeStatus) => void,
): Promise<void> {
  const session = readSession();
  if (!session?.accessToken) return;

  onStatus('CONNECTING');
  const response = await fetch(`${env.apiBaseUrl}/v1/realtime/events`, {
    method: 'GET',
    headers: {
      Accept: 'text/event-stream',
      Authorization: `Bearer ${session.accessToken}`,
      'Cache-Control': 'no-cache',
    },
    signal,
  });

  if (response.status === 401) {
    clearSession();
    window.dispatchEvent(new Event('auth:unauthorized'));
    throw new Error('UNAUTHORIZED');
  }
  if (!response.ok || !response.body) {
    throw new Error(`Realtime connection failed with status ${response.status}`);
  }

  onStatus('CONNECTED');
  const reader = response.body.getReader();
  const decoder = new TextDecoder();
  const parser = new SseParser();

  try {
    while (!signal.aborted) {
      const { value, done } = await reader.read();
      if (done) break;
      const chunk = decoder.decode(value, { stream: true });
      for (const event of parser.push(chunk)) onEvent(event);
    }
    for (const event of parser.push(decoder.decode())) onEvent(event);
    for (const event of parser.finish()) onEvent(event);
  } finally {
    reader.releaseLock();
  }
}
