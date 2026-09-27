"""Turns recorded audio into text using OpenAI's Whisper API — real
transcription, not a mock. Kept server-side (OpenAI, not local) because
a Pi-class CPU is too slow for accurate local Whisper inference in
real time; this trades a small network round-trip for speed and
accuracy that's actually usable in a shop.
"""
import io
import wave

import numpy as np
from openai import OpenAI

from wake_word import SAMPLE_RATE


def _to_wav_bytes(audio: np.ndarray) -> bytes:
    buffer = io.BytesIO()
    with wave.open(buffer, "wb") as wav_file:
        wav_file.setnchannels(1)
        wav_file.setsampwidth(2)  # int16
        wav_file.setframerate(SAMPLE_RATE)
        wav_file.writeframes(audio.tobytes())
    return buffer.getvalue()


def transcribe(client: OpenAI, audio: np.ndarray) -> str:
    if audio.size == 0:
        return ""

    wav_bytes = _to_wav_bytes(audio)
    wav_bytes_io = io.BytesIO(wav_bytes)
    wav_bytes_io.name = "utterance.wav"  # the SDK reads this for the multipart filename

    result = client.audio.transcriptions.create(
        model="whisper-1",
        file=wav_bytes_io,
    )
    return result.text.strip()
