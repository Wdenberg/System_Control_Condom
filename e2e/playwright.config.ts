import { defineConfig, devices } from '@playwright/test';

const baseURL = process.env.E2E_FRONTEND_URL ?? 'http://127.0.0.1:5173';
const backendURL = process.env.E2E_BACKEND_URL ?? 'http://127.0.0.1:8080';

export default defineConfig({
  testDir: './tests',
  fullyParallel: false,
  forbidOnly: Boolean(process.env.CI),
  retries: process.env.CI ? 2 : 0,
  workers: 1,
  reporter: [['list'], ['html', { outputFolder: 'playwright-report', open: 'never' }]],
  timeout: 60_000,
  expect: { timeout: 10_000 },
  use: {
    baseURL,
    trace: 'retain-on-failure',
    screenshot: 'only-on-failure',
    video: 'retain-on-failure',
    actionTimeout: 15_000,
    navigationTimeout: 20_000,
  },
  projects: [
    { name: 'chromium', use: { ...devices['Desktop Chrome'] } },
  ],
  webServer: {
    command: 'npm run dev -- --host 127.0.0.1',
    cwd: '../frontend',
    url: baseURL,
    reuseExistingServer: false,
    timeout: 120_000,
    env: {
      VITE_DEV_BACKEND_URL: backendURL,
      VITE_API_BASE_URL: '/api',
    },
  },
});
