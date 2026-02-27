/* ============================================
   런치픽(LunchPick) 프로토타입 공통 JavaScript
   샘플 데이터, 화면 전환, Web Components
   ============================================ */

/* --- Sample Data --- */
const SAMPLE_DATA = {
  user: {
    id: 'user-001',
    name: '준혁',
    nickname: '준혁',
    email: 'junhyuk@example.com',
    plan: 'free', // 'free' | 'premium'
    feedbackCount: 12,
    onboardingComplete: true,
    dietaryRestrictions: [],
    allergyItems: [],
    locationConsent: true,
    currentLocation: '강남역 근처',
    joinDate: '2026-01-27'
  },
  recommendations: [
    {
      id: 'rec-001',
      rank: 1,
      restaurantName: '미소된장',
      category: '한식',
      categoryColor: 'var(--color-cat-korean)',
      mainMenu: '된장찌개 정식',
      price: 8500,
      score: 87,
      distance: '350m',
      walkTime: '5분',
      reason: '비 오는 날 따뜻한 국물 추천',
      reasonDetail: '비 오는 날 + 어제 양식을 드셨으니 따뜻한 한식 국물을 추천했어요',
      contextTags: ['🌧️ 날씨', '📋 어제 이력', '❤️ 취향'],
      feedbackApplied: true,
      address: '서울 강남구 테헤란로 123'
    },
    {
      id: 'rec-002',
      rank: 2,
      restaurantName: '봉주르 파스타',
      category: '양식',
      categoryColor: 'var(--color-cat-western)',
      mainMenu: '크림 파스타',
      price: 11000,
      score: 72,
      distance: '550m',
      walkTime: '8분',
      reason: '이번 주 한식 많았으니 양식',
      reasonDetail: '이번 주 4일 중 3일 한식이었으니 기분 전환으로 양식을 추천했어요',
      contextTags: ['📋 이번주 이력', '❤️ 취향'],
      feedbackApplied: false,
      address: '서울 강남구 역삼로 45'
    },
    {
      id: 'rec-003',
      rank: 3,
      restaurantName: '홍콩반점',
      category: '중식',
      categoryColor: 'var(--color-cat-chinese)',
      mainMenu: '짬뽕',
      price: 9000,
      score: 65,
      distance: '200m',
      walkTime: '3분',
      reason: '가까운 곳에서 빠르게',
      reasonDetail: '남은 점심시간이 40분이라 가까운 곳을 우선했어요. 짬뽕은 평소 선호 카테고리예요',
      contextTags: ['⏰ 시간', '📍 거리'],
      feedbackApplied: false,
      address: '서울 강남구 테헤란로 88'
    }
  ],
  onboardingFoods: [
    { id: 1, name: '김치찌개', emoji: '🍲', tags: '#한식 #국물 #매운맛' },
    { id: 2, name: '크림 파스타', emoji: '🍝', tags: '#양식 #면 #크리미' },
    { id: 3, name: '초밥', emoji: '🍣', tags: '#일식 #생선 #담백' },
    { id: 4, name: '짜장면', emoji: '🍜', tags: '#중식 #면 #달콤' },
    { id: 5, name: '샐러드', emoji: '🥗', tags: '#건강식 #가벼운 #채소' },
    { id: 6, name: '치킨', emoji: '🍗', tags: '#튀김 #고소한 #간식' },
    { id: 7, name: '부대찌개', emoji: '🫕', tags: '#한식 #국물 #얼큰' },
    { id: 8, name: '피자', emoji: '🍕', tags: '#양식 #치즈 #든든' },
    { id: 9, name: '쌀국수', emoji: '🍜', tags: '#아시안 #면 #시원' },
    { id: 10, name: '불고기', emoji: '🥩', tags: '#한식 #고기 #달콤' }
  ],
  allergens: ['땅콩', '갑각류', '우유', '밀', '달걀', '대두', '생선', '조개류'],
  dietTypes: ['일반', '채식', '비건', '할랄', '기타'],
  rejectReasons: ['오늘 기분 아님', '너무 멀어요', '최근에 갔어요', '기타'],
  feedbackKeywords: ['맛있었어요', '양 적당', '빨리 나왔어요'],
  mealHistory: [
    { date: '2026-02-01', restaurant: '미소된장', category: 'korean', feedback: 'good' },
    { date: '2026-02-03', restaurant: '봉주르 파스타', category: 'western', feedback: 'good' },
    { date: '2026-02-04', restaurant: '홍콩반점', category: 'chinese', feedback: 'bad' },
    { date: '2026-02-05', restaurant: '미소된장', category: 'korean', feedback: 'good' },
    { date: '2026-02-06', restaurant: '스시히로', category: 'japanese', feedback: 'good' },
    { date: '2026-02-07', restaurant: '미소된장', category: 'korean', feedback: 'good' },
    { date: '2026-02-10', restaurant: '봉주르 파스타', category: 'western', feedback: 'good' },
    { date: '2026-02-11', restaurant: '미소된장', category: 'korean', feedback: 'good' },
    { date: '2026-02-12', restaurant: '미소된장', category: 'korean', feedback: 'good' },
    { date: '2026-02-13', restaurant: '홍콩반점', category: 'chinese', feedback: 'good' },
    { date: '2026-02-14', restaurant: '봉주르 파스타', category: 'western', feedback: 'bad' },
    { date: '2026-02-17', restaurant: '미소된장', category: 'korean', feedback: 'good' }
  ],
  insights: {
    topCategories: [
      { name: '한식', percent: 62 },
      { name: '양식', percent: 25 },
      { name: '중식', percent: 8 },
      { name: '일식', percent: 3 },
      { name: '기타', percent: 2 }
    ],
    weeklyPattern: '한식을 가장 좋아하시네요! 이번 주 4일 연속 국물 메뉴였어요',
    satisfactionAvg: 4.2,
    satisfactionTrend: 'up',
    totalMeals: 12,
    accuracyImprovement: 42
  },
  plans: {
    free: {
      name: '무료',
      price: 0,
      features: ['추천 3개/일', '이력 30일', '기본 인사이트']
    },
    premium: {
      name: '프리미엄',
      priceMonthly: 4900,
      priceYearly: 3900,
      features: ['추천 3개/일', '이력 무제한 ✓', '고급 인사이트 ✓', '우선 학습 ✓']
    }
  }
};

