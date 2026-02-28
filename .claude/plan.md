# generate-runtime-env.sh 솔루션을 NPD 플러그인에 보완하는 작업 계획

## 컨텍스트

런치픽 프로젝트에서 검증된 `runtime-env.js` 런타임 환경변수 방식을 NPD 플러그인의 개발 스킬에 범용 솔루션으로 통합한다.

### 현재 상태 (AS-IS)

1. **NPD 도구 디렉토리** (`resources/tools/customs/general/`): `run-intellij-service-profile.py`, `generate_image.py` 2개만 존재
2. **SKILL.md Step 8**: `run-intellij-service-profile.py` 1개만 하드코딩으로 복사
3. **프론트엔드 가이드** (`frontend-env-setup-react.md`, `frontend-env-setup-vue.md`): `runtime-env.js`를 수동으로 생성하도록 안내 (7단계에서 하드코딩)
4. **프론트엔드 통합 가이드** (`frontend-integration-react.md`, `frontend-integration-vue.md`): `runtime-env.js` 값을 수동으로 Mock에서 실제 URL로 교체하도록 안내
5. **런치픽 프로젝트**: ROOT `.env` 기반 `generate-runtime-env.sh` + `predev`/`prebuild` 훅으로 자동화 완료

### 목표 상태 (TO-BE)

1. **NPD 도구 디렉토리**: `generate-runtime-env.sh` 추가 (범용 버전)
2. **SKILL.md Step 8**: `customs/general/` 디렉토리의 모든 도구를 `tools/`로 복사
3. **프론트엔드 가이드**: `generate-runtime-env.sh` 기반 자동 생성 + `predev`/`prebuild` 훅 안내 추가
4. **프론트엔드 통합 가이드**: 스크립트 재실행으로 전환하도록 안내 변경

---

## 변경 대상 파일 목록

| # | 파일 경로 (NPD 플러그인 기준) | 변경 유형 | 설명 |
|---|------|----------|------|
| 1 | `resources/tools/customs/general/generate-runtime-env.sh` | **신규 생성** | 범용 runtime-env.js 생성 스크립트 |
| 2 | `skills/develop/SKILL.md` | **수정** | Step 8 도구 복사 로직 확장 |
| 3 | `resources/guides/develop/frontend-env-setup-react.md` | **수정** | 7단계에 generate-runtime-env.sh 기반 안내 추가 |
| 4 | `resources/guides/develop/frontend-env-setup-vue.md` | **수정** | 7단계에 generate-runtime-env.sh 기반 안내 추가 |
| 5 | `resources/guides/develop/frontend-integration-react.md` | **수정** | 3단계 환경변수 전환을 스크립트 재실행 방식으로 변경 |
| 6 | `resources/guides/develop/frontend-integration-vue.md` | **수정** | 3단계 환경변수 전환을 스크립트 재실행 방식으로 변경 |

---

## 변경 순서 및 의존성

```
[Task 1] generate-runtime-env.sh 범용 스크립트 생성
    ↓ (의존)
[Task 2] SKILL.md Step 8 도구 복사 로직 확장
    (독립)
[Task 3] frontend-env-setup-react.md 7단계 수정
    (독립)
[Task 4] frontend-env-setup-vue.md 7단계 수정
    (독립)
[Task 5] frontend-integration-react.md 3단계 수정
    (독립)
[Task 6] frontend-integration-vue.md 3단계 수정
```

Task 1이 선행 필수. Task 2~6은 서로 독립적이므로 병렬 작업 가능.

---

## Task 1. 범용 `generate-runtime-env.sh` 생성

**파일**: `resources/tools/customs/general/generate-runtime-env.sh`

### 설계 원칙

런치픽 버전(`C:/Users/hiond/workspace/lunch-menu-recommender/tools/generate-runtime-env.sh`)은 서비스 목록(MEMBER, RECOMMENDATION, PAYMENT, AI_PIPELINE)이 하드코딩되어 있다. NPD 범용 버전은 프로젝트별 서비스 목록이 다르므로 다음 전략을 사용한다:

