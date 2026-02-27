import { defineConfig } from '@playwright/test';
import path from 'path';

export default defineConfig({
  testDir: './tests',
  outputDir: path.resolve(__dirname, '../.temp/playwright-output'),
  timeout: 30000,
  retries: 0,
  workers: 1,
  reporter: [
    ['json', { outputFile: path.resolve(__dirname, '../.temp/test-results.json') }],
    ['list']
  ],
  use: {
    baseURL: 'http://localhost:3000',
    screenshot: 'only-on-failure',
    trace: 'retain-on-failure',
  },
  projects: [
    { name: 'chromium', use: { browserName: 'chromium' } },
  ],
});