/* --- Navigation Utility --- */
function navigateTo(filename, data) {
  if (data) {
    localStorage.setItem('lunchpick_page_data', JSON.stringify(data));
  }
  window.location.href = filename;
}

function getPageData() {
  const data = localStorage.getItem('lunchpick_page_data');
  return data ? JSON.parse(data) : null;
}

function clearPageData() {
  localStorage.removeItem('lunchpick_page_data');
}

/* --- Form Auto Save --- */
function saveFormData(formId) {
  const form = document.getElementById(formId);
  if (!form) return;
  const data = {};
  const inputs = form.querySelectorAll('input, select, textarea');
  inputs.forEach(input => {
    if (input.type === 'checkbox' || input.type === 'radio') {
      data[input.name || input.id] = input.checked;
    } else {
      data[input.name || input.id] = input.value;
    }
  });
  localStorage.setItem('lunchpick_form_' + formId, JSON.stringify(data));
}

function restoreFormData(formId) {
  const saved = localStorage.getItem('lunchpick_form_' + formId);
  if (!saved) return;
  const data = JSON.parse(saved);
  const form = document.getElementById(formId);
  if (!form) return;
  Object.entries(data).forEach(([key, value]) => {
    const input = form.querySelector('[name="' + key + '"], #' + key);
    if (!input) return;
    if (input.type === 'checkbox' || input.type === 'radio') {
      input.checked = value;
    } else {
      input.value = value;
    }
  });
}

/* --- Toast --- */
function showToast(message, duration) {
  duration = duration || 3000;
  let toast = document.getElementById('global-toast');
  if (!toast) {
    toast = document.createElement('div');
    toast.id = 'global-toast';
    toast.className = 'toast';
    toast.setAttribute('role', 'alert');
    toast.setAttribute('aria-live', 'polite');
    document.body.appendChild(toast);
  }
  toast.textContent = message;
  toast.classList.add('show');
  setTimeout(function() {
    toast.classList.remove('show');
  }, duration);
}

/* --- Format Price --- */
function formatPrice(num) {
  return num.toLocaleString('ko-KR') + '원';
}

/* ============================================
   Web Components
   ============================================ */