- ROOT `.env` 파일에서 `*_SERVICE_PORT` 패턴의 모든 변수를 자동 탐지
- `*_SERVICE_PORT` 키에서 서비스명을 추출하여 `{SERVICE}_HOST: "http://localhost:{PORT}"` 형태로 변환
- 추가 환경변수(KAKAO, GOOGLE 등 OAuth 관련)도 `*_CLIENT_ID`, `*_API_KEY` 패턴으로 자동 포함
- `.env` 파일에 `FRONTEND_DIR` 변수가 있으면 해당 경로를, 없으면 `frontend/public/` 기본값 사용

### 구체적 변경 내용

```bash
#!/usr/bin/env bash
# generate-runtime-env.sh
# ROOT .env 파일에서 프론트엔드 runtime-env.js를 자동 생성한다.
# 사용법: bash tools/generate-runtime-env.sh [env파일경로] [프로젝트루트경로]
# 기본값: CWD의 .env, CWD를 프로젝트 루트로 사용
# Git Bash (Windows) + Mac/Linux 호환

set -euo pipefail

# [Architect 피드백 #1] PROJECT_ROOT는 CWD 기반으로 결정
# 플러그인 경로에서 직접 실행해도 CWD가 프로젝트 루트이면 정상 동작
PROJECT_ROOT="${2:-$(pwd)}"
ENV_FILE="${1:-$PROJECT_ROOT/.env}"

# .env에서 FRONTEND_DIR 읽기 (기본값: frontend)
FRONTEND_DIR=$(grep -E "^FRONTEND_DIR=" "$ENV_FILE" 2>/dev/null | head -1 | cut -d'=' -f2- | tr -d '\r')
FRONTEND_DIR="${FRONTEND_DIR:-frontend}"
OUTPUT_FILE="$PROJECT_ROOT/$FRONTEND_DIR/public/runtime-env.js"

# .env 파일 존재 확인
if [ ! -f "$ENV_FILE" ]; then
  echo "WARNING: .env 파일 없음 ($ENV_FILE). 기본 runtime-env.js를 생성합니다."
  mkdir -p "$(dirname "$OUTPUT_FILE")"
  cat > "$OUTPUT_FILE" <<'FALLBACK'
window.__runtime_config__ = {
  APP_ENV: "development",
  API_GROUP: "/api/v1"
};
FALLBACK
  echo "OK: $OUTPUT_FILE 생성 완료 (기본값)"
  exit 0
fi

# .env 파일에서 값 읽기
get_env() {
  local key="$1"
  local default="$2"
  local value
  value=$(grep -E "^${key}=" "$ENV_FILE" | head -1 | cut -d'=' -f2- | tr -d '\r')
  echo "${value:-$default}"
}

# API_GROUP 읽기
API_GROUP=$(get_env "API_GROUP" "/api/v1")

# runtime-env.js 생성 시작
mkdir -p "$(dirname "$OUTPUT_FILE")"

# [Architect 피드백 #2] 프론트엔드 허용 API_KEY allowlist 방식
# 서버 전용 키(GEMINI, OPENAI 등)가 노출되지 않도록 명시적 허용 목록만 포함
FRONTEND_SAFE_API_KEY_PREFIXES="KAKAO|GOOGLE_MAPS|NAVER_MAPS|TMAP"

{
  echo "window.__runtime_config__ = {"
  echo "  APP_ENV: \"development\","
  echo "  API_GROUP: \"${API_GROUP}\","

  # [Architect 피드백 #3] grep 빈 결과 시 pipefail로 인한 스크립트 중단 방지
  # *_SERVICE_PORT 패턴의 모든 변수에서 서비스별 HOST 생성
  { grep -E "^[A-Z_]+_SERVICE_PORT=" "$ENV_FILE" 2>/dev/null || :; } | while IFS='=' read -r key value; do
    [ -z "$key" ] && continue
    value=$(echo "$value" | tr -d '\r')
    # MEMBER_SERVICE_PORT → MEMBER
    service_name=$(echo "$key" | sed 's/_SERVICE_PORT$//')
    echo "  ${service_name}_HOST: \"http://localhost:${value}\","
  done

  # *_CLIENT_ID 패턴 변수 포함
  { grep -E "^[A-Z_]+_CLIENT_ID=" "$ENV_FILE" 2>/dev/null || :; } | while IFS='=' read -r key value; do
    [ -z "$key" ] && continue
    value=$(echo "$value" | tr -d '\r')
    echo "  ${key}: \"${value}\","
  done

  # *_API_KEY 패턴 변수 포함 (allowlist 방식 — 프론트엔드 안전 키만 허용)
  { grep -E "^(${FRONTEND_SAFE_API_KEY_PREFIXES})_API_KEY=" "$ENV_FILE" 2>/dev/null || :; } | while IFS='=' read -r key value; do
    [ -z "$key" ] && continue
    value=$(echo "$value" | tr -d '\r')
    echo "  ${key}: \"${value}\","
  done

  echo "};"
} > "$OUTPUT_FILE"

echo "OK: $OUTPUT_FILE 생성 완료"
```

