# LunchPick (런치픽)

> AI 기반 점심 메뉴 추천 서비스 — 매일 반복되는 점심 메뉴 의사결정 피로를 3초 이내 개인화 추천으로 해결합니다.

## 주요 기능

- **AI 개인화 추천**: 취향 벡터 + 위치 + 날씨 + 요일 컨텍스트 기반 맞춤 추천 3개 제시
- **투명한 추천 근거**: "비 오는 날 + 어제 양식 → 따뜻한 한식 추천" 형태의 자연어 이유 제공
- **원탭 식사 기록**: "먹었어요" 1탭으로 식사 기록 완료, 피드백 루프 마찰 제거
- **취향 학습 플라이휠**: 쌓인 데이터가 추천 품질을 높이고, 높아진 품질이 더 많은 피드백을 유도
- **카카오 소셜 로그인**: 간편 인증 + 취향 온보딩 퀴즈로 빠른 시작
- **구독 결제**: 프리미엄 플랜 구독, 7일 무료 체험 연장
- **인사이트 리포트**: 30일 식사 이력 타임라인 + 취향 분석 리포트

## 기술 스택

| 영역 | 기술 |
|------|------|
| 백엔드 | Java 21, Spring Boot 3.4.x, Gradle |
| 프론트엔드 | TypeScript 5.x, React 19, Next.js 15, Tailwind CSS 4.x, TanStack Query 5.x |
| AI 서비스 | Python 3.12, FastAPI 0.115.x, LangChain, Anthropic/OpenAI SDK |
| 데이터베이스 | PostgreSQL 16 |
| 캐시 | Redis 7.x (Cache + Redis Streams MQ) |
| 컨테이너 | Docker Compose |

## 시작하기

### 로컬 수행

#### 사전 요구사항

- Java 21 (JDK)
- Node.js 20+ & npm
- Docker & Docker Compose
- Python 3.12+ (AI 서비스)

#### 실행

```bash
# 1. 환경변수 설정
cp .env.example .env
# .env 파일을 열어 필요한 값을 수정합니다

# 2. 백킹서비스 기동
docker compose up -d

# 3. 백엔드 서비스 기동
python3 tools/run-intellij-service-profile.py --config-dir . --delay 5

# 4. 프론트엔드 기동
cd frontend && npm run dev

# 5. AI 서비스 기동
docker compose --profile ai up -d
```

#### 중지

```bash
# 백킹서비스 + AI 서비스 중지
docker compose down

# 백엔드 서비스 중지
python3 tools/run-intellij-service-profile.py --stop

# 프론트엔드 중지
# 실행 중인 터미널에서 Ctrl+C
```

## 테스트 결과

| 항목 | 결과 |
|------|------|
| 전체 TC | 32개 PASS / 32개 |
| 브라우저 E2E (자동화) | PASS (Playwright Test Suite, 2분 42초) |
| PO 제품 검증 | CONDITIONAL PASS → 실제 버그 수정 후 PASS |
| SP UI/UX 검증 | CONDITIONAL PASS → 실제 버그 수정 후 PASS |
| 회귀 테스트 | PASS (32/32, 신규 버그 0건) |

**상세 보고서**: [Final Report](docs/develop/test/final-report.md)

### Known Improvements

| # | 관점 | 설명 |
|---|------|------|
| 1 | PO | 퀴즈 완료 시 Top 3 취향 카테고리 즉시 표시 |
| 2 | PO/SP | 구독 플랜 무료/프리미엄 2열 비교 레이아웃 |
| 3 | SP | 추천 이유 바텀시트 복수 컨텍스트 태그 표시 |
| 4 | SP | 바텀시트 명시적 닫기(X) 버튼 추가 |
| 5 | SP | 이력 달력 카테고리 도트 크기 확대 (8px+) |

> 상세 테스트 레포트: [docs/develop/test/](docs/develop/test/)

## 라이선스

This project is proprietary and confidential.
