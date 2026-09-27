"""Loads and validates environment configuration for the listener.
Fails loudly at startup rather than partway through a listening session —
a misconfigured device should never come up half-working.
"""
import os
import sys
from dataclasses import dataclass

from dotenv import load_dotenv

load_dotenv()


def _required(name: str) -> str:
    value = os.environ.get(name)
    if not value or not value.strip():
        print(f"Missing required environment variable: {name}. Check your .env file.", file=sys.stderr)
        sys.exit(1)
    return value


def _optional(name: str, default: str) -> str:
    value = os.environ.get(name)
    return value if value and value.strip() else default


def _optional_int(name: str) -> int | None:
    value = os.environ.get(name)
    return int(value) if value and value.strip() else None


@dataclass(frozen=True)
class Config:
    backend_url: str
    assistant_api_key: str
    openai_api_key: str
    wake_word_model: str
    wake_word_threshold: float
    input_device: int | None
    output_device: int | None


def load_config() -> Config:
    return Config(
        backend_url=_required("BACKEND_URL").rstrip("/"),
        assistant_api_key=_required("ASSISTANT_API_KEY"),
        openai_api_key=_required("OPENAI_API_KEY"),
        wake_word_model=_optional("WAKE_WORD_MODEL", "hey_jarvis"),
        wake_word_threshold=float(_optional("WAKE_WORD_THRESHOLD", "0.5")),
        input_device=_optional_int("INPUT_DEVICE"),
        output_device=_optional_int("OUTPUT_DEVICE"),
    )
