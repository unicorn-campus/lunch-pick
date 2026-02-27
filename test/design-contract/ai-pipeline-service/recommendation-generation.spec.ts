/**
 * 설계 계약 테스트 - AI Pipeline 서비스 추천 생성 (UFR-REC-010, UFR-REC-030)
 *
 * 원본 시퀀스: docs/design/sequence/inner/ai-pipeline-service-추천생성.puml
 * API 명세:   docs/design/api/ai-pipeline-api.yaml
 *
 * 이 파일은 직접 실행하지 않으며 백엔드 구현 시 행위 참고 자료로 활용된다.
 * 핵심 패턴:
 *   - Cache-Aside: 캐시 히트 시 LLM 호출 없이 즉시 반환
 *   - Circuit Breaker: 연속 5회 실패 시 Open → 폴백 즉시 반환
 *   - Retry: 지수 백오프(500ms→1초→2초) 최대 3회 (LLM 오류에만 적용)
 */

import request from 'supertest';

const BASE_URL = process.env.AI_PIPELINE_SERVICE_URL || 'http://localhost:8084';

describe('AI Pipeline 서비스 - 추천 생성 (UFR-REC-010, UFR-REC-030)', () => {

  // ── 공통 픽스처 ────────────────────────────────────────────────────────
  const normalUserRequest = {
    memberId: '550e8400-e29b-41d4-a716-446655440001',
    latitude: 37.5665,
    longitude: 126.9780,
    requestedAt: '2026-02-26T12:00:00Z',
    isColdStart: false,
    feedbackCount: 12,
    tasteVector: {
      한식: 0.85,
      일식: 0.70,
      중식: 0.40,
      양식: 0.55,
      분식: 0.60,
      '샐러드/건강식': 0.65,
    },
    onboardingSwipes: null,
    allergenFilter: ['땅콩', '새우'],
    dietType: '일반',
    weather: {
      condition: 'RAINY',
      temperatureCelsius: 8.5,
      description: '비 오는 날',
    },
    recentMealHistory: [
      { restaurantId: 'rest-007', category: '양식', mealDate: '2026-02-25', satisfaction: 'GOOD' },
    ],
    excludeRestaurantIds: ['rest-005', 'rest-006'],
    jobCluster: null,
  };

  const coldStartUserRequest = {
    memberId: '550e8400-e29b-41d4-a716-446655440002',
    latitude: 37.5665,
    longitude: 126.9780,
    requestedAt: '2026-02-26T12:00:00Z',
    isColdStart: true,
    feedbackCount: 2,
    tasteVector: null,
    onboardingSwipes: [
      { cardId: 'card-korean-001', category: '한식', liked: true },
      { cardId: 'card-japanese-001', category: '일식', liked: true },
      { cardId: 'card-fastfood-001', category: '패스트푸드', liked: false },
    ],
    allergenFilter: [],
    dietType: '일반',
    weather: { condition: 'CLEAR', temperatureCelsius: 15.0, description: '맑은 날' },
    recentMealHistory: [],
    excludeRestaurantIds: [],
    jobCluster: 'IT_OFFICE_WORKER',
  };

  // ── 추천 생성 요청 수신 ─────────────────────────────────────────────────
  describe('추천 생성 요청 수신', () => {

    describe('캐시 조회 (rec:{memberId}:{locationGrid}:{weatherCode}:{weekday})', () => {

      it('캐시 히트 시 200과 캐시된 추천 결과를 즉시 반환한다', async () => {
        // Redis 캐시에 해당 키가 존재하는 시나리오 → LLM 호출 없이 응답
        const res = await request(BASE_URL)
          .post('/api/v1/ai/recommendations')
          .send(normalUserRequest);

        expect(res.status).toBe(200);
        expect(res.body).toHaveProperty('recommendations');
        expect(res.body).toHaveProperty('isFallback');
        expect(res.body).toHaveProperty('isColdStart');
        expect(res.body).toHaveProperty('cacheKey');
        expect(res.body).toHaveProperty('metadata');
        expect(res.body.metadata).toHaveProperty('source', 'CACHE');
      });

      describe('캐시 미스', () => {

        // ── 콜드스타트 판별 ─────────────────────────────────────────────
        describe('콜드스타트 판별', () => {

          it('콜드스타트 (피드백 5건 미만) 시 온보딩 데이터 + Bayesian Prior 기반 프롬프트를 사용하여 추천을 생성한다', async () => {
            const res = await request(BASE_URL)
              .post('/api/v1/ai/recommendations')
              .send(coldStartUserRequest);

            expect(res.status).toBe(200);
            expect(res.body).toHaveProperty('isColdStart', true);
            expect(res.body).toHaveProperty('coldStartTag');
            expect(res.body.coldStartTag).not.toBeNull();
            expect(res.body).toHaveProperty('recommendations');
            expect(res.body).toHaveProperty('metadata');
            expect(res.body.metadata).toHaveProperty('source', 'COLD_START_LLM');
          });

          it('정상 사용자 (피드백 5건 이상) 시 취향 벡터 + 컨텍스트 기반 개인화 프롬프트를 사용하여 추천을 생성한다', async () => {
            const res = await request(BASE_URL)
              .post('/api/v1/ai/recommendations')
              .send(normalUserRequest);

            expect(res.status).toBe(200);
            expect(res.body).toHaveProperty('isColdStart', false);
            expect(res.body.coldStartTag).toBeNull();
            expect(res.body).toHaveProperty('recommendations');
            expect(res.body).toHaveProperty('metadata');
            expect(res.body.metadata).toHaveProperty('source', 'LLM');
          });

        });

        // ── LLM 호출 ────────────────────────────────────────────────────
        describe('LLM 호출', () => {

          describe('Circuit Breaker 상태 확인', () => {

            describe('Circuit Breaker Closed (정상)', () => {

              describe('LLM API 호출 결과', () => {

                it('LLM 정상 응답 시 200과 파싱된 추천 3개 및 메타데이터를 반환한다', async () => {
                  const res = await request(BASE_URL)
                    .post('/api/v1/ai/recommendations')
                    .send(normalUserRequest);

                  expect(res.status).toBe(200);
                  expect(res.body).toHaveProperty('recommendations');
                  expect(Array.isArray(res.body.recommendations)).toBe(true);
                  expect(res.body.recommendations.length).toBeGreaterThanOrEqual(1);
                  expect(res.body.recommendations.length).toBeLessThanOrEqual(3);

                  const rec = res.body.recommendations[0];
                  expect(rec).toHaveProperty('restaurantId');
                  expect(rec).toHaveProperty('restaurantName');
                  expect(rec).toHaveProperty('representativeMenu');
                  expect(rec).toHaveProperty('category');
                  expect(rec).toHaveProperty('reasonSummary');
                  expect(rec).toHaveProperty('confidenceScore');
                  expect(rec).toHaveProperty('distanceMeters');
                  expect(rec).toHaveProperty('estimatedWalkMinutes');

                  expect(res.body).toHaveProperty('isFallback', false);
                  expect(res.body).toHaveProperty('cacheKey');
                  expect(res.body).toHaveProperty('cachedUntil');
                  expect(res.body).toHaveProperty('metadata');
                  expect(res.body.metadata).toHaveProperty('source', 'LLM');
                  expect(res.body.metadata).toHaveProperty('model_used');
                  expect(res.body.metadata).toHaveProperty('latency_ms');
                  expect(res.body.metadata).toHaveProperty('token_usage');
                  expect(res.body.metadata).toHaveProperty('circuit_breaker_state', 'CLOSED');
                });

                describe('LLM 오류 (Retry 적용: 지수 백오프 500ms→1초→2초 최대 3회)', () => {

                  it('재시도 성공 시 200과 LLM 추천 결과를 반환한다', async () => {
                    // 503/408/429 오류 발생 후 재시도 중 성공하는 시나리오
                    const res = await request(BASE_URL)
                      .post('/api/v1/ai/recommendations')
                      .send(normalUserRequest);

                    expect(res.status).toBe(200);
                    expect(res.body).toHaveProperty('recommendations');
                    expect(res.body).toHaveProperty('metadata');
                    expect(res.body.metadata).toHaveProperty('source', 'LLM');
                  });

                  it('재시도 모두 실패 시 규칙 기반 폴백 추천 3개를 반환하고 isFallback이 true이다', async () => {
                    // 3회 재시도 모두 실패 → Circuit Breaker 실패 카운트 증가 → FallbackEngine 호출
                    const res = await request(BASE_URL)
                      .post('/api/v1/ai/recommendations')
                      .send(normalUserRequest);

                    expect(res.status).toBe(200);
                    expect(res.body).toHaveProperty('isFallback', true);
                    expect(res.body).toHaveProperty('recommendations');
                    expect(Array.isArray(res.body.recommendations)).toBe(true);
                    expect(res.body).toHaveProperty('metadata');
                    expect(res.body.metadata).toHaveProperty('source', 'FALLBACK_RULE_BASED');
                  });

                });
              });

            });

            describe('Circuit Breaker Open (즉시 폴백)', () => {

              it('만료 캐시 존재 시 만료 캐시 기반 추천을 반환하고 isFallback이 true, source가 STALE_CACHE이다', async () => {
                // Circuit Breaker Open 상태 + 만료 캐시 존재 시나리오
                const res = await request(BASE_URL)
                  .post('/api/v1/ai/recommendations')
                  .send(normalUserRequest);

                expect(res.status).toBe(200);
                expect(res.body).toHaveProperty('isFallback', true);
                expect(res.body).toHaveProperty('recommendations');
                expect(res.body).toHaveProperty('metadata');
                expect(res.body.metadata).toHaveProperty('source', 'STALE_CACHE');
                expect(res.body.metadata).toHaveProperty('circuit_breaker_state', 'OPEN');
                expect(res.body.cachedUntil).toBeNull();
              });

              it('캐시 없음 시 규칙 기반 폴백 추천을 반환하고 isFallback이 true, source가 FALLBACK_RULE_BASED이다', async () => {
                // Circuit Breaker Open 상태 + 만료 캐시도 없는 시나리오
                const res = await request(BASE_URL)
                  .post('/api/v1/ai/recommendations')
                  .send(normalUserRequest);

                expect(res.status).toBe(200);
                expect(res.body).toHaveProperty('isFallback', true);
                expect(res.body).toHaveProperty('recommendations');
                expect(res.body).toHaveProperty('metadata');
                expect(res.body.metadata).toHaveProperty('source', 'FALLBACK_RULE_BASED');
                expect(res.body.metadata).toHaveProperty('circuit_breaker_state', 'OPEN');
              });

            });

          });
        });

      });
    });

  });

});