/* --- App Header --- */
class AppHeader extends HTMLElement {
  connectedCallback() {
    const title = this.getAttribute('title') || '';
    const showBack = this.hasAttribute('back');
    const backPage = this.getAttribute('back') || '';
    const showLocation = this.hasAttribute('location');
    const showNotification = this.hasAttribute('notification');

    this.innerHTML = '\
      <header class="app-header" role="banner">\
        <div class="app-header__inner">\
          ' + (showBack ? '<button class="btn-icon app-header__back" aria-label="뒤로 가기" onclick="' + (backPage ? "navigateTo('" + backPage + "')" : 'history.back()') + '">←</button>' : '') + '\
          ' + (showLocation ? '<div class="app-header__location"><span class="app-header__location-icon">📍</span> <span>' + (SAMPLE_DATA.user.currentLocation) + '</span></div>' : '') + '\
          ' + (title ? '<div class="app-header__title">' + title + '</div>' : '') + '\
          <div class="app-header__spacer"></div>\
          ' + (showNotification ? '<button class="btn-icon app-header__notification" aria-label="알림">🔔</button>' : '') + '\
        </div>\
      </header>';
  }
}

/* --- App Bottom Nav --- */
class AppBottomNav extends HTMLElement {
  connectedCallback() {
    const active = this.getAttribute('active') || 'home';
    var tabs = [
      { id: 'home', label: '홈', icon: '🏠', page: '05-홈.html' },
      { id: 'history', label: '이력', icon: '📋', page: '08-인사이트.html' },
      { id: 'insight', label: '인사이트', icon: '📊', page: '08-인사이트.html' },
      { id: 'profile', label: '프로필', icon: '👤', page: '09-프로필설정.html' }
    ];

    var tabsHtml = tabs.map(function(tab) {
      var isActive = tab.id === active;
      return '<button class="bottom-nav__tab' + (isActive ? ' bottom-nav__tab--active' : '') + '" ' +
        'onclick="navigateTo(\'' + tab.page + '\')" ' +
        'aria-label="' + tab.label + '" ' +
        (isActive ? 'aria-current="page"' : '') + '>' +
        '<span class="bottom-nav__icon">' + tab.icon + '</span>' +
        '<span class="bottom-nav__label">' + tab.label + '</span>' +
        '</button>';
    }).join('');

    this.innerHTML = '\
      <nav class="bottom-nav" role="navigation" aria-label="주 메뉴">\
        ' + tabsHtml + '\
      </nav>';
  }
}

/* --- Register Web Components --- */
customElements.define('app-header', AppHeader);
customElements.define('app-bottom-nav', AppBottomNav);

/* --- Header / Bottom Nav Styles (injected) --- */
(function() {
  var style = document.createElement('style');
  style.textContent = '\
    .app-header { \
      position: sticky; top: 0; z-index: 10; \
      background: var(--color-surface); \
      border-bottom: 1px solid #E5E7EB; \
    } \
    .app-header__inner { \
      display: flex; align-items: center; \
      height: 56px; padding: 0 var(--space-m); \
      max-width: 480px; margin: 0 auto; \
    } \
    .app-header__back { font-size: 20px; margin-right: var(--space-s); } \
    .app-header__location { \
      display: flex; align-items: center; gap: var(--space-xs); \
      font-size: var(--font-size-body2); font-weight: var(--font-weight-medium); \
    } \
    .app-header__title { \
      font-size: var(--font-size-h3); font-weight: var(--font-weight-semibold); \
    } \
    .app-header__spacer { flex: 1; } \
    .app-header__notification { font-size: 20px; } \
    \
    .bottom-nav { \
      position: fixed; bottom: 0; left: 0; right: 0; \
      display: flex; justify-content: space-around; \
      height: 56px; background: var(--color-surface); \
      border-top: 1px solid #E5E7EB; z-index: 10; \
      max-width: 480px; margin: 0 auto; \
    } \
    .bottom-nav__tab { \
      flex: 1; display: flex; flex-direction: column; \
      align-items: center; justify-content: center; gap: 2px; \
      background: none; border: none; cursor: pointer; \
      font-family: var(--font-family); \
      color: var(--color-text-secondary); \
      font-size: 10px; \
    } \
    .bottom-nav__tab--active { color: var(--color-primary); } \
    .bottom-nav__icon { font-size: 20px; } \
    .bottom-nav__label { font-size: 10px; } \
  ';
  document.head.appendChild(style);
})();
