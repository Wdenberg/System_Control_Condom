import { afterEach, describe, expect, it, vi } from 'vitest';
import { api, ApiError } from '@/services/api';
import { saveSession, clearSession } from '@/auth/auth-storage';

afterEach(() => {
  vi.restoreAllMocks();
  vi.unstubAllGlobals();
  clearSession();
});

describe('api client', () => {
  it('sends bearer token when a session exists', async () => {
    saveSession({ accessToken: 'abc', tokenType: 'Bearer', expiresAt: new Date(Date.now() + 60_000).toISOString(), user: { id: 1, username: 'u', fullName: 'User', role: 'VIEWER', permissions: [] } });
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(new Response(JSON.stringify({ activeApartments: 2 }), { status: 200, headers: { 'content-type': 'application/json' } })));

    await api.dashboard.summary();
    const [, init] = vi.mocked(fetch).mock.calls[0];
    expect((init?.headers as Headers).get('Authorization')).toBe('Bearer abc');
  });

  it('dispatches auth event on HTTP 401', async () => {
    const listener = vi.fn();
    window.addEventListener('auth:unauthorized', listener);
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(new Response('', { status: 401 })));

    await expect(api.me()).rejects.toThrow('UNAUTHORIZED');
    expect(listener).toHaveBeenCalledTimes(1);
    window.removeEventListener('auth:unauthorized', listener);
  });

  const registration = {
    requestId: 'e876af92-5080-4508-948d-5ab4fe6d13ab',
    apartmentNumber: '101', apartmentActive: true,
    residents: [{ fullName: 'Maria Silva', active: true }],
    parkingNumber: '127', parkingType: 'ASSIGNED', parkingActive: true,
    plate: 'ABC1D23', brand: 'Fiat', model: 'Uno', color: 'Branco', vehicleActive: false,
  };

  it('posts the atomic registration contract with a bounded timeout', async () => {
    const signal = new AbortController().signal;
    const timeout = vi.spyOn(AbortSignal, 'timeout').mockReturnValue(signal);
    const result = { apartmentId: 1, residentIds: [2], parkingSpaceId: 3, vehicleId: 4 };
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(new Response(JSON.stringify(result), { status: 201, headers: { 'content-type': 'application/json' } })));
    await expect(api.registrations.create(registration)).resolves.toEqual(result);
    const [url, init] = vi.mocked(fetch).mock.calls[0];
    expect(url).toMatch(/\/v1\/registrations$/);
    expect(init?.method).toBe('POST');
    expect(JSON.parse(init?.body as string)).toEqual(registration);
    expect(init?.signal).toBe(signal);
    expect(timeout).toHaveBeenCalledWith(15_000);
  });

  it('preserves a supplied cancellation signal and does not retry uncertain registration results', async () => {
    const controller = new AbortController();
    const error = new DOMException('The operation timed out.', 'TimeoutError');
    vi.stubGlobal('fetch', vi.fn().mockRejectedValue(error));
    await expect(api.registrations.create(registration, controller.signal)).rejects.toBe(error);
    expect(fetch).toHaveBeenCalledTimes(1);
    expect(vi.mocked(fetch).mock.calls[0][1]?.signal).toBe(controller.signal);
  });

  it('preserves structured conflict errors for registration recovery', async () => {
    const body = { message: 'Apartamento já cadastrado.' };
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(new Response(JSON.stringify(body), { status: 409, headers: { 'content-type': 'application/json' } })));
    await expect(api.registrations.create(registration)).rejects.toMatchObject({
      status: 409, body, message: body.message,
    } satisfies Partial<ApiError>);
  });

  it('revokes the original token without restoring it from storage or dispatching another unauthorized event', async () => {
    const listener = vi.fn();
    window.addEventListener('auth:unauthorized', listener);
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(new Response('', { status: 401 })));
    try {
      await expect(api.revoke('expired-token')).resolves.toBeUndefined();
      const [url, init] = vi.mocked(fetch).mock.calls[0];
      expect(url).toMatch(/\/v1\/auth\/revoke$/);
      expect(init?.method).toBe('POST');
      expect(new Headers(init?.headers).get('Authorization')).toBe('Bearer expired-token');
      expect(listener).not.toHaveBeenCalled();
    } finally {
      window.removeEventListener('auth:unauthorized', listener);
    }
  });

  it('sends a physical passage observation separately from access authorization', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(new Response(JSON.stringify({ movementId: 7, passageState: 'COMPLETED' }), { status: 200, headers: { 'content-type': 'application/json' } })));
    await api.movements.confirmPassage(7, true, 'Observado na portaria.');
    const [url, init] = vi.mocked(fetch).mock.calls[0];
    expect(url).toMatch(/\/v1\/movements\/7\/passage-confirmation$/);
    expect(init?.method).toBe('POST');
    expect(JSON.parse(init?.body as string)).toEqual({ passed: true, justification: 'Observado na portaria.', controllerSettled: false });
  });

  it('sends explicit controller verification when reconciling an uncertain command', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(new Response(JSON.stringify({ movementId: 7, passageState: 'FAILED' }), { status: 200, headers: { 'content-type': 'application/json' } })));
    await api.movements.confirmPassage(7, false, 'Controlador verificado na portaria.', true);
    const [, init] = vi.mocked(fetch).mock.calls[0];
    expect(JSON.parse(init?.body as string)).toEqual({ passed: false, justification: 'Controlador verificado na portaria.', controllerSettled: true });
  });
});
