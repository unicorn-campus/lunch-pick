"""
POST /api/v1/ai/recommendations 엔드포인트.

내부 전용 API — recommendation-service 전용.
"""

import logging

from fastapi import APIRouter, HTTPException, status

from model.recommendation_request import AiRecommendationRequest
from model.recommendation_response import AiRecommendationResponse
from service.recommendation_service import RecommendationService

logger = logging.getLogger(__name__)

router = APIRouter(tags=["Recommendation"])
_service = RecommendationService()


@router.post(
    "/ai/recommendations",
    response_model=AiRecommendationResponse,
    status_code=status.HTTP_200_OK,
    summary="AI 추천 생성 요청 (내부 API)",
    description=(
        "추천·이력 서비스가 호출하는 내부 API. "
        "LLM 장애 또는 Circuit Breaker Open 시에도 200 반환 (폴백 추천 포함)."
    ),
    response_model_by_alias=True,
)
async def generate_ai_recommendations(
    request: AiRecommendationRequest,
) -> AiRecommendationResponse:
    """AI 추천 생성 엔드포인트.

    처리 순서:
      1. Redis 캐시 확인
      2. 콜드스타트 분기 (isColdStart 또는 feedbackCount < 5)
      3. LLM Circuit Breaker 확인
      4. LLM 호출 → 파싱 → 캐시 저장
      5. Open 시: Stale 캐시 → 규칙 기반 폴백
    """
    try:
        return await _service.generate(request)
    except Exception as exc:
        logger.error("추천 생성 예상치 못한 오류: %s", exc, exc_info=True)
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="추천 생성 중 오류가 발생했습니다.",
        ) from exc
