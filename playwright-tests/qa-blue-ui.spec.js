const { test, expect } = require('@playwright/test');

test('QA stage UI shows blue-green deployment context', async ({ page }) => {
  await page.goto('/', { waitUntil: 'domcontentloaded' });

  await expect(page.locator('body')).toContainText(/Harness Blue\/Green Deployment/i);
  await expect(page.locator('body')).toContainText(/How Harness Blue\/Green Works:/i);
  await expect(page.locator('body')).toContainText(/Stage Deployment/i);
  await expect(page.locator('body')).toContainText(/Validate/i);
  await expect(page.locator('body')).toContainText(/BLUE|GREEN/i);
  await expect(page.getByRole('link', { name: /Health Check/i })).toBeVisible();
});
