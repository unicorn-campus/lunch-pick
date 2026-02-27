const { chromium } = require('playwright');

(async () => {
  const browser = await chromium.launch({ headless: true });
  const context = await browser.newContext({
    viewport: { width: 390, height: 844 }  // iPhone 14 size
  });

  const results = [];
  const pages = [
    { name: 'Login Page', url: 'http://localhost:3000/login', expect: 'rendered' },
    { name: 'Home Page', url: 'http://localhost:3000/', expect: 'redirect-or-render' },
    { name: 'Quiz Page', url: 'http://localhost:3000/quiz', expect: 'rendered' },
    { name: 'Location Page', url: 'http://localhost:3000/location', expect: 'rendered' },
    { name: 'Dietary Page', url: 'http://localhost:3000/dietary', expect: 'rendered' },
    { name: 'Navigation Page', url: 'http://localhost:3000/navigation', expect: 'rendered' },
    { name: 'Meal Record Page', url: 'http://localhost:3000/meal-record', expect: 'rendered' },
    { name: 'Insights Page', url: 'http://localhost:3000/insights', expect: 'rendered' },
    { name: 'Profile Page', url: 'http://localhost:3000/profile', expect: 'rendered' },
    { name: 'Subscription Page', url: 'http://localhost:3000/subscription', expect: 'rendered' },
  ];

  for (const p of pages) {
    const page = await context.newPage();
    try {
      const response = await page.goto(p.url, { timeout: 15000, waitUntil: 'domcontentloaded' });
      const status = response?.status() || 0;
      const title = await page.title();
      const hasContent = (await page.content()).length > 500;
      const errors = [];

      // Check for console errors
      page.on('pageerror', err => errors.push(err.message));

      // Check for visible text content
      const bodyText = await page.evaluate(() => document.body?.innerText?.length || 0);

      const passed = status === 200 && hasContent;
      results.push({
        name: p.name,
        url: p.url,
        status,
        hasContent,
        bodyTextLen: bodyText,
        passed: passed ? 'PASS' : 'FAIL',
      });
    } catch (err) {
      results.push({
        name: p.name,
        url: p.url,
        status: 0,
        error: err.message.substring(0, 100),
        passed: 'FAIL',
      });
    }
    await page.close();
  }

  await browser.close();

  // Print results
  console.log('============================================');
  console.log(' LunchPick Browser Smoke Test Results');
  console.log('============================================');
  let pass = 0, fail = 0;
  for (const r of results) {
    const mark = r.passed === 'PASS' ? 'PASS' : 'FAIL';
    if (mark === 'PASS') pass++; else fail++;
    console.log(`  ${mark}  ${r.name.padEnd(22)} HTTP ${r.status}  content=${r.hasContent || false}  bodyLen=${r.bodyTextLen || 0}`);
  }
  console.log('--------------------------------------------');
  console.log(` TOTAL: ${results.length} | PASS: ${pass} | FAIL: ${fail}`);
  console.log('============================================');

  process.exit(fail > 0 ? 1 : 0);
})();
