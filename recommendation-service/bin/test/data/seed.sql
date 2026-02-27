-- recommendation-service 테스트 데이터 seed
-- 멱등성 보장: TRUNCATE → INSERT 순서
-- 실행 DB: recommendation (jdbc:postgresql://localhost:15432/recommendation)

-- 테이블 초기화
TRUNCATE TABLE feedback RESTART IDENTITY CASCADE;
TRUNCATE TABLE learning_message RESTART IDENTITY CASCADE;
TRUNCATE TABLE meal_record RESTART IDENTITY CASCADE;
TRUNCATE TABLE preference_vector RESTART IDENTITY CASCADE;
TRUNCATE TABLE recommendation RESTART IDENTITY CASCADE;

-- 테스트 추천 데이터 (PENDING 상태 — accept/reject 테스트용)
INSERT INTO recommendation (recommendation_id, member_id, restaurant_id, restaurant_name, representative_menu, reason_summary, confidence_score, distance_meters, estimated_walk_minutes, category, is_fallback, status, created_at, updated_at)
VALUES (
    'rec-uuid-0001',
    'test-member-uuid-0001',
    'rest-uuid-0001',
    '강남 한식당',
    '된장찌개 정식',
    '오늘 날씨에 따뜻한 국물 요리가 딱이에요',
    85,
    350,
    5,
    '한식',
    false,
    'PENDING',
    NOW(),
    NOW()
);

INSERT INTO recommendation (recommendation_id, member_id, restaurant_id, restaurant_name, representative_menu, reason_summary, confidence_score, distance_meters, estimated_walk_minutes, category, is_fallback, status, created_at, updated_at)
VALUES (
    'rec-uuid-0002',
    'test-member-uuid-0001',
    'rest-uuid-0002',
    '스시 타로',
    '연어 초밥 세트',
    '좋아하시는 일식 메뉴예요',
    72,
    500,
    7,
    '일식',
    false,
    'PENDING',
    NOW(),
    NOW()
);

INSERT INTO recommendation (recommendation_id, member_id, restaurant_id, restaurant_name, representative_menu, reason_summary, confidence_score, distance_meters, estimated_walk_minutes, category, is_fallback, status, created_at, updated_at)
VALUES (
    'rec-uuid-0003',
    'test-member-uuid-0001',
    'rest-uuid-0003',
    '교촌치킨 강남점',
    '교촌 허니콤보',
    '피드백 기반 추천',
    60,
    800,
    11,
    '치킨',
    false,
    'PENDING',
    NOW(),
    NOW()
);

-- 이미 수락된 추천 (meal_record 연동용)
INSERT INTO recommendation (recommendation_id, member_id, restaurant_id, restaurant_name, representative_menu, reason_summary, confidence_score, distance_meters, estimated_walk_minutes, category, is_fallback, status, reaction_time_ms, created_at, updated_at)
VALUES (
    'rec-uuid-0004',
    'test-member-uuid-0001',
    'rest-uuid-0001',
    '강남 한식당',
    '비빔밥',
    '어제도 맛있게 드셨죠',
    78,
    350,
    5,
    '한식',
    false,
    'ACCEPTED',
    3200,
    NOW() - INTERVAL '1 day',
    NOW() - INTERVAL '1 day'
);

-- 식사 기록 (feedback 테스트용)
INSERT INTO meal_record (meal_id, member_id, recommendation_id, restaurant_id, restaurant_name, menu_name, category, recorded_at, created_at, updated_at)
VALUES (
    'meal-uuid-0001',
    'test-member-uuid-0001',
    'rec-uuid-0004',
    'rest-uuid-0001',
    '강남 한식당',
    '비빔밥',
    '한식',
    NOW() - INTERVAL '1 day',
    NOW() - INTERVAL '1 day',
    NOW() - INTERVAL '1 day'
);

-- preference_vector (취향 학습 배치 테스트용)
INSERT INTO preference_vector (member_id, category_vector, updated_at)
VALUES (
    'test-member-uuid-0001',
    '{"한식": 0.85, "일식": 0.70, "중식": 0.40}',
    NOW()
) ON CONFLICT (member_id) DO NOTHING;
