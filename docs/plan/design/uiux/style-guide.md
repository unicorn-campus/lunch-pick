# 스타일 가이드

**서비스**: 런치픽(LunchPick)
**작성일**: 2026-02-26
**데이터 출처**: uiux.md, 핵심솔루션.md

---

## 컬러 팔레트

### 브랜드 컬러

```
Primary Color:   #FF6B35 - 따뜻한 오렌지. 식욕을 자극하면서 신뢰감을 주는 핵심 브랜드 컬러
Secondary Color: #1A1A2E - 딥 네이비. 고급스러움과 안정감을 부여하는 보조 컬러
Accent Color:    #4ECDC4 - 민트 그린. 긍정적 피드백과 성공 상태에 사용하는 강조 컬러
```

### 텍스트 컬러

```
Primary Text:    #1A1A2E - 본문, 제목 등 주요 텍스트
Secondary Text:  #6B7280 - 보조 설명, 캡션, 비활성 라벨
Disabled Text:   #9CA3AF - 비활성 상태 텍스트
Inverse Text:    #FFFFFF - 어두운 배경 위의 텍스트
```

### 배경

```
Background:       #FAFAFA - 기본 배경 (Light Gray)
Surface:          #FFFFFF - 카드, 바텀시트 등 표면
Surface Elevated: #FFFFFF - 그림자 있는 표면 (카드)
Overlay:          rgba(0,0,0,0.40) - 모달/바텀시트 오버레이
```

### 상태 컬러

```
Success:  #10B981 - 성공, 완료, 긍정적 피드백 (좋아요)
Warning:  #F59E0B - 경고, 주의, 콜드스타트 안내
Error:    #EF4444 - 에러, 실패, 부정적 피드백 (별로)
Info:     #3B82F6 - 정보, 안내, 학습 반영 태그
```

### 확장 컬러 (카테고리 태그)

```
한식: #FF6B35 (Primary)
양식: #3B82F6 (Blue)
중식: #10B981 (Green)
일식: #8B5CF6 (Purple)
기타: #6B7280 (Gray)
```

### 확신 스코어 컬러

```
High (80%+):    #FF6B35 (Primary) - 높은 확신
Medium (60-79%): #F59E0B (Warning) - 보통 확신
Low (<60%):     #6B7280 (Gray) - 낮은 확신
```

---

## 타이포그래피

### 폰트 패밀리

```
Font Family: "Pretendard Variable", "Pretendard", -apple-system, BlinkMacSystemFont, system-ui, sans-serif

선정 이유:
- 한글 가독성 우수 (직장인 빠른 스캔에 최적)
- Variable Font 지원 (파일 크기 효율적)
- 무료 오픈소스 (상업적 사용 가능)
- Apple SF Pro / Google Roboto와 조화
```

### 타입 스케일

```
제목:
- H1: 28px, Bold (700), line-height 36px  — 페이지 타이틀
- H2: 24px, Bold (700), line-height 32px  — 섹션 타이틀
- H3: 18px, SemiBold (600), line-height 26px — 카드 타이틀, 식당명

본문:
- Body 1: 16px, Regular (400), line-height 24px — 기본 본문, 추천 이유
- Body 2: 14px, Regular (400), line-height 20px — 보조 본문, 메뉴/가격

기타:
- Caption: 12px, Regular (400), line-height 16px — 거리, 시간, 법적 고지
- Label:   14px, Medium (500), line-height 20px — 버튼 텍스트, 탭 라벨
- Badge:   12px, Bold (700), line-height 16px — 확신 스코어, 태그

letter-spacing: -0.02em (한글 기본)
```

---

## 간격 시스템

### 기본 단위: 4px

```
XS:  4px  — 아이콘-텍스트 사이, 인라인 간격
S:   8px  — 칩 내부 패딩, 태그 간격, 리스트 아이템 간격
M:   16px — 카드 내부 패딩, 섹션 내 요소 간격
L:   24px — 섹션 간 간격, 카드 간 간격
XL:  32px — 큰 섹션 간격, 화면 좌우 마진 (모바일: 16px)
XXL: 48px — 페이지 상단/하단 여백, 주요 섹션 분리
```

### 화면 마진

```
모바일 좌우 마진:  16px
태블릿 좌우 마진:  24px
데스크톱 좌우 마진: 32px (최대 컨텐츠 폭 480px 중앙 정렬)
```

