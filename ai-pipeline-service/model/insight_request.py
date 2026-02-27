"""AiInsightRequest Pydantic 모델."""

from pydantic import BaseModel, Field


class MealRecord(BaseModel):
    date: str
    restaurant_name: str = Field(alias="restaurantName")
    menu_name: str = Field(alias="menuName")
    category: str
    satisfaction: str | None = None
    keyword: str | None = None

    model_config = {"populate_by_name": True}


class AiInsightRequest(BaseModel):
    member_id: str = Field(alias="memberId")
    recent_meals: list[MealRecord] = Field(alias="recentMeals")
    category_distribution: dict[str, float] = Field(alias="categoryDistribution")
    total_meal_count: int = Field(alias="totalMealCount")
    period_days: int = Field(alias="periodDays", default=30)

    model_config = {"populate_by_name": True}
