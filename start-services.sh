#!/bin/bash
# 공통 환경변수
export DB_KIND=postgresql
export DB_HOST=localhost
export DB_PORT=15432
export DB_USER=lunchpick
export DB_PASSWORD='P@ssw0rd$'
export DDL_AUTO=update
export SHOW_SQL=true
export REDIS_HOST=localhost
export REDIS_PORT=16379
export JWT_SECRET=lunchpick-dev-jwt-secret-key-2026-must-be-at-least-256-bits-long-for-hs256
export CORS_ALLOWED_ORIGINS=http://localhost:3000
export MQ_SUBSCRIPTION_TOPIC=subscription-events
export SPRING_PROFILES_ACTIVE=dev

cd /c/Users/hiond/workspace/lunch-menu-recommender

# member-service
DB_NAME=member REDIS_DATABASE=1 MQ_REDIS_STREAMS_ENABLED=true SERVER_PORT=8081 \
  java -jar member-service/build/libs/member-service.jar > logs/member-service-console.log 2>&1 &
echo "member-service PID: $!"

# payment-service
DB_NAME=payment REDIS_DATABASE=3 SERVER_PORT=8083 \
  java -jar payment-service/build/libs/payment-service.jar > logs/payment-service-console.log 2>&1 &
echo "payment-service PID: $!"

# recommendation-service
DB_NAME=recommendation REDIS_DATABASE=2 MEMBER_SERVICE_URL=http://localhost:8081 AI_PIPELINE_SERVICE_URL=http://localhost:8084 SERVER_PORT=8082 \
  java -jar recommendation-service/build/libs/recommendation-service.jar > logs/recommendation-service-console.log 2>&1 &
echo "recommendation-service PID: $!"

echo "All services starting..."
