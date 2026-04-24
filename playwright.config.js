const { defineConfig, devices } = require('@playwright/test');

const parsedWorkers = Number.parseInt(process.env.PLAYWRIGHT_WORKERS || '2', 10);
const workers = Number.isNaN(parsedWorkers) ? 2 : parsedWorkers;

module.exports = defineConfig({
  testDir: './playwright-tests',
  timeout: 30000,
  fullyParallel: true,
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 1 : 0,
  workers,
  reporter: [
    ['list'],
    ['junit', { outputFile: 'playwright-report/junit/results.xml' }],
    ['html', { outputFolder: 'playwright-report/html', open: 'never' }]
  ],
  use: {
    baseURL: process.env.PLAYWRIGHT_BASE_URL || 'http://127.0.0.1:8080',
    ignoreHTTPSErrors: true,
    trace: 'retain-on-failure',
    screenshot: 'only-on-failure',
    video: 'retain-on-failure'
  },
  projects: [
    {
      name: 'chromium',
      use: {
        ...devices['Desktop Chrome'],
        browserName: 'chromium'
      }
    }
  ]
});