> **참고**: 생성된 `runtime-env.js`는 마지막 속성 뒤에 trailing comma를 포함할 수 있다 (ES5+ 호환).

### 런치픽 버전과의 차이점

| 항목 | 런치픽 버전 | NPD 범용 버전 |
|------|-----------|-------------|
| 서비스 목록 | 4개 하드코딩 (MEMBER, RECOMMENDATION, PAYMENT, AI_PIPELINE) | `*_SERVICE_PORT` 패턴으로 자동 탐지 |
| OAuth 키 | KAKAO_CLIENT_ID, KAKAO_API_KEY 하드코딩 | `*_CLIENT_ID` 자동 탐지, `*_API_KEY`는 allowlist 방식 |
| API_KEY 보안 | 수동 | allowlist 방식 (KAKAO/GOOGLE_MAPS/NAVER_MAPS/TMAP만 허용) |
| 출력 경로 | `frontend/public/runtime-env.js` 고정 | `FRONTEND_DIR` 변수로 커스터마이징 가능 |
| PROJECT_ROOT | `SCRIPT_DIR/..` (tools/ 하위 전제) | CWD 기반 또는 2번째 인자 (플러그인 경로에서도 동작) |
| .env 미존재 시 | ERROR로 중단 | WARNING + 기본 fallback 파일 생성 |
| pipefail 안전성 | 미고려 | grep 빈 결과 시 `|| :` 로 안전 처리 |

### 수용 기준

- [ ] `resources/tools/customs/general/generate-runtime-env.sh` 파일이 존재한다
- [ ] 실행 권한(`chmod +x`)이 부여되어 있다
- [ ] `.env`에 `MEMBER_SERVICE_PORT=8081`, `PAYMENT_SERVICE_PORT=8083`이 있을 때 `MEMBER_HOST`, `PAYMENT_HOST`가 생성된다
- [ ] `.env`가 없어도 기본 fallback `runtime-env.js`가 생성된다
- [ ] 서버 전용 키(GEMINI_API_KEY, OPENAI_API_KEY 등)가 `runtime-env.js`에 노출되지 않는다

---

## Task 2. SKILL.md Step 8 도구 복사 로직 확장

**파일**: `skills/develop/SKILL.md`

### AS-IS (현재 756~757행)

```markdown
2. **실행 도구 복사**: `{PLUGIN_DIR}/resources/tools/customs/general/run-intellij-service-profile.py`를 프로젝트 루트 `tools/run-intellij-service-profile.py`로 복사한다
   - **EXPECTED OUTCOME**: `tools/run-intellij-service-profile.py`
```

### TO-BE

