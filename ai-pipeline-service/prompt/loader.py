"""
프롬프트 템플릿 로더.

파일 기반 프롬프트 관리:
  - templates/ 디렉토리에서 .txt 파일 로드
  - string.Template으로 변수 치환
  - SHA-256 해시 12자리 반환 (캐시 키 생성용)
"""

import hashlib
import logging
from pathlib import Path
from string import Template

logger = logging.getLogger(__name__)

TEMPLATE_DIR = Path(__file__).parent / "templates"

# 현재 운영 버전
ACTIVE_RECOMMENDATION_TEMPLATE = "recommendation-system-v1.0.txt"
ACTIVE_COLDSTART_TEMPLATE = "recommendation-coldstart-v1.0.txt"
ACTIVE_REASON_TEMPLATE = "reason-system-v1.0.txt"
ACTIVE_INSIGHT_TEMPLATE = "insight-system-v1.0.txt"


def load_template(template_name: str) -> str:
    """템플릿 파일 로드."""
    path = TEMPLATE_DIR / template_name
    if not path.exists():
        raise FileNotFoundError(f"프롬프트 템플릿 파일 없음: {path}")
    return path.read_text(encoding="utf-8")


def render_prompt(template_name: str, variables: dict) -> tuple[str, str]:
    """프롬프트 렌더링 후 (rendered_prompt, prompt_hash) 반환.

    prompt_hash는 SHA-256 앞 12자리이며 캐시 키 생성에 사용된다.
    변수가 없는 시스템 프롬프트는 variables={}로 호출한다.
    """
    template_str = load_template(template_name)
    try:
        rendered = Template(template_str).safe_substitute(variables)
    except Exception as exc:
        logger.warning("프롬프트 변수 치환 오류 (template=%s): %s", template_name, exc)
        rendered = template_str

    prompt_hash = hashlib.sha256(rendered.encode("utf-8")).hexdigest()[:12]
    return rendered, prompt_hash


def load_system_prompt(template_name: str) -> str:
    """시스템 프롬프트 파일 로드 (변수 치환 없음)."""
    return load_template(template_name)


def render_user_prompt(template_name: str, variables: dict) -> str:
    """사용자 프롬프트 렌더링 (변수 치환 포함)."""
    rendered, _ = render_prompt(template_name, variables)
    return rendered
