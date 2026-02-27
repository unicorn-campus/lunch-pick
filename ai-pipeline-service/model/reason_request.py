"""AiReasonRequest Pydantic 모델."""

from pydantic import BaseModel, Field

from model.recommendation_request import RecentMealHistory
from model.weather_context import WeatherContext


class AiReasonRequest(BaseModel):
    recommendation_id: str = Field(alias="recommendationId")
    restaurant_id: str = Field(alias="restaurantId")
    restaurant_name: str = Field(alias="restaurantName")
    category: str
    member_id: str = Field(alias="memberId")
    taste_vector: dict[str, float] | None = Field(default=None, alias="tasteVector")
    weather: WeatherContext | None = None
    recent_meal_history: list[RecentMealHistory] | None = Field(
        default=None, alias="recentMealHistory"
    )
    confidence_score: int | None = Field(default=None, alias="confidenceScore", ge=0, le=100)
    representative_menu: str | None = Field(default=None, alias="representativeMenu")

    model_config = {"populate_by_name": True}
