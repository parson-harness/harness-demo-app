const { test, expect } = require('@playwright/test');

const isBlueGreenVariant = (value) => ['blue', 'green'].includes(String(value || '').toLowerCase());

test('QA stage health endpoint is up and identifies a blue-green variant', async ({ request }) => {
  const response = await request.get('/api/health');
  expect(response.ok()).toBeTruthy();

  const data = await response.json();
  expect(data.status).toBe('UP');
  expect(isBlueGreenVariant(data.deploymentVariant)).toBeTruthy();
});

test('QA stage info endpoint exposes blue-green deployment metadata', async ({ request }) => {
  const response = await request.get('/api/info');
  expect(response.ok()).toBeTruthy();

  const data = await response.json();
  expect(data.deploymentStrategy).toBe('blue-green');
  expect(isBlueGreenVariant(data.deploymentVariant)).toBeTruthy();
  expect(String(data.deploymentNarrative || '')).toMatch(/viewing the (blue|green) release/i);
});
