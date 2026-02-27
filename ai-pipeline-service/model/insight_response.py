"""AiInsightResponse Pydantic 모델."""

from pydantic import BaseModel, Field

from model.ai_metadata import AiMetadata


class MealBalance(BaseModel):
    diversity_score: int = Field(alias="diversityScore")
    diagnosis: str
    coaching_comment: str = Field(alias="coachingComment")

    model_config = {"populate_by_name": True}


class SatisfactionAnalysis(BaseModel):
    satisfaction_rate: int = Field(alias="satisfactionRate")
    patterns: list[str]
    pattern_comment: str = Field(alias="patternComment")

    model_config = {"populate_by_name": True}


class AiInsightResponse(BaseModel):
    weekly_summary: str = Field(alias="weeklySummary")
    meal_balance: MealBalance = Field(alias="mealBalance")
    satisfaction_analysis: SatisfactionAnalysis = Field(alias="satisfactionAnalysis")
    metadata: AiMetadata

    model_config = {"populate_by_name": True}