---

## 아이콘

- **아이콘 라이브러리**: Material Symbols (Rounded)
- **크기**:
  - Small: 16px — 인라인 텍스트, 캡션 옆
  - Medium: 24px — 버튼, 네비게이션, 일반 UI
  - Large: 32px — 하단 탭바, 온보딩 가이드
- **스타일**: Rounded (둥근 모서리 — 친근하고 따뜻한 톤)
- **굵기**: 400 (기본), 600 (강조)

### 커스텀 아이콘

```
확신 스코어: ⭐ (filled star) — 확신도 표시
카테고리 태그: 🟠🔵🟢🟣 (색상 도트) — 달력 뷰 카테고리
좋아요/싫어요: 👍👎 — 피드백 (대형 이모지 스타일)
위치: 📍 — 현재 위치 표시
```

---

## 반응형 브레이크포인트

### 브레이크포인트

```
모바일:   < 768px  (기본, Mobile First)
태블릿:   768px - 1024px
데스크톱:  > 1024px
```

### 화면별 레이아웃 전략

#### 모바일 (< 768px) — Primary

- **레이아웃**: 단일 컬럼, 풀 와이드 카드
- **네비게이션**: 하단 탭바 (4탭)
- **추천 카드**: 세로 스택, 카드 간 16px 간격
- **터치 타겟**: 최소 44x44px
- **스와이프**: 추천 카드 좌우 스와이프 활성화
- **좌우 마진**: 16px

#### 태블릿 (768px - 1024px)

- **레이아웃**: 단일 컬럼 (최대폭 600px 중앙 정렬)
- **네비게이션**: 하단 탭바 유지
- **추천 카드**: 세로 스택, 카드 내부 정보 수평 확장
- **터치 타겟**: 최소 44x44px
- **좌우 마진**: 24px

#### 데스크톱 (> 1024px)

- **레이아웃**: 단일 컬럼 (최대폭 480px 중앙 정렬, 모바일 앱 느낌 유지)
- **네비게이션**: 좌측 사이드바 (아이콘 + 라벨)
- **추천 카드**: 세로 스택
- **마우스/키보드 인터랙션**: 호버 효과, 키보드 네비게이션
- **좌우 마진**: 32px

> **설계 원칙**: 런치픽은 Mobile First 서비스로, 핵심 경험은 모바일에서 최적화된다. 태블릿/데스크톱은 모바일 레이아웃을 그대로 중앙 정렬하여 일관된 경험을 제공한다.

---

## 인터랙션 디자인

### 애니메이션 원칙

- **Duration**: 200ms (빠른 반응), 300ms (기본 전환), 500ms (강조 효과)
- **Easing**: `cubic-bezier(0.4, 0, 0.2, 1)` (Material Design 기본)
- **Hover Effects**: 카드 배경 밝기 +5%, 커서 pointer (데스크톱 전용)
- **Click Feedback**: 버튼 스케일 0.97 → 1.0 (200ms), 리플 효과
- **`prefers-reduced-motion`**: 모든 애니메이션 비활성화, 즉시 상태 전환

### 마이크로 인터랙션

1. **추천 카드 스와이프 (거절)**:
   - 왼쪽 스와이프 시 카드가 왼쪽으로 슬라이드 아웃 (300ms)
   - 거절 사유 선택 바텀시트 슬라이드 업 (300ms)
   - 대체 추천 카드 오른쪽에서 슬라이드 인 (300ms)

2. **온보딩 카드 스와이프 (좋아요/싫어요)**:
   - 오른쪽 스와이프: 카드 회전 + 초록 오버레이 페이드인 (300ms)
   - 왼쪽 스와이프: 카드 회전 + 빨간 오버레이 페이드인 (300ms)
   - 다음 카드: 아래에서 스케일 0.95 → 1.0 (200ms)
   - 진행률 바: 부드러운 증가 애니메이션 (200ms)

3. **"먹었어요" 원탭 기록**:
   - 버튼 탭: 체크마크 ✅ 팝 애니메이션 (스케일 0 → 1.2 → 1.0, 500ms)
   - 성공 메시지 슬라이드 다운 (300ms)
   - 실행 취소 바 하단에서 슬라이드 업 (300ms)
   - 30초 카운트다운 프로그레스 바 감소 애니메이션

