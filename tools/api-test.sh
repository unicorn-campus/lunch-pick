#!/bin/bash
# LunchPick API Integration Test Script
# Tests all endpoints across 4 services

MEMBER=http://localhost:8081
RECOMMEND=http://localhost:8082
PAYMENT=http://localhost:8083
AI=http://localhost:8000

PASS=0; FAIL=0; RESULTS=""

test_api() {
  local desc="$1" method="$2" url="$3" expected="$4"
  shift 4
  local body="" headers=()
  while [[ $# -gt 0 ]]; do
    case "$1" in
      -d) body="$2"; shift 2;;
      -H) headers+=(-H "$2"); shift 2;;
      *) shift;;
    esac
  done
  local cmd=(curl -s -o /tmp/api_resp.json -w "%{http_code}" -X "$method" "$url" "${headers[@]}")
  [[ -n "$body" ]] && cmd+=(-d "$body")
  local code=$("${cmd[@]}" 2>/dev/null)
  local resp=$(cat /tmp/api_resp.json 2>/dev/null | head -c 200)
  if [[ "$code" == "$expected" ]]; then
    PASS=$((PASS+1)); RESULTS+="  PASS  $desc  [HTTP $code]\n"
  else
    FAIL=$((FAIL+1)); RESULTS+="  FAIL  $desc  [HTTP $code, expected $expected] $resp\n"
  fi
}

echo "============================================"
echo " LunchPick API Integration Test"
echo "============================================"

# Get JWT Token
TOKEN_RESP=$(curl -s -X POST $MEMBER/api/test/login -H "Content-Type: application/json" -d '{"nickname":"testuser01"}')
TOKEN=$(echo "$TOKEN_RESP" | python3 -c "import sys,json; print(json.load(sys.stdin).get('accessToken',''))" 2>/dev/null)
MEMBER_ID=$(echo "$TOKEN_RESP" | python3 -c "import sys,json; print(json.load(sys.stdin).get('memberId',''))" 2>/dev/null)
if [[ -z "$TOKEN" ]]; then echo "FATAL: No JWT token!"; echo "$TOKEN_RESP"; exit 1; fi
echo "Token OK. memberId=$MEMBER_ID"
echo ""
AUTH="Authorization: Bearer $TOKEN"
CT="Content-Type: application/json"

echo "========== MEMBER SERVICE (8081) =========="
# 1. Kakao login (500 expected — no real Kakao service)
test_api "POST /auth/kakao (no Kakao = 500)" POST "$MEMBER/api/v1/auth/kakao" "500" -H "$CT" -d '{"code":"test-code"}'
# 2. GET profile
test_api "GET /members/me" GET "$MEMBER/api/v1/members/me" "200" -H "$AUTH"
# 3. PUT profile
test_api "PUT /members/me" PUT "$MEMBER/api/v1/members/me" "200" -H "$AUTH" -H "$CT" \
  -d '{"nickname":"updatedUser","notificationSettings":{"recommendationAlert":true,"feedbackReminder":false}}'
# 4. Location consent
test_api "POST /members/me/location-consent" POST "$MEMBER/api/v1/members/me/location-consent" "200" -H "$AUTH" -H "$CT" \
  -d '{"consented":true,"consentedAt":null}'
# 5. Dietary restrictions
test_api "PUT /members/me/dietary-restrictions" PUT "$MEMBER/api/v1/members/me/dietary-restrictions" "200" -H "$AUTH" -H "$CT" \
  -d '{"healthInfoConsentGiven":true,"allergens":["PEANUT"],"customAllergens":[],"dietType":"NORMAL"}'
# 6. Onboarding submit
test_api "POST /onboarding" POST "$MEMBER/api/v1/onboarding" "200" -H "$AUTH" -H "$CT" \
  -d '{"swipeResults":[{"cardId":"c1","liked":true,"category":"KOREAN"},{"cardId":"c2","liked":false,"category":"CHINESE"},{"cardId":"c3","liked":true,"category":"JAPANESE"},{"cardId":"c4","liked":true,"category":"WESTERN"},{"cardId":"c5","liked":false,"category":"SNACK"},{"cardId":"c6","liked":true,"category":"ASIAN"},{"cardId":"c7","liked":false,"category":"FASTFOOD"}],"healthInfoConsentGiven":true}'
# 7. Onboarding progress
test_api "PUT /onboarding/progress" PUT "$MEMBER/api/v1/onboarding/progress" "200" -H "$AUTH" -H "$CT" \
  -d '{"swipeResults":[{"cardId":"c1","liked":true,"category":"KOREAN"}]}'
# 8. Get subscription status
test_api "GET /members/me/subscription" GET "$MEMBER/api/v1/members/me/subscription" "200" -H "$AUTH"
# 9. Internal taste profile
test_api "GET /internal/members/{id}/taste-profile" GET "$MEMBER/internal/members/$MEMBER_ID/taste-profile" "200"
# 10. Actuator health
test_api "GET /actuator/health (member)" GET "$MEMBER/actuator/health" "200"

