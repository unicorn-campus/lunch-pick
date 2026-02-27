import request from 'supertest';

const BASE_URL = process.env.MEMBER_SERVICE_URL || 'http://localhost:8081';

/**
 * 회원 서비스 - 취향 온보딩 (UFR-MBR-020)
 *
 * 설계 출처: docs/design/sequence/inner/member-service-취향온보딩.puml
 * API 명세: docs/design/api/member-service-api.yaml
 *   - POST /api/v1/members/onboarding
 *   - PUT  /api/v1/members/onboarding/progress
 *   - GET  /api/v1/members/onboarding/progress (시퀀스 내 getProgress)
 *
 * 이 파일은 직접 실행하지 않으며, 백엔드 구현 시 행위 참고 자료로만 활용된다.
 * alt/else 분기 수와 it() 케이스 수는 1:1 대응한다.
 */
describe('회원 서비스 - 취향 온보딩 (UFR-MBR-020)', () => {

  /** Authorization 헤더에 사용할 테스트용 Bearer 토큰 */
  const BEARER_TOKEN = 'Bearer test-jwt-token';

  /**
   * == 온보딩 퀴즈 결과 제출 ==
   *
   * Controller → OnboardingService: submitOnboarding(memberId, swipeResults[])
   * OnboardingService: 최소 스와이프 수 검증 (7장 이상 완료 여부 확인)
   */
  describe('온보딩 퀴즈 결과 제출', () => {

    /**
     * alt — 7장 미만
     *
     * OnboardingService → Controller: ValidationException
     *   {code: INSUFFICIENT_SWIPES, message: "조금만 더!"}
     * → 400 Bad Request
     */
    it('7장 미만 스와이프 시 400 Bad Request와 INSUFFICIENT_SWIPES를 반환해야 한다', async () => {
      const res = await request(BASE_URL)
        .post('/api/v1/members/onboarding')
        .set('Authorization', BEARER_TOKEN)
        .send({
          swipeResults: [
            { cardId: 'card-korean-001', liked: true, category: '한식' },
            { cardId: 'card-japanese-001', liked: true, category: '일식' },
            { cardId: 'card-chinese-001', liked: false, category: '중식' },
          ],
          healthInfoConsentGiven: true,
        })
        .expect(400);

      expect(res.body).toHaveProperty('error');
      expect(res.body).toHaveProperty('message');
      expect(res.body).toHaveProperty('timestamp');
      expect(res.body.error).toBe('INSUFFICIENT_SWIPES');
    });

    /**
     * else — 7장 이상 완료
     *
     * OnboardingService → TasteVectorService: buildInitialVector(swipeResults[])
     *   TasteVectorService: 좋아요/싫어요 카테고리 추출 → 취향 벡터 생성 → Top 3 산출
     *   → TasteVector {카테고리 가중치 맵, 선호 Top 3}
     * OnboardingService → MemberRepo: 취향 벡터 저장
     *   (UPDATE 회원 취향 벡터 {memberId, tasteVector JSON, 온보딩 완료 시각})
     * OnboardingService → OnboardingRepo: 온보딩 상세 기록 저장
     *   (INSERT 온보딩 결과 {memberId, 카드별 스와이프 결과, 완료 시각})
     * → Controller: 200 OK, OnboardingResult {선호 카테고리 Top 3, onboardingCompleted: true}
     */
    it('7장 이상 완료 시 200 OK와 선호 카테고리 Top 3를 반환해야 한다', async () => {
      const res = await request(BASE_URL)
        .post('/api/v1/members/onboarding')
        .set('Authorization', BEARER_TOKEN)
        .send({
          swipeResults: [
            { cardId: 'card-korean-001', liked: true, category: '한식' },
            { cardId: 'card-japanese-001', liked: true, category: '일식' },
            { cardId: 'card-chinese-001', liked: false, category: '중식' },
            { cardId: 'card-western-001', liked: true, category: '양식' },
            { cardId: 'card-bunsik-001', liked: true, category: '분식' },
            { cardId: 'card-salad-001', liked: true, category: '샐러드/건강식' },
            { cardId: 'card-fastfood-001', liked: false, category: '패스트푸드' },
          ],
          healthInfoConsentGiven: true,
        })
        .expect(200);

      expect(res.body).toHaveProperty('message');
      expect(res.body).toHaveProperty('topCategories');
      expect(res.body).toHaveProperty('tasteVectorCreated');
      expect(res.body.tasteVectorCreated).toBe(true);
      expect(Array.isArray(res.body.topCategories)).toBe(true);
    });
  });

  /**
   * == 온보딩 진행 상태 임시 저장 (중간 이탈 시) ==
   *
   * Controller → OnboardingService: saveProgress(memberId, partialSwipeResults[])
   * OnboardingService → OnboardingRepo: 진행 상태 임시 저장
   *   (UPSERT 온보딩 진행 상태 {memberId, 현재까지 스와이프 결과, 저장 시각})
   * → Controller: 200 OK, ProgressSaved {savedCount: N, remainingCount: M}
   *
   * 시퀀스 내 이 섹션은 alt/else 분기 없이 단일 흐름이므로 it() 케이스 1개
   */
  describe('온보딩 진행 상태 임시 저장 (중간 이탈 시)', () => {

    it('진행 상태 임시 저장 요청 시 200 OK와 savedCount를 반환해야 한다', async () => {
      const res = await request(BASE_URL)
        .put('/api/v1/members/onboarding/progress')
        .set('Authorization', BEARER_TOKEN)
        .send({
          swipeResults: [
            { cardId: 'card-korean-001', liked: true, category: '한식' },
            { cardId: 'card-japanese-001', liked: true, category: '일식' },
            { cardId: 'card-chinese-001', liked: false, category: '중식' },
            { cardId: 'card-western-001', liked: true, category: '양식' },
          ],
        })
        .expect(200);

      expect(res.body).toHaveProperty('savedCount');
      expect(res.body).toHaveProperty('message');
    });
  });

  /**
   * == 온보딩 진행 상태 조회 (재진입 시) ==
   *
   * Controller → OnboardingService: getProgress(memberId)
   * OnboardingService → OnboardingRepo: 진행 상태 조회
   *   (SELECT 온보딩 진행 상태 WHERE memberId)
   * → Controller: OnboardingProgress {완료된 카드 목록, 남은 카드 목록}
   *
   * 시퀀스 내 이 섹션은 alt/else 분기 없이 단일 흐름이므로 it() 케이스 1개
   */
  describe('온보딩 진행 상태 조회 (재진입 시)', () => {

    it('진행 상태 조회 요청 시 200 OK와 완료/남은 카드 목록을 반환해야 한다', async () => {
      const res = await request(BASE_URL)
        .get('/api/v1/members/onboarding/progress')
        .set('Authorization', BEARER_TOKEN)
        .expect(200);

      expect(res.body).toHaveProperty('completedCards');
      expect(res.body).toHaveProperty('remainingCards');
    });
  });
});
