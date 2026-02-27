/**
 * 설계 계약 테스트 - AI Pipeline 서비스 추천 이유 생성 (UFR-REC-020)
 *
 * 원본 시퀀스: docs/design/sequence/inner/ai-pipeline-service-추천이유생성.puml
 * API 명세:   docs/design/api/ai-pipeline-api.yaml
 *
 * 이 파일은 직접 실행하지 않으며 백엔드 구현 시 행위 참고 자료로 활용된다.
 * 핵심 패턴:
 *   - Cache-Aside: reason:{recommendationId} 캐시 히트 시 LLM 호출 없이 즉시 반환
 *   - Circuit Breaker Open 시 기본 이유(거리/평점) 반환 (폴백)
 *   - LLM 실패 시에도 200 반환 (isReasonReady: false, fallbackReason 포함)
 */

import request from 'supertest';

const BASE_URL = process.env.AI_PIPELINE_SERVICE_URL || 'http://localhost:8084';

describe('AI Pipeline 서비스 - 추천 이유 생성 (UFR-REC-020)', () => {

  // ── 공통 픽스처 ────────────────────────────────────────────────────────
  const reasonRequest = {
    recommendationId: 'rec-550e8400-e29b-41d4-a716-446655440010',
    restaurantId: 'rest-001',
    restaurantName: '광화문 된장마을',
    category: '한식',
    memberId: '550e8400-e29b-41d4-a716-446655440001',
    tasteVector: {
      한식: 0.85,
      일식: 0.70,
      중식: 0.40,
    },
    weather: {
      condition: 'RAINY',
      temperatureCelsius: 8.5,
      description: '비 오는 날',
    },
    recentMealHistory: [
      { restaurantId: 'rest-007', category: '양식', mealDate: '2026-02-25', satisfaction: 'GOOD' },
    ],
    confidenceScore: 87,
  };

  // ── 추천 이유 상세 생성 요청 ─────────────────────────────────────────
  describe('추천 이유 상세 생성 요청', () => {

    describe('캐시 조회 (reason:{recommendationId})', () => {

      it('캐시 히트 시 200과 캐시된 추천 이유를 즉시 반환한다', async () => {
        // Redis reason:{recommendationId} 캐시가 존재하는 시나리오
        const res = await request(BASE_URL)
          .post('/api/v1/ai/recommendation-reason')
          .send(reasonRequest);

        expect(res.status).toBe(200);
        expect(res.body).toHaveProperty('recommendationId');
        expect(res.body).toHaveProperty('naturalLanguageReason');
        expect(res.body).toHaveProperty('confidenceScore');
        expect(res.body).toHaveProperty('contextTags');
        expect(res.body).toHaveProperty('isReasonReady');
        expect(res.body).toHaveProperty('cachedUntil');
        expect(res.body).toHaveProperty('metadata');
        expect(res.body.metadata).toHaveProperty('source', 'CACHE');
      });

      describe('캐시 미스', () => {

        // ── LLM 호출 ────────────────────────────────────────────────────
        describe('LLM 호출', () => {

          describe('Circuit Breaker 상태 확인', () => {

            describe('Circuit Breaker Closed (정상)', () => {

              describe('LLM API 호출 결과', () => {

                it('LLM 정상 응답 시 200과 자연어 이유, 확신 스코어, 컨텍스트 태그를 반환하며 isReasonReady가 true이다', async () => {
                  const res = await request(BASE_URL)
                    .post('/api/v1/ai/recommendation-reason')
                    .send(reasonRequest);

                  expect(res.status).toBe(200);
                  expect(res.body).toHaveProperty('recommendationId', reasonRequest.recommendationId);
                  expect(res.body).toHaveProperty('naturalLanguageReason');
                  expect(typeof res.body.naturalLanguageReason).toBe('string');

                  expect(res.body).toHaveProperty('confidenceScore');
                  expect(res.body.confidenceScore).toBeGreaterThanOrEqual(0);
                  expect(res.body.confidenceScore).toBeLessThanOrEqual(100);

                  expect(res.body).toHaveProperty('contextTags');
                  expect(Array.isArray(res.body.contextTags)).toBe(true);

                  expect(res.body).toHaveProperty('isReasonReady', true);
                  expect(res.body.fallbackReason).toBeNull();

                  expect(res.body).toHaveProperty('cachedUntil');
                  expect(res.body).toHaveProperty('metadata');
                  expect(res.body.metadata).toHaveProperty('source', 'LLM');
                  expect(res.body.metadata).toHaveProperty('model_used');
                  expect(res.body.metadata).toHaveProperty('latency_ms');
                  expect(res.body.metadata).toHaveProperty('token_usage');
                  expect(res.body.metadata).toHaveProperty('circuit_breaker_state', 'CLOSED');
                });

                describe('LLM 오류 (재시도 최대 3회, 지수 백오프)', () => {

                  it('재시도 실패 시 200과 기본 이유(거리/평점)를 반환하며 isReasonReady가 false이다', async () => {
                    // LLM 오류 + 3회 재시도 모두 실패 시나리오
                    // 폴백: "추천 이유를 준비 중이에요" 기본 이유 반환
                    const res = await request(BASE_URL)
                      .post('/api/v1/ai/recommendation-reason')
                      .send(reasonRequest);

                    expect(res.status).toBe(200);
                    expect(res.body).toHaveProperty('isReasonReady', false);
                    expect(res.body).toHaveProperty('fallbackReason');
                    expect(res.body.fallbackReason).not.toBeNull();
                    expect(res.body.naturalLanguageReason).toBeTruthy(); // 거리/평점 기반 기본 이유
                    expect(res.body).toHaveProperty('metadata');
                    expect(res.body.metadata).toHaveProperty('source', 'FALLBACK_RULE_BASED');
                  });

                });
              });

            });

            it('Circuit Breaker Open 시 200과 기본 이유(거리/평점)를 반환하며 isReasonReady가 false이다', async () => {
              // Circuit Breaker Open 상태 → 즉시 폴백: "추천 이유를 준비 중이에요"
              const res = await request(BASE_URL)
                .post('/api/v1/ai/recommendation-reason')
                .send(reasonRequest);

              expect(res.status).toBe(200);
              expect(res.body).toHaveProperty('isReasonReady', false);
              expect(res.body).toHaveProperty('fallbackReason');
              expect(res.body.fallbackReason).not.toBeNull();
              expect(res.body.cachedUntil).toBeNull();
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
