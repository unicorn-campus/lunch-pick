"""WeatherContext Pydantic 모델."""

from pydantic import BaseModel, Field

from model.common import WeatherCondition


class WeatherContext(BaseModel):
    condition: WeatherCondition
    temperature_celsius: float = Field(alias="temperatureCelsius")
    description: str | None = None

    model_config = {"populate_by_name": True}
