import { randomUUID } from 'node:crypto';
import { test, expect } from '@playwright/test';
import { loginApi } from '../utils/api-client';
import { sendHikvisionEvent } from '../utils/hikvision';

test('placa desconhecida gera acesso negado', async ({ page, request }) => {
  const username = process.env.E2E_ADMIN_USERNAME ?? 'admin';
  const password = process.env.E2E_ADMIN_PASSWORD ?? 'E2E@GarageControl#2026';
  await loginApi(request, username, password);

  const plate = `UNK${Date.now().toString().slice(-4)}`.slice(0, 7);
  const result = await sendHikvisionEvent(
    request,
    plate,
    randomUUID(),
    process.env.E2E_HIKVISION_WEBHOOK_TOKEN ?? 'e2e-entry-secret-at-least-32-characters',
  );

  expect(result.movementStatus).toBe('DENIED');
  expect(result.accessDecision).toBe('UNKNOWN_VEHICLE');

  await page.goto('/login');
  await page.getByLabel('Usuário').fill(username);
  await page.getByLabel('Senha').fill(password);
  await page.getByRole('button', { name: 'Entrar' }).click();
  await expect(page).toHaveURL(/\/dashboard$/);
  await page.getByRole('link', { name: 'Portaria', exact: true }).click();
  await expect(page.getByRole('heading', { name: 'Portaria' })).toBeVisible();
  await expect(page.getByText(plate, { exact: true }).first()).toBeVisible({ timeout: 15_000 });
});

test('usuário ATTENDANT não acessa administração de usuários', async ({ browser, request }) => {
  const adminUser = process.env.E2E_ADMIN_USERNAME ?? 'admin';
  const adminPassword = process.env.E2E_ADMIN_PASSWORD ?? 'E2E@GarageControl#2026';
  const admin = await loginApi(request, adminUser, adminPassword);
  const username = `attendant_${Date.now().toString().slice(-6)}`;
  const response = await request.post('/api/v1/users', {
    headers: { Authorization: `Bearer ${admin.accessToken}` },
    data: {
      username,
      password: 'Attendant@E2E#2026',
      fullName: 'Atendente E2E',
      role: 'ATTENDANT',
      active: true,
    },
  });
  await expect(response).toBeOK();

  const context = await browser.newContext();
  const page = await context.newPage();
  await page.goto('/login');
  await page.getByLabel('Usuário').fill(username);
  await page.getByLabel('Senha').fill('Attendant@E2E#2026');
  await page.getByRole('button', { name: 'Entrar' }).click();
  await expect(page).toHaveURL(/\/dashboard$/);
  await expect(page.getByRole('link', { name: 'Usuários' })).toHaveCount(0);
  await page.goto('/users');
  await expect(page.getByRole('heading', { name: 'Acesso negado' })).toBeVisible();
  await context.close();
});
