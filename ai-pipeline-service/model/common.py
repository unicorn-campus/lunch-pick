"""공통 Enum 및 상수."""

from enum import Enum


class WeatherCondition(str, Enum):
    CLEAR = "CLEAR"
    CLOUDY = "CLOUDY"
    RAINY = "RAINY"
    SNOWY = "SNOWY"
    WINDY = "WINDY"
    HOT = "HOT"
    COLD = "COLD"


class Satisfaction(str, Enum):
    GOOD = "GOOD"
    BAD = "BAD"
    NEUTRAL = "NEUTRAL"


class AiSource(str, Enum):
    LLM = "LLM"
    CACHE = "CACHE"
    STALE_CACHE = "STALE_CACHE"
    FALLBACK_RULE_BASED = "FALLBACK_RULE_BASED"
    COLD_START_LLM = "COLD_START_LLM"


class CBStateEnum(str, Enum):
    CLOSED = "CLOSED"
    OPEN = "OPEN"
    HALF_OPEN = "HALF_OPEN"


class DietType(str, Enum):
    GENERAL = "일반"
    VEGETARIAN = "채식"
    VEGAN = "비건"
    HALAL = "할랄"
    OTHER = "기타"


class ContextTag(str, Enum):
    WEATHER = "날씨"
    HISTORY = "이력"
    TASTE = "취향"
    WEEKDAY = "요일"
    TIME = "시간"


COLD_START_TAG = "아직 취향을 학습 중이에요. 3일만 더 기록하면 추천이 확 달라져요!"
COLD_START_FEEDBACK_THRESHOLD = 5
