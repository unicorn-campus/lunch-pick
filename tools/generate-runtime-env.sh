#!/usr/bin/env bash
# ============================================================
# generate-runtime-env.sh
# ROOT .env 파일에서 프론트엔드 runtime-env.js를 생성한다.
# 사용법: bash tools/generate-runtime-env.sh [env파일경로]
# 기본값: 프로젝트 루트의 .env
# Git Bash (Windows) + Mac/Linux 호환
# ============================================================
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

ENV_FILE="${1:-$PROJECT_ROOT/.env}"
OUTPUT_FILE="$PROJECT_ROOT/frontend/public/runtime-env.js"

if [ ! -f "$ENV_FILE" ]; then
  echo "WARNING: .env 파일 없음 ($ENV_FILE). 기본값으로 runtime-env.js를 생성합니다."
fi

# .env 파일에서 값 읽기 (# 주석, 빈 줄 무시). 파일 없으면 기본값 반환
get_env() {
  local key="$1"
  local default="$2"
  if [ -f "$ENV_FILE" ]; then
    local value
    value=$(grep -E "^${key}=" "$ENV_FILE" | head -1 | cut -d'=' -f2- | tr -d '\r')
    echo "${value:-$default}"
  else
    echo "$default"
  fi
}

# 서비스 포트 → 호스트 URL 매핑
MEMBER_PORT=$(get_env "MEMBER_SERVICE_PORT" "8081")
RECOMMENDATION_PORT=$(get_env "RECOMMENDATION_SERVICE_PORT" "8082")
PAYMENT_PORT=$(get_env "PAYMENT_SERVICE_PORT" "8083")
AI_PORT=$(get_env "AI_PIPELINE_SERVICE_PORT" "8084")

KAKAO_CLIENT_ID=$(get_env "KAKAO_CLIENT_ID" "")
KAKAO_API_KEY=$(get_env "KAKAO_API_KEY" "")
KAKAO_JS_KEY=$(get_env "KAKAO_JS_KEY" "")

cat > "$OUTPUT_FILE" <<EOF
window.__runtime_config__ = {
  APP_ENV: "development",
  API_GROUP: "/api/v1",
  MEMBER_HOST: "http://localhost:${MEMBER_PORT}",
  RECOMMENDATION_HOST: "http://localhost:${RECOMMENDATION_PORT}",
  PAYMENT_HOST: "http://localhost:${PAYMENT_PORT}",
  AI_HOST: "http://localhost:${AI_PORT}",
  KAKAO_CLIENT_ID: "${KAKAO_CLIENT_ID}",
  KAKAO_API_KEY: "${KAKAO_API_KEY}",
  KAKAO_JS_KEY: "${KAKAO_JS_KEY}"
};
EOF

echo "OK: $OUTPUT_FILE 생성 완료"
