import request from 'supertest';

const BASE_URL = process.env.MEMBER_SERVICE_URL || 'http://localhost:8081';

/**
 * 회원가입-온보딩 (외부 통합 시퀀스)
 *
 * 설계 출처: docs/design/sequence/outer/회원가입-온보딩.puml
 * API 명세: docs/design/api/member-service-api.yaml
 *   - POST /api/v1/auth/kakao                 (UFR-MBR-010)
 *   - POST /api/v1/members/onboarding/taste   (UFR-MBR-020, outer 시퀀스 경로)
 *   - PUT  /api/v1/members/onboarding/progress (UFR-MBR-020, outer 시퀀스 경로)
 *   - POST /api/v1/members/location-consent   (UFR-MBR-030)
 *
 * 이 파일은 직접 실행하지 않으며, 백엔드 구현 시 행위 참고 자료로만 활용된다.
 * alt/else 분기 수와 it() 케이스 수는 1:1 대응한다.
 */
describe('회원가입-온보딩', () => {

  /** Authorization 헤더에 사용할 테스트용 Bearer 토큰 */
  const BEARER_TOKEN = 'Bearer test-jwt-token';

  /**
   * == 소셜 로그인 (UFR-MBR-010) ==
   *
   * User → Frontend → Kakao: OAuth 인증 코드 발급
   * Frontend → Gateway: POST /api/v1/auth/kakao {카카오 인증 코드}
   * Gateway → MemberService: 소셜 로그인 요청 전달
   * MemberService → Kakao: 카카오 토큰 검증 요청 → 사용자 프로필 반환 (이메일, 카카오ID)
   */
  describe('소셜 로그인 (UFR-MBR-010)', () => {

    /**
     * alt — 기존 회원
     *
     * MemberService → MemberDB: 회원 조회 (이메일) → 기존 회원 정보 반환
     * MemberService → Redis: 세션 저장 (TTL: 1시간)
     * MemberService → Gateway: 200 OK {JWT 토큰, 회원 ID, isNewUser: false}
     * Gateway → Frontend: 로그인 성공 응답
     * Frontend → Frontend: 메인 화면으로 이동
     */
    it('기존 회원 시 200 OK와 isNewUser: false를 반환해야 한다', async () => {
      const res = await request(BASE_URL)
        .post('/api/v1/auth/kakao')
        .send({
          authorizationCode: 'EXISTING_USER_AUTH_CODE',
        })
        .expect(200);

      expect(res.body).toHaveProperty('accessToken');
      expect(res.body).toHaveProperty('tokenType');
      expect(res.body).toHaveProperty('expiresIn');
      expect(res.body).toHaveProperty('memberId');
      expect(res.body).toHaveProperty('isNewUser');
      expect(res.body).toHaveProperty('onboardingCompleted');
      expect(res.body.isNewUser).toBe(false);
    });

    /**
     * else — 신규 회원가입
     *
     * MemberService → MemberDB: 신규 회원 생성 (회원 ID, 이메일, 카카오 ID)
     * MemberDB → MemberService: 회원 생성 완료
     * MemberService → Redis: 세션 저장 (TTL: 1시간)
     * MemberService → Gateway: 201 Created {JWT 토큰, 회원 ID, isNewUser: true}
     * Gateway → Frontend: 회원가입 성공 응답
     * Frontend → Frontend: 취향 온보딩 화면으로 이동
     */
    it('신규 회원가입 시 201 Created와 isNewUser: true를 반환해야 한다', async () => {
      const res = await request(BASE_URL)
        .post('/api/v1/auth/kakao')
        .send({
          authorizationCode: 'NEW_USER_AUTH_CODE',
        })
        .expect(201);

      expect(res.body).toHaveProperty('accessToken');
      expect(res.body).toHaveProperty('tokenType');
      expect(res.body).toHaveProperty('expiresIn');
      expect(res.body).toHaveProperty('memberId');
      expect(res.body).toHaveProperty('isNewUser');
      expect(res.body).toHaveProperty('onboardingCompleted');
      expect(res.body.isNewUser).toBe(true);
    });

    /**
     * else — 카카오 인증 실패
     *
     * MemberService → Gateway: 401 Unauthorized {error: "카카오 인증에 실패했어요"}
     * Gateway → Frontend: 인증 실패 응답
     * Frontend → User: "인증에 실패했어요. 다시 시도해주세요" 안내
     */
    it('카카오 인증 실패 시 401 Unauthorized와 에러 메시지를 반환해야 한다', async () => {
      const res = await request(BASE_URL)
        .post('/api/v1/auth/kakao')
        .send({
          authorizationCode: 'INVALID_AUTH_CODE',
        })
        .expect(401);

      expect(res.body).toHaveProperty('error');
      expect(res.body).toHaveProperty('message');
      expect(res.body).toHaveProperty('timestamp');
    });
  });

  /**
   * == 취향 온보딩 퀴즈 (UFR-MBR-020) ==
   *
   * Frontend → User: 음식 카드 10장 표시
   * User → Frontend: 음식 카드 스와이프 (좋아요/싫어요)
   * Frontend → Frontend: 진행률 실시간 표시
   */
  describe('취향 온보딩 퀴즈 (UFR-MBR-020)', () => {

    /**
     * alt — 최소 7장 이상 완료
     *
     * Frontend → Gateway: POST /api/v1/members/onboarding/taste {카드별 스와이프 결과 배열}
     * Gateway → MemberService: 취향 온보딩 저장 요청
     * MemberService → MemberDB: 초기 취향 벡터 저장 (선호/비선호 카테고리)
     * MemberDB → MemberService: 저장 완료
     * MemberService → Gateway: 200 OK {선호 카테고리 Top 3}
     * Gateway → Frontend: 온보딩 완료 응답
     * Frontend → User: "취향 프로파일 완성!" 축하 메시지 + 선호 카테고리 Top 3 표시
     *
     * Note: outer 시퀀스의 엔드포인트는 /api/v1/members/onboarding/taste 이나
     *       API 명세의 실제 경로는 /api/v1/members/onboarding (POST)를 사용한다.
     */
    it('최소 7장 이상 완료 시 200 OK와 선호 카테고리 Top 3를 반환해야 한다', async () => {
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
      expect(Array.isArray(res.body.topCategories)).toBe(true);
      expect(res.body.tasteVectorCreated).toBe(true);
    });

    /**
     * else — 7장 미만 스와이프
     *
     * Frontend → User: "조금만 더!" 안내 메시지
     *
     * Note: 7장 미만은 프론트엔드에서 서버 호출 없이 처리하는 분기이나,
     *       서버 측 검증도 동일하게 400을 반환해야 한다.
     */
    it('7장 미만 스와이프 시 400 Bad Request를 반환해야 한다', async () => {
      const res = await request(BASE_URL)
        .post('/api/v1/members/onboarding')
        .set('Authorization', BEARER_TOKEN)
        .send({
          swipeResults: [
            { cardId: 'card-korean-001', liked: true, category: '한식' },
            { cardId: 'card-japanese-001', liked: true, category: '일식' },
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
     * else — 중간 이탈
     *
     * Frontend → Gateway: PUT /api/v1/members/onboarding/progress {현재까지 스와이프 결과}
     * Gateway → MemberService: 진행 상태 임시 저장
     * MemberService → MemberDB: 온보딩 진행 상태 저장
     * MemberService → Gateway: 200 OK
     * Gateway → Frontend: 저장 완료
     * Frontend → User: 재진입 시 이어서 진행 가능 안내
     */
    it('중간 이탈 시 PUT 진행 상태 저장 요청에 200 OK를 반환해야 한다', async () => {
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
   * == 위치 정보 동의 (UFR-MBR-030) ==
   *
   * Frontend → User: 위치 정보 수집 동의 화면 표시 (수집 목적, 보유 기간 6개월 고지)
   * User → Frontend: 위치 정보 동의/거절 선택
   */
  describe('위치 정보 동의 (UFR-MBR-030)', () => {

    /**
     * alt — 동의
     *
     * Frontend → Gateway: POST /api/v1/members/location/consent {동의 여부: true}
     * Gateway → MemberService: 위치 동의 처리
     * MemberService → MemberDB: 위치 동의 이력 저장 (동의 시각, 동의 내용)
     * MemberDB → MemberService: 저장 완료
     * MemberService → Gateway: 200 OK {위치 기반 추천 활성화}
     * Gateway → Frontend: 동의 처리 완료
     * Frontend → User: 위치 자동 인식 + 메인 화면 전환
     *
     * Note: outer 시퀀스 경로 /api/v1/members/location/consent는
     *       API 명세의 /api/v1/members/location-consent와 대응한다.
     */
    it('위치 정보 동의 시 200 OK와 locationEnabled: true를 반환해야 한다', async () => {
      const res = await request(BASE_URL)
        .post('/api/v1/members/location-consent')
        .set('Authorization', BEARER_TOKEN)
        .send({
          consented: true,
          consentedAt: '2026-02-26T12:01:30Z',
        })
        .expect(200);

      expect(res.body).toHaveProperty('locationEnabled');
      expect(res.body).toHaveProperty('message');
      expect(res.body.locationEnabled).toBe(true);
    });

    /**
     * else — 거절
     *
     * Frontend → Gateway: POST /api/v1/members/location/consent {동의 여부: false}
     * Gateway → MemberService: 위치 거절 처리
     * MemberService → MemberDB: 위치 거절 이력 저장
     * MemberService → Gateway: 200 OK {수동 위치 입력 모드}
     * Gateway → Frontend: 거절 처리 완료
     * Frontend → User: "위치를 직접 입력해주세요" 안내
     */
    it('위치 정보 거절 시 200 OK와 locationEnabled: false를 반환해야 한다', async () => {
      const res = await request(BASE_URL)
        .post('/api/v1/members/location-consent')
        .set('Authorization', BEARER_TOKEN)
        .send({
          consented: false,
          consentedAt: '2026-02-26T12:01:30Z',
        })
        .expect(200);

      expect(res.body).toHaveProperty('locationEnabled');
      expect(res.body).toHaveProperty('message');
      expect(res.body.locationEnabled).toBe(false);
    });
  });
});
