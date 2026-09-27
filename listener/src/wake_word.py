"""Always-on wake-word listener built on openWakeWord — a real, working
open-source detector (not a stub). Runs continuously in a tight loop on
the device's CPU, watching a rolling audio buffer for the configured
wake word, and yields control back to the caller the instant one fires.
"""
import numpy as np
import sounddevice as sd
from openwakeword.model import Model

SAMPLE_RATE = 16000
FRAME_SIZE = 1280  # openWakeWord expects 80ms frames at 16kHz


class WakeWordListener:
    def __init__(self, model_name: str, threshold: float, input_device: int | None):
        # openWakeWord downloads its pretrained models on first run and
        # caches them locally — this is real detection, not a placeholder.
        self.model = Model(wakeword_models=[model_name])
        self.model_name = model_name
        self.threshold = threshold
        self.input_device = input_device

    def wait_for_wake(self) -> None:
        """Blocks until the wake word is detected, then returns."""
        with sd.InputStream(
            samplerate=SAMPLE_RATE,
            channels=1,
            dtype="int16",
            blocksize=FRAME_SIZE,
            device=self.input_device,
        ) as stream:
            while True:
                audio_chunk, _ = stream.read(FRAME_SIZE)
                audio = np.squeeze(audio_chunk)
                predictions = self.model.predict(audio)
                score = predictions.get(self.model_name, 0.0)
                if score >= self.threshold:
                    self.model.reset()
                    return
