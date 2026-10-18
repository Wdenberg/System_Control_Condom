import { randomUUID } from 'node:crypto';
import { test, expect } from '@playwright/test';
import { loginApi, seedParkingScenario } from '../utils/api-client';
import { sendHikvisionEvent } from '../utils/hikvision';

test('cadastro -> Hikvision -> autorização -> portaria em tempo real', async ({ page, request }) => {
  const admin = await loginApi(
    request,
    process.env.E2E_ADMIN_USERNAME ?? 'admin',
    process.env.E2E_ADMIN_PASSWORD ?? 'E2E@GarageControl#2026',
  );
  const scenario = await seedParkingScenario(request, admin.accessToken);

  await page.goto('/login');
  await page.getByLabel('Usuário').fill(process.env.E2E_ADMIN_USERNAME ?? 'admin');
  await page.getByLabel('Senha').fill(process.env.E2E_ADMIN_PASSWORD ?? 'E2E@GarageControl#2026');
  await page.getByRole('button', { name: 'Entrar' }).click();
  await expect(page).toHaveURL(/\/dashboard$/);

  await page.goto('/portaria');
  await expect(page.getByRole('heading', { name: 'Portaria' })).toBeVisible();

  const response = await sendHikvisionEvent(
    request,
    scenario.plate,
    randomUUID(),
    process.env.E2E_HIKVISION_WEBHOOK_TOKEN ?? 'e2e-entry-secret-at-least-32-characters',
  );

  expect(response.status).toBe('RECEIVED');
  expect(response.movementId).not.toBeNull();
  expect(response.movementStatus).toBe('AUTHORIZED');
  expect(response.accessDecision).toBe('AUTHORIZED');

  await expect(page.getByText(scenario.plate, { exact: true }).first()).toBeVisible({ timeout: 15_000 });
  await expect(page.getByText('Morador E2E', { exact: false }).first()).toBeVisible();
  await expect(page.getByText('Entrada', { exact: true }).first()).toBeVisible();
});