```markdown
2. **실행 도구 복사**: `{PLUGIN_DIR}/resources/tools/customs/general/` 디렉토리의 모든 파일(하위 디렉토리 제외)을 프로젝트 루트 `tools/`로 복사한다
   - `__pycache__/` 등 캐시 디렉토리는 제외한다
   - 이미 존재하는 파일은 덮어쓴다
   - **EXPECTED OUTCOME**: `tools/run-intellij-service-profile.py`, `tools/generate-runtime-env.sh`, `tools/generate_image.py` 등
```

### 수용 기준

- [ ] Step 8 항목 2번이 디렉토리 전체 복사 방식으로 변경되어 있다
- [ ] `__pycache__/` 등 캐시 디렉토리 제외가 명시되어 있다
- [ ] 새로운 도구가 `customs/general/`에 추가되어도 SKILL.md 수정 없이 자동으로 복사된다

---

## Task 3. frontend-env-setup-react.md 7단계 수정

**파일**: `resources/guides/develop/frontend-env-setup-react.md`

### 변경 범위: 7.1 런타임 환경변수 설정 (519~564행)

### AS-IS 핵심

- `public/runtime-env.js` 파일을 수동으로 생성 (서비스별 HOST를 하드코딩)
- `index.html`에 `<script>` 태그 수동 추가
- `src/config/runtime.ts` 헬퍼 수동 생성

### TO-BE

7.1 섹션을 다음과 같이 재구성한다:

```markdown
#### 7.1 런타임 환경변수 설정

프론트엔드 환경변수는 `public/runtime-env.js`에서 런타임으로 주입한다.
`generate-runtime-env.sh` 스크립트가 ROOT `.env` 파일에서 자동 생성하므로 수동 작성이 불필요하다.

**7.1.1 `generate-runtime-env.sh`로 초기 생성**

프로젝트 루트 `tools/generate-runtime-env.sh`가 존재하면 실행하여 초기 `runtime-env.js`를 생성한다.
존재하지 않으면 `{PLUGIN_DIR}/resources/tools/customs/general/generate-runtime-env.sh`를 사용한다.

```bash
# 프로젝트 루트에서 실행
bash tools/generate-runtime-env.sh
# 또는 (tools/ 미존재 시)
bash {PLUGIN_DIR}/resources/tools/customs/general/generate-runtime-env.sh
```

스크립트는 ROOT `.env`의 `*_SERVICE_PORT` 변수를 읽어 서비스별 HOST를 자동 생성한다.
Mock 단계이므로 생성된 파일의 HOST 값을 모두 `http://localhost:4010`(Prism Mock 서버)으로 교체한다.

> `.env` 파일에 서비스 포트가 정의되어 있지 않거나 `.env` 파일 자체가 없으면,
> 스크립트가 기본 fallback 파일을 생성한다. 이 경우 수동으로 서비스별 HOST를 추가한다.

**7.1.2 `package.json`에 자동 생성 훅 추가**

`npm run dev` 또는 `npm run build` 실행 시 `runtime-env.js`가 자동 갱신되도록 npm 훅을 설정한다.

```json
// Vite 프로젝트 (React + Vite, Vue + Vite)
{
  "scripts": {
    "predev": "bash ../tools/generate-runtime-env.sh",
    "dev": "vite",
    "prebuild": "bash ../tools/generate-runtime-env.sh",
    "build": "vite build"
  }
}

// Next.js 프로젝트
{
  "scripts": {
    "predev": "bash ../tools/generate-runtime-env.sh",
    "dev": "next dev",
    "prebuild": "bash ../tools/generate-runtime-env.sh",
    "build": "next build"
  }
}
```

> `predev`/`prebuild`는 npm의 lifecycle hook으로, `dev`/`build` 실행 전 자동으로 호출된다.
> `tools/generate-runtime-env.sh`가 없으면 훅이 실패하므로, Step 8(개발 완료)에서 도구가 복사된 후 정상 동작한다.
> **개발 중에는** `bash tools/generate-runtime-env.sh` 또는 플러그인 경로를 직접 사용할 수 있다.
> Next.js에서는 `<Script src="/runtime-env.js" strategy="beforeInteractive" />`을 layout.tsx에 추가한다 (index.html 대신).

