"""AiReasonResponse Pydantic 모델."""

from pydantic import BaseModel, Field

from model.ai_metadata import AiMetadata


class ParsedReason(BaseModel):
    """LLM 이유 JSON 응답 파싱용 내부 모델."""

    natural_language_reason: str = Field(max_length=200)
    confidence_score: int = Field(ge=0, le=100)
    context_tags: list[str] = Field(default_factory=list)


class AiReasonResponse(BaseModel):
    recommendation_id: str = Field(alias="recommendationId")
    natural_language_reason: str = Field(alias="naturalLanguageReason")
    confidence_score: int = Field(alias="confidenceScore", ge=0, le=100)
    context_tags: list[str] = Field(default_factory=list, alias="contextTags")
    is_reason_ready: bool = Field(default=True, alias="isReasonReady")
    fallback_reason: str | None = Field(default=None, alias="fallbackReason")
    cached_until: str | None = Field(default=None, alias="cachedUntil")
    metadata: AiMetadata

    model_config = {"populate_by_name": True}
