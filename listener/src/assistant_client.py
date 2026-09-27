"""Talks to the already-deployed Railway backend — the exact same
/api/assistant/query endpoint the iOS app uses. One backend, two front
ends (phone app and this counter device); neither reimplements the
Claude + FrameKraft-tools logic.
"""
import requests


class AssistantError(Exception):
    pass


def ask_assistant(backend_url: str, api_key: str, transcript: str) -> str:
    try:
        response = requests.post(
            f"{backend_url}/api/assistant/query",
            headers={"x-assistant-key": api_key, "Content-Type": "application/json"},
            json={"transcript": transcript},
            timeout=30,
        )
    except requests.RequestException as exc:
        raise AssistantError(f"Couldn't reach the backend: {exc}") from exc

    if response.status_code != 200:
        raise AssistantError(f"Backend returned {response.status_code}: {response.text}")

    data = response.json()
    return data.get("reply", "")
