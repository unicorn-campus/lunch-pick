import request from 'supertest';

const BASE_URL = process.env.MEMBER_SERVICE_URL || 'http://localhost:8081';

/**
 * 회원 서비스 - 소셜 로그인 (UFR-MBR-010)
 *
 * 설계 출처: docs/design/sequence/inner/member-service-소셜로그인.puml
 * API 명세: docs/design/api/member-service-api.yaml — POST /api/v1/auth/kakao
 *
 * 이 파일은 직접 실행하지 않으며, 백엔드 구현 시 행위 참고 자료로만 활용된다.
 * alt/else 분기 수와 it() 케이스 수는 1:1 대응한다.
 */
describe('회원 서비스 - 소셜 로그인 (UFR-MBR-010)', () => {

  /**
   * 최상위 alt: 카카오 토큰 교환 결과 분기
   *
   * 시퀀스 흐름:
   *   AuthService → Kakao: 카카오 액세스 토큰 교환 요청 (인증 코드 → 액세스 토큰)
   *   alt 토큰 교환 성공
   *     → 프로필 조회 → findOrCreateMember → JWT 생성 → Redis 세션 저장
   *   else 카카오 토큰 교환 실패 (Circuit Breaker 적용)
   *     → AuthException {code: KAKAO_AUTH_FAILED}
   */
  describe('카카오 액세스 토큰 교환', () => {

    /**
     * alt: 토큰 교환 성공 후 중첩 분기 — 기존 회원 vs 신규 회원
     *
     * 토큰 교환 성공 경로:
     *   AuthService → Kakao: 프로필 조회 (이메일, 카카오ID, 닉네임 반환)
     *   AuthService → MemberService: findOrCreateMember(kakaoId, email, nickname)
     *   MemberService → MemberRepo: 이메일로 회원 조회
     */
    describe('토큰 교환 성공', () => {

      /**
       * 중첩 alt — 기존 회원
       *
       * MemberService: 이메일로 회원 조회 → 기존 회원 발견
       *   → MemberRepo: 카카오 ID 갱신 (없는 경우)
       *   → AuthService: {memberId, isNewMember: false}
       *   → TokenService: JWT 생성 (subject: memberId, exp: 1시간)
       *   → Redis: 세션 저장 (session:{memberId}, TTL: 1시간)
       *   → Controller: 200 OK, isNewMember: false
       */
      it('기존 회원 시 200 OK와 isNewUser: false를 반환해야 한다', async () => {
        const res = await request(BASE_URL)
          .post('/api/v1/auth/kakao')
          .send({
            authorizationCode: '4BQ3X5Y_f8z2Kp9mNjR',
          })
          .expect(200);

        expect(res.body).toHaveProperty('accessToken');
        expect(res.body).toHaveProperty('tokenType');
        expect(res.body).toHaveProperty('expiresIn');
        expect(res.body).toHaveProperty('memberId');
        expect(res.body).toHaveProperty('isNewUser');
        expect(res.body).toHaveProperty('onboardingCompleted');
        expect(res.body.isNewUser).toBe(false);
        expect(res.body.tokenType).toBe('Bearer');
        expect(res.body.expiresIn).toBe(3600);
      });

      /**
       * 중첩 alt — 신규 회원
       *
       * MemberService: 이메일로 회원 조회 → 조회 결과 없음
       *   → MemberRepo: INSERT 회원 정보 {카카오ID, 이메일, 닉네임, 가입시각}
       *   → AuthService: {memberId, isNewMember: true}
       *   → TokenService: JWT 생성
       *   → Redis: 세션 저장
       *   → Controller: 201 Created, isNewMember: true
       */
      it('신규 회원 시 201 Created와 isNewUser: true를 반환해야 한다', async () => {
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
        expect(res.body.tokenType).toBe('Bearer');
        expect(res.body.expiresIn).toBe(3600);
      });
    });

    /**
     * else: 카카오 토큰 교환 실패 (Circuit Breaker 적용)
     *
     * Kakao → AuthService: 인증 오류
     * AuthService → Controller: AuthException {code: KAKAO_AUTH_FAILED}
     * Controller: 401 Unauthorized
     */
    it('카카오 토큰 교환 실패 시 401 Unauthorized와 KAKAO_AUTH_FAILED를 반환해야 한다', async () => {
      const res = await request(BASE_URL)
        .post('/api/v1/auth/kakao')
        .send({
          authorizationCode: 'INVALID_OR_EXPIRED_CODE',
        })
        .expect(401);

      expect(res.body).toHaveProperty('error');
      expect(res.body).toHaveProperty('message');
      expect(res.body).toHaveProperty('timestamp');
      expect(res.body.error).toBe('KAKAO_AUTH_FAILED');
    });
  });
});
