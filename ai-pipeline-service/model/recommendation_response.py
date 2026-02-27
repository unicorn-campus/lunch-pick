"""AiRecommendationResponse, RecommendedRestaurant Pydantic 모델."""

from pydantic import BaseModel, Field

from model.ai_metadata import AiMetadata


class RecommendedRestaurant(BaseModel):
    restaurant_id: str = Field(alias="restaurantId")
    restaurant_name: str = Field(alias="restaurantName")
    representative_menu: str = Field(alias="representativeMenu")
    category: str
    reason_summary: str = Field(alias="reasonSummary", max_length=80)
    confidence_score: int = Field(alias="confidenceScore", ge=0, le=100)
    distance_meters: int | None = Field(default=None, alias="distanceMeters")
    estimated_walk_minutes: int | None = Field(default=None, alias="estimatedWalkMinutes")

    model_config = {"populate_by_name": True}


class LLMRecommendationOutput(BaseModel):
    """LLM JSON 응답 파싱용 내부 모델."""

    restaurant_id: str
    restaurant_name: str
    representative_menu: str
    category: str
    reason_summary: str = Field(max_length=80)
    confidence_score: int = Field(ge=0, le=100)


class LLMRecommendationResponse(BaseModel):
    """LLM 전체 JSON 응답."""

    recommendations: list[LLMRecommendationOutput] = Field(min_length=1, max_length=3)


class AiRecommendationResponse(BaseModel):
    recommendations: list[RecommendedRestaurant] = Field(min_length=1, max_length=3)
    is_fallback: bool = Field(default=False, alias="isFallback")
    is_cold_start: bool = Field(default=False, alias="isColdStart")
    cold_start_tag: str | None = Field(default=None, alias="coldStartTag")
    cache_key: str = Field(alias="cacheKey")
    cached_until: str | None = Field(default=None, alias="cachedUntil")
    metadata: AiMetadata

    model_config = {"populate_by_name": True}
