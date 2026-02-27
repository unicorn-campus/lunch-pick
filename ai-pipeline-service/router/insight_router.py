"""
POST /api/v1/ai/insights 엔드포인트.

내부 전용 API — recommendation-service 전용.
"""

import logging

from fastapi import APIRouter, HTTPException, status

from model.insight_request import AiInsightRequest
from model.insight_response import AiInsightResponse
from service.insight_service import InsightService

logger = logging.getLogger(__name__)

router = APIRouter(tags=["Insight"])
_service = InsightService()


@router.post(
    "/ai/insights",
    response_model=AiInsightResponse,
    status_code=status.HTTP_200_OK,
    summary="AI 인사이트 분석 요청 (내부 API)",
    description=(
        "추천·이력 서비스가 호출하는 내부 API. "
        "LLM 장애 시에도 200 반환 (규칙 기반 폴백 인사이트 포함)."
    ),
    response_model_by_alias=True,
)
async def generate_ai_insights(
    request: AiInsightRequest,
) -> AiInsightResponse:
    """AI 인사이트 분석 엔드포인트."""
    try:
        return await _service.analyze(request)
    except Exception as exc:
        logger.error("인사이트 분석 예상치 못한 오류: %s", exc, exc_info=True)
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="인사이트 분석 중 오류가 발생했습니다.",
        ) from exc
