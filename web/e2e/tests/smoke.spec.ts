import { expect, test, type Page } from '@playwright/test';

// The demo's users (docker/demo.env and the "demo" profile)
const ADMIN = 'admin@demo.orudesk.org';
const PRINCIPAL = 'principal@demo.orudesk.org';
const PASSWORD = 'demo-password';

async function logIn(page: Page, email: string, path = '/') {
  await page.goto(path);
  await page.getByLabel(/Email/).fill(email);
  await page.getByLabel(/Password/).fill(PASSWORD);
  await page.getByRole('button', { name: 'Log in' }).click();
}

/** On a phone the menu hides behind the burger button. */
async function openMenu(page: Page) {
  const burger = page.getByRole('button', { name: 'Menu' });
  if (await burger.isVisible()) await burger.click();
}

test('a wrong password is refused, with a message', async ({ page }) => {
  await page.goto('/');
  await page.getByLabel(/Email/).fill(ADMIN);
  await page.getByLabel(/Password/).fill('not-the-password');
  await page.getByRole('button', { name: 'Log in' }).click();

  await expect(page.getByRole('alert')).toHaveText('Wrong email or password.');
});

test('an admin logs in, stays logged in across a reload, and logs out', async ({
  page,
  context,
}) => {
  await logIn(page, ADMIN);
  await expect(page.getByText(`Signed in as ${ADMIN}`)).toBeVisible();

  // The refresh token is an HttpOnly cookie: there, but out of reach of scripts
  const cookie = (await context.cookies()).find((c) => c.name === 'orudesk_refresh');
  expect(cookie?.httpOnly).toBe(true);
  expect(await page.evaluate(() => document.cookie)).toBe('');

  await page.reload();
  await expect(page.getByText(`Signed in as ${ADMIN}`)).toBeVisible();

  await page.getByRole('button', { name: 'Account' }).click();
  await page.getByRole('menuitem', { name: 'Log out' }).click();
  await expect(page.getByRole('heading', { name: 'Log in to Orudesk' })).toBeVisible();
});

test('a deep link survives the login, and the registry can be searched', async ({ page }) => {
  await logIn(page, ADMIN, '/people');

  await expect(page).toHaveURL(/\/people$/);
  await page.getByLabel('Search').fill('kumar');
  await page.getByRole('link', { name: 'Kumar, Asha' }).click();
  await expect(page.getByRole('heading', { name: 'Asha Kumar' })).toBeVisible();
});

test('an admin adds a person; the server checks the form', async ({ page }) => {
  await logIn(page, ADMIN, '/people/new');
  await page.getByRole('button', { name: 'Save' }).click();
  await expect(page.getByText('must not be blank').first()).toBeVisible();

  const lastName = `Smoke-${Date.now()}`; // unique, so a rerun against the same demo still passes
  await page.getByLabel(/First name/).fill('Test');
  await page.getByLabel(/Last name/).fill(lastName);
  await page.getByRole('button', { name: 'Save' }).click();

  await expect(page.getByRole('heading', { name: `Test ${lastName}` })).toBeVisible();
});

test('a school admin works in their school, and the admin screens are not theirs', async ({
  page,
}) => {
  await logIn(page, PRINCIPAL);

  await expect(page.getByLabel('Your roles here')).toContainText('ADMIN');
  // Their only unit, picked for them
  await expect(page.getByRole('combobox', { name: 'Org unit' })).toHaveValue('North School');
  await openMenu(page);
  await expect(page.getByRole('link', { name: 'Users' })).toHaveCount(0);

  await page.goto('/users');
  await expect(page.getByRole('alert')).toHaveText("You don't have access to this page.");
});

test('an unknown address shows the 404 page inside the app', async ({ page }) => {
  await logIn(page, ADMIN, '/no-such-page');
  await expect(page.getByRole('heading', { name: 'Page not found' })).toBeVisible();
});
