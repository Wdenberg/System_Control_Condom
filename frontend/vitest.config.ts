import { defineConfig } from 'vitest/config';
import react from '@vitejs/plugin-react';

export default defineConfig({
  plugins: [react()],
  resolve: {
    tsconfigPaths: true,
  },
  test: {
    environment: 'jsdom',
    globals: true,
    setupFiles: './src/test-setup.ts',
    css: true,
    coverage: {
      provider: 'v8',
      include: ['src/**/*.{ts,tsx}'],
      exclude: ['src/**/*.test.{ts,tsx}', 'src/**/__tests__/**', 'src/test-setup.ts', 'src/main.tsx', 'src/types/**'],
      reporter: ['text', 'html', 'lcov', 'json-summary'],
      // Initial gates must be raised after the first verified CI baseline.
      thresholds: { lines: 40, statements: 40, functions: 30, branches: 30 },
    },
  },
});
