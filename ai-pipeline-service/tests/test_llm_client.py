"""LLMClient + Circuit Breaker 연동 단위 테스트."""

import pytest
from unittest.mock import AsyncMock, MagicMock, patch

from llm.circuit_breaker import CBState, CircuitBreaker
from llm.llm_client import LLMClient, LLMInvokeError


def _make_mock_llm_response(content: str) -> MagicMock:
    mock_response = MagicMock()
    mock_response.content = content
    mock_response.usage_metadata = MagicMock(input_tokens=100, output_tokens=50)
    mock_response.response_metadata = {"usage": {"input_tokens": 100, "output_tokens": 50}}
    return mock_response


@pytest.fixture
def llm_client_with_mock_models():
    """실제 API 초기화 없이 LLMClient 생성."""
    with patch("llm.llm_client.init_chat_model") as mock_init:
        mock_model = AsyncMock()
        mock_init.return_value = mock_model
        client = LLMClient()
        yield client, mock_model


@pytest.mark.asyncio
async def test_circuit_breaker_open_blocks_call(llm_client_with_mock_models):
    client, _ = llm_client_with_mock_models
    # CircuitBreaker.is_open()을 직접 패치하여 OPEN 상태 시뮬레이션
    with patch.object(client._circuit_breaker, "is_open", return_value=True):
        with pytest.raises(LLMInvokeError, match="Circuit Breaker OPEN"):
            await client.generate_recommendation("sys", "user")


@pytest.mark.asyncio
async def test_successful_call_records_success(llm_client_with_mock_models):
    client, mock_model = llm_client_with_mock_models
    mock_response = _make_mock_llm_response('{"recommendations": []}')
    mock_model.ainvoke.return_value = mock_response

    # CB가 닫힌 상태에서 성공
    client._circuit_breaker.reset()
    raw, token_usage = await client.generate_recommendation("sys", "user")

    assert raw == '{"recommendations": []}'
    assert client._circuit_breaker.state == CBState.CLOSED


@pytest.mark.asyncio
async def test_failure_increments_circuit_breaker_count(llm_client_with_mock_models):
    client, mock_model = llm_client_with_mock_models
    # 모든 모델 호출이 실패하도록 설정
    mock_model.ainvoke.side_effect = Exception("503 Service Unavailable")
    client._circuit_breaker.reset()

    with pytest.raises(LLMInvokeError):
        await client.generate_recommendation("sys", "user")

    assert client._circuit_breaker.failure_count >= 1


@pytest.mark.asyncio
async def test_non_retryable_error_no_retry(llm_client_with_mock_models):
    client, mock_model = llm_client_with_mock_models
    # 401은 재시도 없이 즉시 실패
    mock_model.ainvoke.side_effect = Exception("401 Authentication failed")
    client._circuit_breaker.reset()

    with pytest.raises(LLMInvokeError):
        await client.generate_recommendation("sys", "user")

    # 재시도 없이 1회만 호출됨 (primary + fallback 각 1회)
    assert mock_model.ainvoke.call_count <= 2


@pytest.mark.asyncio
async def test_extract_token_usage(llm_client_with_mock_models):
    client, _ = llm_client_with_mock_models
    mock_response = MagicMock()
    mock_response.usage_metadata = MagicMock(input_tokens=300, output_tokens=100)

    usage = client._extract_token_usage(mock_response)
    assert usage["prompt_tokens"] == 300
    assert usage["completion_tokens"] == 100
    assert usage["total_tokens"] == 400


def test_is_retryable_503(llm_client_with_mock_models):
    client, _ = llm_client_with_mock_models
    assert client._is_retryable(Exception("503 Service Unavailable")) is True


def test_is_retryable_timeout(llm_client_with_mock_models):
    client, _ = llm_client_with_mock_models
    assert client._is_retryable(Exception("timeout error")) is True


def test_is_not_retryable_401(llm_client_with_mock_models):
    client, _ = llm_client_with_mock_models
    assert client._is_retryable(Exception("401 authentication failed")) is False
