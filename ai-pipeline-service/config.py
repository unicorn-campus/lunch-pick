from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    model_config = SettingsConfigDict(
        env_file=".env",
        env_file_encoding="utf-8",
        case_sensitive=False,
    )

    # 앱 설정
    app_env: str = "development"
    app_port: int = 8000
    log_level: str = "INFO"

    # LLM 설정 — 일반 추천/이유 생성 (Claude 3.5 Haiku)
    primary_model_id: str = "claude-3-5-haiku-20241022"
    primary_model_provider: str = "anthropic"
    primary_model_temperature: float = 0.3
    primary_model_max_tokens: int = 1024

    # LLM 설정 — 콜드스타트 (Claude 3.5 Sonnet)
    coldstart_model_id: str = "claude-3-5-sonnet-20241022"
    coldstart_model_provider: str = "anthropic"
    coldstart_model_temperature: float = 0.4
    coldstart_model_max_tokens: int = 1024

    # LLM 설정 — 이유 생성 (Claude 3.5 Haiku, max_tokens 절감)
    reason_model_id: str = "claude-3-5-haiku-20241022"
    reason_model_provider: str = "anthropic"
    reason_model_temperature: float = 0.5
    reason_model_max_tokens: int = 512

    # LLM API 키
    anthropic_api_key: str = ""
    openai_api_key: str = ""
    openai_api_base: str = ""

    @property
    def is_llm_key_present(self) -> bool:
        """API 키 문자열이 존재하고 형식이 올바른지 확인 (실제 유효성은 미검증)."""
        # Anthropic 키 확인
        ant_key = self.anthropic_api_key.strip()
        if ant_key and len(ant_key) >= 90 and (ant_key.startswith("sk-ant-api03-") or ant_key.startswith("sk-ant-")):
            return True
        # OpenAI/Groq 키 확인
        oai_key = self.openai_api_key.strip()
        if oai_key and len(oai_key) >= 20:
            return True
        return False

    @property
    def is_llm_available(self) -> bool:
        """런타임 probe 결과 기반 LLM 가용성 반환.

        main.py lifespan 에서 _probe_llm_api_key() 결과가
        _llm_available_verified 속성으로 주입된다.
        주입 전(테스트 등)에는 is_llm_key_present 로 폴백.
        """
        verified = getattr(self, "_llm_available_verified", None)
        if verified is not None:
            return verified
        return self.is_llm_key_present

    # Redis 설정 (ai-pipeline-service 전용: DB 4)
    redis_host: str = "localhost"
    redis_port: int = 6379
    redis_db: int = 4

    # Circuit Breaker 설정
    cb_failure_threshold: int = 5
    cb_recovery_timeout: float = 60.0

    # Rate Limiting
    rate_limit_global_per_minute: int = 500

    # LangSmith (선택)
    langchain_tracing_v2: bool = False
    langchain_api_key: str = ""
    langchain_project: str = "ai-pipeline-service"


settings = Settings()