**7.1.3 `.gitignore`에 `runtime-env.js` 등록**

`runtime-env.js`는 `.env` 기반으로 생성되며 실제 키 값을 포함할 수 있으므로 Git에 커밋하지 않는다.

`frontend/.gitignore`에 다음 항목을 추가한다:

```
# runtime-env.js (실제 키 값 포함 — generate-runtime-env.sh로 생성)
/public/runtime-env.js
```

**7.1.4 `index.html`에 script 태그 추가** (앱 번들보다 먼저 로드)

```html
<!-- index.html의 <head> 내부, 앱 스크립트보다 앞에 배치 -->
<script src="/runtime-env.js"></script>
```

**7.1.5 `src/config/runtime.ts` 헬퍼 생성**

(기존 코드 유지 — 변경 없음)
```

### 수동 생성 fallback 유지

기존 수동 생성 패턴(`public/runtime-env.js`를 직접 작성)도 여전히 동작한다. 스크립트는 편의 도구이지 필수가 아니므로, 스크립트 없이 수동으로 `runtime-env.js`를 작성해도 `src/config/runtime.ts` 헬퍼는 동일하게 동작한다.

### 수용 기준

- [ ] 7.1 섹션에 `generate-runtime-env.sh` 기반 자동 생성이 첫 번째 방법으로 안내된다
- [ ] `package.json`의 `predev`/`prebuild` 훅 설정 안내가 포함된다
- [ ] `.gitignore` 등록 안내가 포함된다
- [ ] 기존 `index.html` script 태그와 `runtime.ts` 헬퍼 안내는 유지된다
- [ ] 스크립트 없이 수동 생성도 가능하다는 fallback 안내가 있다

---

## Task 4. frontend-env-setup-vue.md 7단계 수정

**파일**: `resources/guides/develop/frontend-env-setup-vue.md`

### 변경 범위: 7.1 런타임 환경변수 설정 (412~454행)

Task 3과 동일한 패턴으로 변경한다. 차이점:

| 항목 | React | Vue |
|------|-------|-----|
| dev 명령 | `vite` 또는 `next dev` | `vite` |
| build 명령 | `vite build` 또는 `next build` | `vite build` |
| 나머지 | 동일 | 동일 |

### 수용 기준

- [ ] Task 3과 동일한 구조로 7.1 섹션이 수정된다
- [ ] Vue 프로젝트의 `package.json` scripts 예시가 정확하다 (`vite`, `vite build`)

---

## Task 5. frontend-integration-react.md 3단계 수정

**파일**: `resources/guides/develop/frontend-integration-react.md`

### 변경 범위: 3단계 환경변수 전환 (162~206행)

### AS-IS 핵심

- 3.1: `runtime-env.js` 파일을 직접 열어 서비스별 HOST를 수동으로 실제 포트로 변경
- Mock 환경 복귀도 수동으로 HOST를 `http://localhost:4010`으로 되돌림

### TO-BE

3.1 섹션에 스크립트 기반 전환을 첫 번째 방법으로 추가한다:

```markdown
#### 3.1 runtime-env.js 값 교체

**방법 A: generate-runtime-env.sh 사용 (권장)**

ROOT `.env`에 서비스별 실제 포트가 이미 정의되어 있으므로, 스크립트를 재실행하면 실제 API URL이 자동 반영된다.

```bash
bash tools/generate-runtime-env.sh
```

Mock 단계에서 Prism URL(`http://localhost:4010`)을 사용하도록 수동 수정했다면,
스크립트 재실행만으로 `.env` 기반의 실제 포트로 자동 전환된다.

> `package.json`에 `predev` 훅이 설정되어 있으면 `npm run dev` 실행 시 자동으로 갱신된다.

**방법 B: 수동 교체**

`frontend/public/runtime-env.js` 파일에서 서비스별 HOST를 실제 백엔드 URL로 직접 변경한다.

