"""Entry point: the always-on loop that makes this a real "Hey <wake word>"
counter device.

    1. Sit listening for the wake word (openWakeWord, always running).
    2. On detection, record the spoken request (stops on silence).
    3. Transcribe it (OpenAI Whisper).
    4. Send the text to the FrameCraft voice-assistant backend on Railway
       (same brain the iOS app uses — real Claude + FrameKraft tools).
    5. Speak the reply back (OpenAI TTS) through the device's speaker.
    6. Go back to listening.

Designed to run as a systemd service on a Raspberry Pi (or any Linux
box with a mic + speaker) so it survives reboots and crashes. See
README.md for hardware setup and the systemd unit file.
"""
import sys
import traceback

from openai import OpenAI

from assistant_client import AssistantError, ask_assistant
from config import load_config
from record import record_utterance
from speak import speak
from transcribe import transcribe
from wake_word import WakeWordListener


def main() -> None:
    config = load_config()
    openai_client = OpenAI(api_key=config.openai_api_key)

    print(f"Loading wake-word model '{config.wake_word_model}'...")
    listener = WakeWordListener(
        model_name=config.wake_word_model,
        threshold=config.wake_word_threshold,
        input_device=config.input_device,
    )
    print("Ready. Listening for the wake word.")

    while True:
        try:
            listener.wait_for_wake()
            print("Wake word detected — listening for your request...")

            audio = record_utterance(config.input_device)
            if audio.size == 0:
                print("Didn't catch anything, going back to listening.")
                continue

            transcript = transcribe(openai_client, audio)
            if not transcript:
                print("Couldn't make out any speech, going back to listening.")
                continue
            print(f"Heard: {transcript!r}")

            reply = ask_assistant(config.backend_url, config.assistant_api_key, transcript)
            print(f"Reply: {reply!r}")

            speak(openai_client, reply, config.output_device)

        except AssistantError as exc:
            print(f"[assistant error] {exc}", file=sys.stderr)
            speak(openai_client, "Sorry, I couldn't reach the assistant.", config.output_device)
        except KeyboardInterrupt:
            print("\nShutting down.")
            break
        except Exception:
            # A device meant to run unattended must not die on one bad
            # turn — log it and keep listening rather than crash the loop.
            print("[unexpected error]", file=sys.stderr)
            traceback.print_exc()


if __name__ == "__main__":
    main()