4. **피드백 제출 (좋아요/별로)**:
   - 탭 시 선택된 이모지 스케일 1.0 → 1.3 → 1.0 바운스 (400ms)
   - 미선택 이모지 투명도 1.0 → 0.4 (200ms)
   - 키워드 칩 등장: 아래에서 페이드인 + 슬라이드업 (300ms, 순차 50ms 딜레이)

5. **확신 스코어 바 로딩**:
   - 바 너비 0% → 목표% (500ms, ease-out)
   - 숫자 카운트업 애니메이션 (500ms)

6. **마일스톤 축하**:
   - 배너 상단에서 슬라이드 다운 (300ms)
   - 컨페티 파티클 효과 (1초)
   - 3초 후 자동 축소 또는 수동 닫기

7. **페이지 전환**:
   - 탭 간 이동: 크로스 페이드 (200ms)
   - 상세 화면 진입: 오른쪽에서 슬라이드 인 (300ms)
   - 바텀시트 열기: 아래에서 슬라이드 업 + 배경 오버레이 (300ms)

8. **로딩 상태**:
   - 추천 로딩: 카드 형태 스켈레톤 UI (펄스 애니메이션 1.5s 반복)
   - 인라인 로딩: 12px 스피너 (회전 800ms 반복)
   - 풀 페이지 로딩: 런치픽 로고 펄스 애니메이션

---

## 그림자 및 Elevation

```
Level 0 (Flat):     none — 배경, 입력 필드
Level 1 (Raised):   0 1px 3px rgba(0,0,0,0.06), 0 1px 2px rgba(0,0,0,0.04) — 비활성 카드
Level 2 (Card):     0 2px 8px rgba(0,0,0,0.08) — 추천 카드, 인사이트 카드
Level 3 (Elevated): 0 4px 16px rgba(0,0,0,0.12) — 바텀시트, 모달
Level 4 (Floating): 0 8px 24px rgba(0,0,0,0.16) — FAB, 토스트
```

---

## 모서리 반경 (Border Radius)

```
XS:  4px  — 칩, 뱃지, 태그
S:   8px  — 입력 필드, 드롭다운
M:   12px — 버튼
L:   16px — 카드
XL:  24px — 바텀시트 상단 모서리
Full: 9999px — 원형 아바타, 원형 버튼
```

---

## 디자인 토큰 요약

| 토큰 카테고리 | 토큰명 | 값 |
|-------------|--------|-----|
| Color | `--color-primary` | #FF6B35 |
| Color | `--color-secondary` | #1A1A2E |
| Color | `--color-accent` | #4ECDC4 |
| Color | `--color-success` | #10B981 |
| Color | `--color-warning` | #F59E0B |
| Color | `--color-error` | #EF4444 |
| Color | `--color-info` | #3B82F6 |
| Color | `--color-bg` | #FAFAFA |
| Color | `--color-surface` | #FFFFFF |
| Color | `--color-text-primary` | #1A1A2E |
| Color | `--color-text-secondary` | #6B7280 |
| Color | `--color-text-disabled` | #9CA3AF |
| Font | `--font-family` | Pretendard Variable |
| Font | `--font-size-h1` | 28px |
| Font | `--font-size-h2` | 24px |
| Font | `--font-size-h3` | 18px |
| Font | `--font-size-body1` | 16px |
| Font | `--font-size-body2` | 14px |
| Font | `--font-size-caption` | 12px |
| Spacing | `--space-xs` | 4px |
| Spacing | `--space-s` | 8px |
| Spacing | `--space-m` | 16px |
| Spacing | `--space-l` | 24px |
| Spacing | `--space-xl` | 32px |
| Spacing | `--space-xxl` | 48px |
| Radius | `--radius-xs` | 4px |
| Radius | `--radius-s` | 8px |
| Radius | `--radius-m` | 12px |
| Radius | `--radius-l` | 16px |
| Radius | `--radius-xl` | 24px |
| Duration | `--duration-fast` | 200ms |
| Duration | `--duration-normal` | 300ms |
| Duration | `--duration-slow` | 500ms |
| Easing | `--easing-standard` | cubic-bezier(0.4, 0, 0.2, 1) |

---

*작성자: 강도윤 (데브-프론트) / 프론트엔드 개발자*
*기반 파일: uiux.md, 핵심솔루션.md*
*작성일: 2026-02-26*
