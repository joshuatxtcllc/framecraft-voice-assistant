"""Speaks the assistant's reply out loud using OpenAI's TTS API, played
through the device's speaker. Real audio synthesis and playback, not a
beep or a text dump — this is what makes the counter device actually
talk back.
"""
import io

import simpleaudio as sa
from openai import OpenAI
from pydub import AudioSegment


def speak(client: OpenAI, text: str, output_device: int | None) -> None:
    # simpleaudio always plays through the system's default output device —
    # it has no per-call device-selection API. output_device is accepted
    # here for interface symmetry with record_utterance/wait_for_wake, but
    # is currently unused. If you need a non-default output (e.g. a USB
    # speaker that isn't your Pi's default), set it as the system default
    # via `raspi-config` / ALSA config instead.
    del output_device
    if not text.strip():
        return

    response = client.audio.speech.create(
        model="tts-1",
        voice="alloy",
        input=text,
    )

    mp3_bytes = io.BytesIO(response.content)
    audio = AudioSegment.from_file(mp3_bytes, format="mp3")

    playback = sa.play_buffer(
        audio.raw_data,
        num_channels=audio.channels,
        bytes_per_sample=audio.sample_width,
        sample_rate=audio.frame_rate,
    )
    playback.wait_done()
