"""AiMetadata, TokenUsage Pydantic 모델."""

from pydantic import BaseModel

from model.common import AiSource, CBStateEnum


class TokenUsage(BaseModel):
    prompt_tokens: int = 0
    completion_tokens: int = 0
    total_tokens: int = 0


class AiMetadata(BaseModel):
    source: AiSource
    model_used: str | None = None
    latency_ms: int
    token_usage: TokenUsage
    circuit_breaker_state: CBStateEnum = CBStateEnum.CLOSED