(기존 수동 교체 내용 유지)
```

### 수용 기준

- [ ] 3.1 섹션에 스크립트 기반 전환이 "방법 A (권장)"로 안내된다
- [ ] 기존 수동 교체 방식이 "방법 B"로 유지된다
- [ ] `predev` 훅 관련 안내가 포함된다

---

## Task 6. frontend-integration-vue.md 3단계 수정

**파일**: `resources/guides/develop/frontend-integration-vue.md`

### 변경 범위: 3단계 환경변수 전환 (147~173행)

Task 5와 동일한 패턴으로 변경한다.

### 수용 기준

- [ ] Task 5와 동일한 구조로 3.1 섹션이 수정된다

---

## 제약 사항 (Guardrails)

### Must Have

- `generate-runtime-env.sh`는 `.env`의 `*_SERVICE_PORT` 패턴으로 서비스를 자동 탐지한다 (하드코딩 금지)
- 서버 전용 API 키(GEMINI, OPENAI 등)가 `runtime-env.js`에 노출되지 않는다
- 기존 수동 생성 패턴과 호환된다 (스크립트 없이도 동작)
- Flutter 가이드는 변경하지 않는다 (Dart 환경변수 체계 사용)
- Git Bash (Windows) + Mac/Linux 호환

### Must NOT Have

- 런치픽 프로젝트 종속 코드 (특정 서비스명 하드코딩)
- 기존 가이드의 수동 방식 삭제 (fallback으로 유지)
- `frontend-env-setup-flutter.md`, `frontend-integration-flutter.md` 변경

---

## 검증 방법

### 1. 범용 스크립트 단독 검증

```bash
# 테스트용 프로젝트 구조 생성
mkdir -p /tmp/test-project/frontend/public
cat > /tmp/test-project/.env <<'EOF'
MEMBER_SERVICE_PORT=8081
RECOMMENDATION_SERVICE_PORT=8082
PAYMENT_SERVICE_PORT=8083
KAKAO_CLIENT_ID=test_kakao_id
KAKAO_API_KEY=test_kakao_key
GEMINI_API_KEY=should_not_appear
OPENAI_API_KEY=should_not_appear
EOF

# 프로젝트 루트에서 실행 (CWD 기반)
cd /tmp/test-project
bash {PLUGIN_DIR}/resources/tools/customs/general/generate-runtime-env.sh

# 출력 확인
cat /tmp/test-project/frontend/public/runtime-env.js
# MEMBER_HOST, RECOMMENDATION_HOST, PAYMENT_HOST, KAKAO_CLIENT_ID, KAKAO_API_KEY 존재
# GEMINI_API_KEY, OPENAI_API_KEY 미존재 확인
```

### 2. SKILL.md 구조 검증

- Step 8 항목 2번에서 단일 파일이 아닌 디렉토리 전체 복사가 명시되어 있는지 확인
- `__pycache__/` 제외 조건이 포함되어 있는지 확인

### 3. 프론트엔드 가이드 검증

- `frontend-env-setup-react.md`와 `frontend-env-setup-vue.md`의 7.1 섹션에 스크립트 안내가 있는지 확인
- `package.json` 훅 예시가 포함되어 있는지 확인
- `.gitignore` 안내가 포함되어 있는지 확인
- `frontend-integration-react.md`와 `frontend-integration-vue.md`의 3.1 섹션에 스크립트 기반 전환이 "방법 A"로 안내되는지 확인

### 4. Flutter 가이드 미변경 검증

- `frontend-env-setup-flutter.md`와 `frontend-integration-flutter.md`가 변경되지 않았는지 확인

---

## 성공 기준

- [ ] `resources/tools/customs/general/generate-runtime-env.sh`가 범용적으로 동작한다
- [ ] SKILL.md Step 8이 디렉토리 전체 복사 방식으로 변경되었다
- [ ] React/Vue 프론트엔드 가이드에 스크립트 기반 자동화가 안내된다
- [ ] 기존 수동 방식이 fallback으로 유지된다
- [ ] Flutter 가이드는 변경되지 않았다
- [ ] 특정 프로젝트(런치픽)에 종속된 코드가 없다
