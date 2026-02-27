"""AiRecommendationRequest Pydantic 모델 (ai-pipeline-api.yaml 기반)."""

from datetime import datetime

from pydantic import BaseModel, Field

from model.common import DietType
from model.weather_context import WeatherContext


class OnboardingSwipeData(BaseModel):
    card_id: str = Field(alias="cardId")
    category: str
    liked: bool

    model_config = {"populate_by_name": True}


class RecentMealHistory(BaseModel):
    restaurant_id: str = Field(alias="restaurantId")
    category: str
    meal_date: str = Field(alias="mealDate")
    satisfaction: str | None = None

    model_config = {"populate_by_name": True}


class AvailableRestaurant(BaseModel):
    restaurant_id: str = Field(alias="restaurantId")
    restaurant_name: str = Field(alias="restaurantName")
    representative_menu: str = Field(alias="representativeMenu")
    category: str
    distance_meters: int = Field(alias="distanceMeters")
    estimated_walk_minutes: int = Field(alias="estimatedWalkMinutes")
    allergens: list[str] = Field(default_factory=list)

    model_config = {"populate_by_name": True}


class AiRecommendationRequest(BaseModel):
    member_id: str = Field(alias="memberId")
    latitude: float
    longitude: float
    requested_at: datetime = Field(alias="requestedAt")
    is_cold_start: bool = Field(alias="isColdStart")
    feedback_count: int | None = Field(default=None, alias="feedbackCount")
    taste_vector: dict[str, float] | None = Field(default=None, alias="tasteVector")
    onboarding_swipes: list[OnboardingSwipeData] | None = Field(
        default=None, alias="onboardingSwipes"
    )
    allergen_filter: list[str] = Field(default_factory=list, alias="allergenFilter")
    diet_type: str | None = Field(default=None, alias="dietType")
    weather: WeatherContext | None = None
    recent_meal_history: list[RecentMealHistory] = Field(
        default_factory=list, alias="recentMealHistory"
    )
    exclude_restaurant_ids: list[str] = Field(
        default_factory=list, alias="excludeRestaurantIds"
    )
    job_cluster: str | None = Field(default=None, alias="jobCluster")
    available_restaurants: list[AvailableRestaurant] | None = Field(
        default=None, alias="availableRestaurants"
    )
    skip_cache: bool = Field(default=False, alias="skipCache")

    model_config = {"populate_by_name": True}
