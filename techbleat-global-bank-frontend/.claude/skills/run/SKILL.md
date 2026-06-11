---
description: Launch the Techbleat Global Bank frontend dev server and drive it with Playwright
---

# Run: Techbleat Global Bank Frontend

Vite + React app. Dev server on port 3000. Backend APIs (User, TX, Activity) are separate services — the frontend works without them but shows a "Failed to fetch" banner.

## Prerequisites

node_modules/.bin/vite must be a symlink, not a plain file. If the dev server fails with `ERR_MODULE_NOT_FOUND: dist/node/cli.js`, fix it once:

```bash
rm node_modules/.bin/vite
ln -s ../vite/bin/vite.js node_modules/.bin/vite
```

## Start

```bash
npm run dev > /tmp/vite-dev.log 2>&1 &
echo $! > /tmp/vite-dev.pid
until curl -sf http://localhost:3000 > /dev/null; do sleep 1; done
echo "Server ready"
```

## Stop

```bash
kill $(cat /tmp/vite-dev.pid) 2>/dev/null || pkill -f 'vite --host'
```

## Drive with Playwright

Playwright is not in the project's dependencies. Install it in a temp workspace:

```bash
mkdir -p /tmp/pw-test && cd /tmp/pw-test
npm init -y > /dev/null && npm install playwright
npx playwright install chromium
```

Then write and run a `.mjs` script from `/tmp/pw-test/`:

```js
import { chromium } from 'playwright';

const browser = await chromium.launch({ headless: true });
const page = await browser.newPage();

await page.goto('http://localhost:3000');
// login screen
await page.fill('input[placeholder="Enter your user ID"]', 'user1');
await page.click('text=Sign In');
await page.waitForTimeout(1000);
await page.screenshot({ path: '/tmp/shot-dashboard.png' });

await browser.close();
```

## Screens and their h1 text

| Nav button | h1 |
|---|---|
| login | Welcome back |
| register | Create your account |
| dashboard | Techbleat Global Bank |
| transfer | Move money |
| report | Statements & reports |

## Environment variables

API URLs live in `.env` (Vite prefix `VITE_`). Defaults:

```
VITE_USER_API=http://localhost:8000
VITE_TX_API=http://localhost:8080
VITE_ACTIVITY_API=http://localhost:8001
```

Override per environment with `.env.local` or `.env.production`.

## Gotchas

- **"Failed to fetch" banner on load** — expected when backend services are not running. The UI itself is fully functional for navigation and form interaction.
- **Broken node_modules** — `npm install` alone does not fix the symlink issue; manually re-link `node_modules/.bin/vite` as shown above.
- **React controlled inputs** — use Playwright's `page.fill()`, not `eval`/`element.value =`; the latter does not trigger React's `onChange`.
- **Register/login require the User API** (`localhost:8000`) to be running before those flows succeed end-to-end.
