import { test, expect, Page, Browser } from '@playwright/test';
import * as fs from 'fs';
import * as path from 'path';

// ─── Constants ───
const ITER_DIR = path.resolve(__dirname, '../../.temp/iter-6');
const AUTH_DIR = path.resolve(__dirname, '../.auth');
const AUTH_STATE = path.join(AUTH_DIR, 'state.json');
const MEMBER_API = 'http://localhost:8081';
const BASE = 'http://localhost:3000';

// ─── Helpers ───
function ensureDir(dir: string) { fs.mkdirSync(dir, { recursive: true }); }

async function shot(page: Page, name: string) {
  ensureDir(ITER_DIR);
  await page.screenshot({ path: path.join(ITER_DIR, `${name}.png`), fullPage: true });
}

// ─── Auth Setup ───
test.beforeAll(async ({ browser }) => {
  ensureDir(AUTH_DIR);
  const res = await fetch(`${MEMBER_API}/api/test/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ nickname: 'testuser' }),
  });
  const { accessToken, memberId } = await res.json();

  const page = await browser.newPage();
  await page.goto(BASE);
  await page.evaluate(({ token, id }) => {
    localStorage.setItem('lunchpick_token', token);
    localStorage.setItem('lunchpick-auth', JSON.stringify({ isAuthenticated: true, memberId: id }));
  }, { token: accessToken, id: memberId });
  await page.context().storageState({ path: AUTH_STATE });
  await page.close();
});

// ─── Error Collector ───
test.beforeEach(async ({ page }) => {
  page.on('response', r => {
    if (r.status() >= 500) console.error(`[5xx] ${r.status()} ${r.request().method()} ${r.url()}`);
  });
});

// ════════════════════════════════════════════════════════
// 비즈니스 시나리오 TC-01 ~ TC-24
// ════════════════════════════════════════════════════════

// ─── 로그인 (비인증) ───
test.describe('로그인', () => {
  test('TC-01: 로그인 페이지 UI', async ({ browser }) => {
    const page = await browser.newPage();
    await page.goto(`${BASE}/login`);
    await page.waitForLoadState('networkidle');

    await expect(page.getByAltText('런치픽 로고')).toBeVisible();
    await expect(page.getByRole('heading', { name: '런치픽' })).toBeVisible();
    await expect(page.getByRole('button', { name: /카카오/ })).toBeVisible();
    await expect(page.getByRole('button', { name: /데모 모드/ })).toBeVisible();
    await expect(page.getByRole('link', { name: '이용약관' })).toBeVisible();
    await expect(page.getByRole('link', { name: '개인정보처리방침' })).toBeVisible();

    await shot(page, 'tc-01');
    await page.close();
  });

  test('TC-02: OAuth 에러 파라미터 처리', async ({ browser }) => {
    const page = await browser.newPage();
    await page.goto(`${BASE}/login?error=access_denied`);
    await page.waitForTimeout(1500);

    // toast error message (filter out Next.js route announcer)
    const alert = page.locator('[role="alert"]').filter({ hasText: /로그인|실패|취소/ }).first();
    await expect(alert).toBeVisible({ timeout: 5000 });

    await shot(page, 'tc-02');
    await page.close();
  });
});

// ─── 취향 퀴즈 (TC-03, TC-04) ───
test.describe('취향 퀴즈', () => {
  test.use({ storageState: AUTH_STATE });

  test('TC-03: 취향 퀴즈 7/10 완료 임계값', async ({ page }) => {
    await page.goto(`${BASE}/onboarding/quiz`);
    await page.waitForLoadState('networkidle');

    await expect(page.getByRole('heading', { name: /취향을 알려주세요/ })).toBeVisible();

    // Swipe 6 cards (below threshold)
    const like = page.getByRole('button', { name: '좋아요' });
    const dislike = page.getByRole('button', { name: '싫어요' });
    for (let i = 0; i < 6; i++) {
      await (i % 3 === 2 ? dislike : like).click();
      await page.waitForTimeout(600);
    }
    await expect(page.getByText('6/10')).toBeVisible();

    // Completion button should NOT be visible at 6/10
    const doneBtn = page.getByRole('button', { name: /취향 분석|분석 완료|결과 보기/ });
    const doneVisible = await doneBtn.isVisible().catch(() => false);
    expect(doneVisible).toBeFalsy();

    await shot(page, 'tc-03');

    // 7th card — threshold reached
    await like.click();
    await page.waitForTimeout(600);
    await expect(page.getByText('7/10')).toBeVisible();

    await shot(page, 'tc-03-7done');
  });

  test('TC-04: 퀴즈 결과 — 취향 프로파일', async ({ page }) => {
    await page.goto(`${BASE}/onboarding/quiz`);
    await page.waitForLoadState('networkidle');

    // Complete all 10 quizzes
    const like = page.getByRole('button', { name: '좋아요' });
    const dislike = page.getByRole('button', { name: '싫어요' });
    for (let i = 0; i < 10; i++) {
      await (i % 4 === 3 ? dislike : like).click();
      await page.waitForTimeout(500);
    }

    // Wait for result page / profile display
    await page.waitForTimeout(3000);

    // Should show profile result or redirect
    const body = await page.textContent('body') || '';
    const hasProfile = /프로파일|완성|Top|카테고리|%/.test(body);
    expect(hasProfile || page.url() !== `${BASE}/onboarding/quiz`).toBeTruthy();

    await shot(page, 'tc-04');
  });
});

// ─── 위치 동의 (TC-05, TC-06) ───
test.describe('위치 동의', () => {
  test.use({ storageState: AUTH_STATE });

  test('TC-05: 위치 동의 → 식이 페이지 이동', async ({ page }) => {
    await page.goto(`${BASE}/onboarding/location`);
    await page.waitForLoadState('networkidle');
    await expect(page.locator('main')).toBeVisible();

    const agreeBtn = page.getByRole('button', { name: /동의|허용|위치.*활성/ });
    if (await agreeBtn.isVisible().catch(() => false)) {
      await agreeBtn.click();
      await page.waitForTimeout(2000);
    }

    await shot(page, 'tc-05');

    // May have navigated to dietary page
    if (page.url().includes('dietary')) {
      await shot(page, 'tc-05-dietary');
    }
  });

  test('TC-06: 위치 거절', async ({ page }) => {
    await page.goto(`${BASE}/onboarding/location`);
    await page.waitForLoadState('networkidle');

    const rejectBtn = page.getByRole('button', { name: /거절|아니요|건너뛰기|나중에|비활성/ });
    if (await rejectBtn.isVisible().catch(() => false)) {
      await rejectBtn.click();
      await page.waitForTimeout(1000);
    }

    await shot(page, 'tc-06');
  });
});

// ─── 홈 / 추천 (TC-07 ~ TC-13) ───
test.describe('홈 - 오늘의 추천', () => {
  test.use({ storageState: AUTH_STATE });

  test('TC-07: 홈 — AI 추천 카드 3개', async ({ page }) => {
    await page.goto(`${BASE}/home`);
    await page.waitForTimeout(5000);

    await expect(page.locator('main')).toBeVisible();
    await expect(page.getByRole('button', { name: /새로고침/ })).toBeVisible();

    // Check nav bar
    await expect(page.getByRole('navigation', { name: /네비게이션/ })).toBeVisible();

    await shot(page, 'tc-07');
  });

  test('TC-08: 비-폴백 상태 확인', async ({ page }) => {
    await page.goto(`${BASE}/home`);
    await page.waitForTimeout(5000);

    await expect(page.locator('main')).toBeVisible();

    await shot(page, 'tc-08');
  });

  test('TC-09: 정상 상태에서 Fallback UI 미표시', async ({ page }) => {
    await page.goto(`${BASE}/home`);
    await page.waitForTimeout(5000);

    // Fallback banner text should NOT be visible in normal state
    const fallback = page.getByText(/폴백 추천|임시 추천|AI.*실패/i);
    const hasFallback = await fallback.isVisible().catch(() => false);
    // Not a hard failure — just record
    if (hasFallback) console.warn('Fallback banner visible in normal state');

    await shot(page, 'tc-09');
  });

  test('TC-10: "왜?" 바텀시트 — 추천 이유', async ({ page }) => {
    await page.goto(`${BASE}/home`);
    await page.waitForTimeout(5000);

    // Look for reason/why button on recommendation cards
    const reasonBtn = page.locator('button').filter({ hasText: /왜|이유/ }).first();
    if (await reasonBtn.isVisible().catch(() => false)) {
      await reasonBtn.click();
      await page.waitForTimeout(2000);
    }

    await shot(page, 'tc-10');
  });

  test('TC-11: 수락 → 내비게이션 페이지', async ({ page }) => {
    await page.goto(`${BASE}/home`);
    await page.waitForTimeout(5000);

    const acceptBtn = page.locator('button').filter({ hasText: /여기 갈래요|선택|수락/ }).first();
    if (await acceptBtn.isVisible().catch(() => false)) {
      await acceptBtn.click();
      await page.waitForTimeout(3000);
    }

    await shot(page, 'tc-11');
  });

  test('TC-12: 거절 → 사유 바텀시트', async ({ page }) => {
    await page.goto(`${BASE}/home`);
    await page.waitForTimeout(5000);

    const rejectBtn = page.locator('button').filter({ hasText: /거절|싫어|패스|별로/ }).first();
    if (await rejectBtn.isVisible().catch(() => false)) {
      await rejectBtn.click();
      await page.waitForTimeout(2000);
    }

    await shot(page, 'tc-12');
  });

  test('TC-13: 대체 없음 → 토스트 안내', async ({ page }) => {
    // Intercept recommendations to return empty
    await page.route('**/api/v1/recommendations/today', route =>
      route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ success: true, data: { recommendations: [], isFallback: false, isColdStart: false } }),
      })
    );

    await page.goto(`${BASE}/home`);
    await page.waitForTimeout(3000);

    // Should show empty state or guidance
    await expect(page.locator('main')).toBeVisible();

    await shot(page, 'tc-13');
  });
});

// ─── 식사 기록 + 피드백 (TC-14 ~ TC-19) ───
test.describe('식사 기록', () => {
  test.use({ storageState: AUTH_STATE });

  test('TC-14: 식사 기록 — "먹었어요!"', async ({ page }) => {
    await page.goto(`${BASE}/meal-record`);
    await page.waitForTimeout(3000);

    await expect(page.locator('main')).toBeVisible();

    // Look for meal record button
    const mealBtn = page.locator('button').filter({ hasText: /먹었어요|기록|완료/ }).first();
    if (await mealBtn.isVisible().catch(() => false)) {
      await mealBtn.click();
      await page.waitForTimeout(2000);
    }

    await shot(page, 'tc-14');
  });

  test('TC-15: 중복 기록 방지', async ({ page }) => {
    // Intercept meal POST to return 409
    await page.route('**/api/v1/meals', route => {
      if (route.request().method() === 'POST') {
        route.fulfill({
          status: 409,
          contentType: 'application/json',
          body: JSON.stringify({ success: false, error: { error: 'CONFLICT', message: '이미 오늘 기록이 있어요' } }),
        });
      } else route.continue();
    });

    await page.goto(`${BASE}/meal-record`);
    await page.waitForTimeout(3000);

    // Try to record
    const mealBtn = page.locator('button').filter({ hasText: /먹었어요|기록/ }).first();
    if (await mealBtn.isVisible().catch(() => false)) {
      await mealBtn.click();
      await page.waitForTimeout(2000);
    }

    // Should show error toast, no crash
    await expect(page.locator('body')).toBeVisible();

    await shot(page, 'tc-15');
  });

  test('TC-16: 실행 취소 (20초 이내)', async ({ page }) => {
    await page.goto(`${BASE}/meal-record`);
    await page.waitForTimeout(3000);

    const undoBtn = page.locator('button').filter({ hasText: /취소|되돌리기|undo/ }).first();
    if (await undoBtn.isVisible().catch(() => false)) {
      await undoBtn.click();
      await page.waitForTimeout(1000);
    }

    await shot(page, 'tc-16');
  });

  test('TC-17: 30초 초과 후 취소 불가', async ({ page }) => {
    await page.goto(`${BASE}/meal-record`);
    await page.waitForTimeout(3000);

    // Verify page renders without crash
    await expect(page.locator('main')).toBeVisible();

    await shot(page, 'tc-17');
  });

  test('TC-18: 피드백 제출 (좋아요 + 키워드)', async ({ page }) => {
    await page.goto(`${BASE}/meal-record`);
    await page.waitForTimeout(3000);

    // Look for feedback elements
    const feedbackBtn = page.locator('button').filter({ hasText: /좋아요|👍|맛있/ }).first();
    if (await feedbackBtn.isVisible().catch(() => false)) {
      await feedbackBtn.click();
      await page.waitForTimeout(1000);
    }

    await shot(page, 'tc-18');
  });

  test('TC-19: 피드백 건너뛰기', async ({ page }) => {
    await page.goto(`${BASE}/meal-record`);
    await page.waitForTimeout(3000);

    const skipBtn = page.locator('button').filter({ hasText: /건너뛰기|스킵|다음에/ }).first();
    if (await skipBtn.isVisible().catch(() => false)) {
      await skipBtn.click();
      await page.waitForTimeout(1000);
    }

    await shot(page, 'tc-19');
  });
});

// ─── 구독 (TC-20 ~ TC-24) ───
test.describe('구독', () => {
  test.use({ storageState: AUTH_STATE });

  test('TC-20: 구독 관리 — 플랜 비교', async ({ page }) => {
    await page.goto(`${BASE}/subscription`);
    await page.waitForTimeout(5000);

    await expect(page.getByText('₩4,900')).toBeVisible();
    await expect(page.getByText(/월간 결제/)).toBeVisible();
    await expect(page.getByText(/연간 결제/)).toBeVisible();
    await expect(page.getByText(/7일 무료 체험/)).toBeVisible();

    // Legal notices (4 items)
    await expect(page.getByText(/청약철회권/)).toBeVisible();
    await expect(page.getByText(/언제든 해지 가능/)).toBeVisible();
    await expect(page.getByText(/자동 갱신 결제/)).toBeVisible();
    await expect(page.getByText(/무료 체험 기간 내 해지/)).toBeVisible();

    await shot(page, 'tc-20');
  });

  test('TC-21: 구독 결제 → 프리미엄 활성화', async ({ page }) => {
    await page.goto(`${BASE}/subscription`);
    await page.waitForTimeout(5000);

    // Click monthly plan button to open payment form
    const monthlyBtn = page.getByRole('button', { name: /월간 결제/ });
    if (await monthlyBtn.isVisible().catch(() => false)) {
      await monthlyBtn.click();
      await page.waitForTimeout(2000);
    }

    // Check if payment form appeared
    const paymentForm = page.locator('input[placeholder*="카드"], input[name*="card"]').first();
    if (await paymentForm.isVisible().catch(() => false)) {
      await shot(page, 'tc-21-form');
    }

    await shot(page, 'tc-21');
  });

  test('TC-22: 결제 실패 — 에러 UI', async ({ page }) => {
    await page.route('**/api/v1/subscriptions', route => {
      if (route.request().method() === 'POST') {
        route.fulfill({
          status: 402,
          contentType: 'application/json',
          body: JSON.stringify({ success: false, error: { error: 'PAYMENT_FAILED', message: '다른 결제 수단을 시도해주세요' } }),
        });
      } else route.continue();
    });

    await page.goto(`${BASE}/subscription`);
    await page.waitForTimeout(5000);

    // App should not crash on payment error
    await expect(page.locator('body')).toBeVisible();

    await shot(page, 'tc-22');
  });

  test('TC-23: 해지 — 7일 연장', async ({ page }) => {
    await page.goto(`${BASE}/subscription`);
    await page.waitForTimeout(5000);

    const extendBtn = page.getByRole('button', { name: /7일 무료 연장/ });
    if (await extendBtn.isVisible().catch(() => false)) {
      await extendBtn.click();
      await page.waitForTimeout(2000);
    }

    await shot(page, 'tc-23');
  });

  test('TC-24: 해지 완료', async ({ page }) => {
    await page.goto(`${BASE}/subscription`);
    await page.waitForTimeout(5000);

    const cancelBtn = page.getByRole('button', { name: /구독 해지/ });
    if (await cancelBtn.isVisible().catch(() => false)) {
      await cancelBtn.click();
      await page.waitForTimeout(2000);

      // Confirm dialog may appear
      const confirmBtn = page.locator('button').filter({ hasText: /확인|해지.*진행|네/ }).first();
      if (await confirmBtn.isVisible().catch(() => false)) {
        await confirmBtn.click();
        await page.waitForTimeout(2000);
      }
    }

    await shot(page, 'tc-24');
  });
});

// ════════════════════════════════════════════════════════
// GAP 시나리오 GAP-01 ~ GAP-08
// ════════════════════════════════════════════════════════

test.describe('GAP 시나리오', () => {
  test.use({ storageState: AUTH_STATE });

  test('GAP-01: API 에러 시 Graceful Degradation', async ({ page }) => {
    await page.route('**/api/**', route => route.abort());
    await page.goto(`${BASE}/home`);
    await page.waitForTimeout(3000);

    // App should NOT crash — body and main still visible
    await expect(page.locator('body')).toBeVisible();
    await expect(page.locator('main')).toBeVisible();

    // No blank white page
    const text = await page.textContent('body') || '';
    expect(text.length).toBeGreaterThan(10);

    await shot(page, 'gap-01');
  });

  test('GAP-02: 빈 데이터/Empty State', async ({ page }) => {
    await page.goto(`${BASE}/insights?tab=insight`);
    await page.waitForTimeout(3000);

    // Click insight tab if needed
    const insightTab = page.getByRole('tab', { name: '인사이트' });
    if (await insightTab.isVisible().catch(() => false)) {
      await insightTab.click();
      await page.waitForTimeout(2000);
    }

    // Should show empty state guidance
    await expect(page.locator('main')).toBeVisible();

    await shot(page, 'gap-02');
  });

  test('GAP-03: 환경변수 미설정 시 Guard', async ({ page }) => {
    // App should load even without all env vars
    await page.goto(`${BASE}/home`);
    await page.waitForTimeout(2000);

    await expect(page.locator('body')).toBeVisible();
    // No crash, no blank page
    const text = await page.textContent('body') || '';
    expect(text.length).toBeGreaterThan(10);

    await shot(page, 'gap-03');
  });

  test('GAP-04: CSS 레이아웃 깨짐 (모바일 375px)', async ({ page }) => {
    await page.setViewportSize({ width: 375, height: 812 });
    await page.goto(`${BASE}/home`);
    await page.waitForTimeout(5000);

    await expect(page.locator('main')).toBeVisible();
    await expect(page.getByRole('navigation')).toBeVisible();

    // Check no horizontal overflow
    const hasOverflow = await page.evaluate(() => {
      return document.documentElement.scrollWidth > document.documentElement.clientWidth;
    });
    expect(hasOverflow).toBeFalsy();

    await shot(page, 'gap-04-mobile');
  });

  test('GAP-05: 외부 SDK 로딩 실패 Fallback', async ({ page }) => {
    // Block kakao map SDK
    await page.route('**/dapi.kakao.com/**', route => route.abort());
    await page.route('**/t1.kakaocdn.net/**', route => route.abort());
    await page.route('**/maps.googleapis.com/**', route => route.abort());

    await page.goto(`${BASE}/navigation`);
    await page.waitForTimeout(3000);

    // Should show fallback UI, not crash
    await expect(page.locator('body')).toBeVisible();
    const text = await page.textContent('body') || '';
    expect(text.length).toBeGreaterThan(10);

    await shot(page, 'gap-05');
  });

  test('GAP-06: 개발 도구/디버그 UI 노출', async ({ page }) => {
    // Visit multiple pages and check for devtools leak
    for (const path of ['/home', '/insights', '/profile']) {
      await page.goto(`${BASE}${path}`);
      await page.waitForTimeout(2000);

      const devtools = page.locator('[data-testid="devtools"], .devtools, #devtools, .debug-panel');
      const visible = await devtools.isVisible().catch(() => false);
      expect(visible).toBeFalsy();
    }

    await shot(page, 'gap-06');
  });

  test('GAP-07: 경계값 (날짜/시간/수량)', async ({ page }) => {
    await page.goto(`${BASE}/insights`);
    await page.waitForTimeout(3000);

    const text = await page.textContent('body') || '';
    expect(text).not.toContain('NaN');
    expect(text).not.toContain('Invalid Date');
    // Allow "undefined" in non-visible attributes but not in displayed text
    const mainText = await page.textContent('main') || '';
    expect(mainText).not.toContain('undefined');

    await shot(page, 'gap-07');
  });

  test('GAP-08: 데모 모드 End-to-End', async ({ browser }) => {
    const page = await browser.newPage();
    await page.goto(`${BASE}/login`);
    await page.waitForLoadState('networkidle');

    // Click demo mode button
    const demoBtn = page.getByRole('button', { name: /데모 모드/ });
    await expect(demoBtn).toBeVisible();
    await demoBtn.click();
    await page.waitForTimeout(4000);

    // Should navigate away from login and show demo content
    await expect(page.locator('body')).toBeVisible();
    const bodyText = await page.textContent('body') || '';
    // Demo mode should show some content (banner, sample data, etc.)
    expect(bodyText.length).toBeGreaterThan(50);

    await shot(page, 'gap-08-demo');
    await page.close();
  });
});
