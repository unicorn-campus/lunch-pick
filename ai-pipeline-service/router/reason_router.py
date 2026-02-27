"""
POST /api/v1/ai/recommendation-reason 엔드포인트.

내부 전용 API — recommendation-service 전용.
LLM 실패 시에도 200 반환 (isReasonReady: false, fallbackReason 포함).
"""

import logging

from fastapi import APIRouter, HTTPException, status

from model.reason_request import AiReasonRequest
from model.reason_response import AiReasonResponse
from service.reason_service import ReasonService

logger = logging.getLogger(__name__)

router = APIRouter(tags=["Reason"])
_service = ReasonService()


@router.post(
    "/ai/recommendation-reason",
    response_model=AiReasonResponse,
    status_code=status.HTTP_200_OK,
    summary="추천 이유 생성 (내부 API)",
    description=(
        "추천·이력 서비스가 추천 이유 상세 조회 시 호출하는 내부 API. "
        "LLM 실패 시에도 200 반환 (isReasonReady: false)."
    ),
    response_model_by_alias=True,
)
async def generate_recommendation_reason(
    request: AiReasonRequest,
) -> AiReasonResponse:
    """추천 이유 생성 엔드포인트.

    처리 순서:
      1. Redis 캐시 확인 (reason:{recommendationId})
      2. LLM Circuit Breaker 확인
      3. LLM 호출 → 파싱 → 캐시 저장
      4. 실패 시: 기본 이유 반환 (isReasonReady: false)
    """
    try:
        return await _service.generate_reason(request)
    except Exception as exc:
        logger.error("이유 생성 예상치 못한 오류: %s", exc, exc_info=True)
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="추천 이유 생성 중 오류가 발생했습니다.",
        ) from exc