echo ""
echo "========== RECOMMENDATION SERVICE (8082) =========="
# 11. Get today's recommendations (latitude, longitude are query params)
test_api "GET /recommendations/today" GET "$RECOMMEND/api/v1/recommendations/today?latitude=37.5665&longitude=126.9780" "200" -H "$AUTH"
# 12. Refresh (needs @NotEmpty rejectedIds)
test_api "POST /recommendations/refresh" POST "$RECOMMEND/api/v1/recommendations/refresh" "200" -H "$AUTH" -H "$CT" \
  -d '{"rejectedIds":["fake-rec-id"],"latitude":37.5665,"longitude":126.9780}'
# 13. Accept recommendation (404 expected — no real rec)
test_api "POST /recommendations/{id}/accept (404)" POST "$RECOMMEND/api/v1/recommendations/fake-rec-id/accept" "404" -H "$AUTH" -H "$CT" \
  -d '{"acceptedAt":"2026-02-26T12:00:00","reactionTimeMs":500}'
# 14. Reject recommendation (404 expected — no real rec)
test_api "POST /recommendations/{id}/reject (404)" POST "$RECOMMEND/api/v1/recommendations/fake-rec-id/reject" "404" -H "$AUTH" -H "$CT" \
  -d '{"rejectReason":"MOOD_NOT_MATCH"}'
# 15. Create meal record
test_api "POST /meals (201 Created)" POST "$RECOMMEND/api/v1/meals" "201" -H "$AUTH" -H "$CT" \
  -d '{"restaurantId":"test-restaurant-001","menuName":"kimchi-jjigae","recordedAt":"2026-02-26T12:00:00"}'
# 16. Get history timeline
test_api "GET /history/timeline" GET "$RECOMMEND/api/v1/history/timeline" "200" -H "$AUTH"
# 17. Get insights
test_api "GET /insights" GET "$RECOMMEND/api/v1/insights" "200" -H "$AUTH"
# 18. Actuator health (recommendation)
test_api "GET /actuator/health (recommendation)" GET "$RECOMMEND/actuator/health" "200"

echo ""
echo "========== PAYMENT SERVICE (8083) =========="
# 19. Get subscription plans
test_api "GET /subscriptions/plans" GET "$PAYMENT/api/v1/subscriptions/plans" "200" -H "$AUTH"
# 20. Create subscription
test_api "POST /subscriptions (201 Created)" POST "$PAYMENT/api/v1/subscriptions" "201" -H "$AUTH" -H "$CT" \
  -d '{"planId":"PREMIUM_MONTHLY","paymentMethod":{"type":"CREDIT_CARD","cardNumber":"4111-1111-1111-1111","expiryMonth":12,"expiryYear":2028,"cvc":"123","cardholderName":"TEST USER"},"autoRenewalAgreed":true,"withdrawalRightAcknowledged":true}'
# 21. Extend trial (200 — subscription created above)
test_api "POST /subscriptions/extend-trial" POST "$PAYMENT/api/v1/subscriptions/extend-trial" "200" -H "$AUTH"
# 22. Actuator health (payment)
test_api "GET /actuator/health (payment)" GET "$PAYMENT/actuator/health" "200"

echo ""
echo "========== AI PIPELINE SERVICE (8000) =========="
# 23. Health check
test_api "GET /health (ai)" GET "$AI/health" "200"
# 24. Recommendations
test_api "POST /ai/recommendations" POST "$AI/api/v1/ai/recommendations" "200" -H "$CT" \
  -d '{"memberId":"'"$MEMBER_ID"'","latitude":37.5665,"longitude":126.9780,"requestedAt":"2026-02-26T12:00:00","isColdStart":true,"feedbackCount":0,"tasteVector":{"KOREAN":0.8,"JAPANESE":0.6},"onboardingSwipes":[{"cardId":"c1","category":"KOREAN","liked":true}],"allergenFilter":[],"dietType":"NORMAL"}'
# 25. Recommendation reason
test_api "POST /ai/recommendation-reason" POST "$AI/api/v1/ai/recommendation-reason" "200" -H "$CT" \
  -d '{"recommendationId":"test-rec-001","restaurantId":"test-rest-001","restaurantName":"Test Restaurant","category":"KOREAN","memberId":"'"$MEMBER_ID"'","tasteVector":{"KOREAN":0.8}}'

echo ""
echo "============================================"
echo " RESULTS SUMMARY"
echo "============================================"
echo -e "$RESULTS"
echo "--------------------------------------------"
echo " TOTAL: $((PASS+FAIL)) | PASS: $PASS | FAIL: $FAIL"
echo "============================================"
