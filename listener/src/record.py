"""Records one spoken phrase after the wake word fires: starts capturing
immediately and stops on ~1.2s of silence (simple RMS-based voice
activity detection — no extra ML model needed for this part).
"""
import numpy as np
import sounddevice as sd

SAMPLE_RATE = 16000
FRAME_MS = 30
FRAME_SIZE = int(SAMPLE_RATE * FRAME_MS / 1000)
SILENCE_RMS_THRESHOLD = 300  # tune to your room/mic if it cuts off too eager or too late
SILENCE_HANG_MS = 1200
MAX_UTTERANCE_SECONDS = 15


def record_utterance(input_device: int | None) -> np.ndarray:
    silence_hang_frames = int(SILENCE_HANG_MS / FRAME_MS)
    max_frames = int(MAX_UTTERANCE_SECONDS * 1000 / FRAME_MS)

    frames: list[np.ndarray] = []
    silent_run = 0
    heard_speech = False

    with sd.InputStream(
        samplerate=SAMPLE_RATE,
        channels=1,
        dtype="int16",
        blocksize=FRAME_SIZE,
        device=input_device,
    ) as stream:
        for _ in range(max_frames):
            chunk, _ = stream.read(FRAME_SIZE)
            chunk = np.squeeze(chunk)
            frames.append(chunk)

            rms = float(np.sqrt(np.mean(chunk.astype(np.float64) ** 2)))
            if rms >= SILENCE_RMS_THRESHOLD:
                heard_speech = True
                silent_run = 0
            elif heard_speech:
                silent_run += 1
                if silent_run >= silence_hang_frames:
                    break

    return np.concatenate(frames) if frames else np.array([], dtype=np.int16)
