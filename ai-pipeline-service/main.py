"""
ai-pipeline-service FastAPI 앱 진입점.

내부 전용 서비스 — 외부 클라이언트 접근 불가.
recommendation-service 전용 내부 API (2개 엔드포인트).

엔드포인트:
  POST /api/v1/ai/recommendations        — AI 추천 생성
  POST /api/v1/ai/recommendation-reason  — 추천 이유 생성
  GET  /health                           — 헬스체크 (VPC 내부 전용)
"""

import logging

from contextlib import asynccontextmanager
from fastapi import FastAPI

from config import settings
from router.health import router as health_router
from router.recommendation_router import router as recommendation_router
from router.reason_router import router as reason_router
from router.insight_router import router as insight_router

logger = logging.getLogger(__name__)


async def _probe_llm_api_key() -> bool:
    """앱 시작 시 LLM API 키 실제 유효성을 검증한다.

    최소 비용 요청(max_tokens=1)으로 401 여부만 확인.
    성공 또는 401 외 오류(429, 503 등)이면 True 반환(키 존재로 간주).
    401이면 False 반환.
    """
    if not settings.is_llm_key_present:
        logger.warning("LLM API 키 미설정 → Mock LLM 엔진 모드")
        return False

    # OpenAI/Groq 호환 API 키가 있으면 해당 방식으로 probe
    oai_key = settings.openai_api_key.strip()
    if oai_key:
        try:
            from openai import AsyncOpenAI
            base_url = settings.openai_api_base.strip() or None
            client = AsyncOpenAI(api_key=oai_key, base_url=base_url)
            await client.chat.completions.create(
                model=settings.primary_model_id,
                max_tokens=1,
                messages=[{"role": "user", "content": "hi"}],
            )
            logger.info("LLM API 키 검증 성공 (OpenAI/Groq) → 실제 LLM 모드")
            return True
        except Exception as exc:
            exc_str = str(exc).lower()
            if "401" in exc_str or "authentication" in exc_str or "invalid" in exc_str:
                logger.warning("LLM API 키 인증 실패 (OpenAI/Groq) → Mock LLM 엔진 모드: %s", exc)
                return False
            logger.warning("LLM API 키 probe 중 일시 오류 (키 유효로 간주): %s", exc)
            return True

    # Anthropic API 키로 probe
    try:
        import anthropic
        client = anthropic.AsyncAnthropic(api_key=settings.anthropic_api_key)
        await client.messages.create(
            model=settings.primary_model_id,
            max_tokens=1,
            messages=[{"role": "user", "content": "hi"}],
        )
        logger.info("LLM API 키 검증 성공 (Anthropic) → 실제 LLM 모드")
        return True
    except anthropic.AuthenticationError:
        logger.warning("LLM API 키 인증 실패(401) → Mock LLM 엔진 모드")
        return False
    except Exception as exc:
        logger.warning("LLM API 키 probe 중 일시 오류 (키 유효로 간주): %s", exc)
        return True


@asynccontextmanager
async def lifespan(app: FastAPI):
    # startup
    llm_available = await _probe_llm_api_key()
    # 전역 플래그를 settings 객체에 동적으로 주입
    object.__setattr__(settings, "_llm_available_verified", llm_available)
    yield
    # shutdown (필요 시 정리 로직 추가)


app = FastAPI(
    title="ai-pipeline-service",
    version="1.0.0",
    description="LunchPick AI Pipeline — LLM 호출, 프롬프트 관리, 추천 이유 생성, 폴백 추천",
    docs_url="/docs" if settings.app_env != "production" else None,
    redoc_url="/redoc" if settings.app_env != "production" else None,
    lifespan=lifespan,
)

# --- 라우터 등록 ---

# 헬스체크
app.include_router(health_router)

# AI 추천 생성 + 이유 생성
app.include_router(recommendation_router, prefix="/api/v1")
app.include_router(reason_router, prefix="/api/v1")
app.include_router(insight_router, prefix="/api/v1")
