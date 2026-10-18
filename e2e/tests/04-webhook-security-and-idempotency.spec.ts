import { randomUUID } from 'node:crypto';
import { test, expect } from '@playwright/test';
import { loginApi, authorizedRequest, seedParkingScenario } from '../utils/api-client';
import { buildAnprXml, sendHikvisionEvent } from '../utils/hikvision';

test('webhook rejeita token inválido', async ({ request }) => {
  const response = await request.post('/api/v1/integrations/hikvision/events', {
    headers: {
      'Content-Type': 'application/xml',
      'X-Hikvision-Webhook-Token': 'invalid-token',
    },
    data: buildAnprXml({ plate: 'BAD9999', uuid: randomUUID() }),
  });
  expect(response.status()).toBe(401);
});

test('mesmo UUID não cria dois movimentos', async ({ request }) => {
  const admin = await loginApi(
    request,
    process.env.E2E_ADMIN_USERNAME ?? 'admin',
    process.env.E2E_ADMIN_PASSWORD ?? 'E2E@GarageControl#2026',
  );
  const scenario = await seedParkingScenario(request, admin.accessToken);
  const uuid = randomUUID();
  const token = process.env.E2E_HIKVISION_WEBHOOK_TOKEN ?? 'e2e-entry-secret-at-least-32-characters';
  const occurredAt = new Date().toISOString();

  const first = await sendHikvisionEvent(request, scenario.plate, uuid, token, '192.168.15.43', occurredAt);
  const second = await sendHikvisionEvent(request, scenario.plate, uuid, token, '192.168.15.43', occurredAt);

  expect(first.eventId).toBe(second.eventId);
  expect(second.status).toBe('DUPLICATE');

  const movements = await authorizedRequest(request, admin.accessToken, 'GET', `/api/v1/movements?page=0&size=100`);
  await expect(movements).toBeOK();
  const body = await movements.json() as { content: Array<{ movementId: number }> };
  const matching = body.content.filter((movement) => movement.movementId === first.movementId);
  expect(matching).toHaveLength(1);
});
