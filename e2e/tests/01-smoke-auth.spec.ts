import { test, expect } from '@playwright/test';
import { loginApi } from '../utils/api-client';

test('login real -> dashboard -> session authenticated', async ({ page, request }) => {
  const username = process.env.E2E_ADMIN_USERNAME ?? 'admin';
  const password = process.env.E2E_ADMIN_PASSWORD ?? 'E2E@GarageControl#2026';

  await page.goto('/login');
  await page.getByLabel('Usuário').fill(username);
  await page.getByLabel('Senha').fill(password);
  await page.getByRole('button', { name: 'Entrar' }).click();

  await expect(page).toHaveURL(/\/dashboard$/);
  await expect(page.getByRole('heading', { name: 'Dashboard' })).toBeVisible();

  const session = await loginApi(request, username, password);
  expect(session.user.role).toBe('SUPER_ADMIN');
  expect(session.user.permissions).toContain('DASHBOARD_READ');
});
