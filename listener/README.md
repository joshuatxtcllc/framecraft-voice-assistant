# FrameCraft Voice Assistant — Listener (Path B: dedicated always-on device)

This is the real "always listening, say its name, no unlocking anything"
version. It doesn't run on your iPhone — iOS won't allow a third-party
app to listen in the background with the screen off (that's covered in
the main repo README and was the reason for the iOS tap-to-talk app in
`/ios`). This runs on a **separate small always-on device** sitting on
the shop counter, which has no such restriction.

## What it does

A continuous loop:

1. **Wake word detection** — [openWakeWord](https://github.com/dscripka/openWakeWord)
   listens to the mic in real time, always on, no network calls, cheap
   enough to run on a Raspberry Pi's CPU.
2. On wake, **records** what you say next (stops automatically after
   ~1.2s of silence).
3. **Transcribes** it with OpenAI's Whisper API.
4. **Sends the text to the same Railway backend** the iOS app talks to
   (`/server` in the repo root) — same Claude + FrameKraft tools, same
   brain, no duplicated logic.
5. **Speaks the reply back** with OpenAI's TTS API through the device's
   speaker.
6. Goes back to listening.

## Hardware

Any small always-on Linux box with a mic and speaker works. A
**Raspberry Pi 4 or 5** with a USB microphone (or a mic+speaker
combo like a ReSpeaker HAT) and a small speaker is the straightforward,
well-documented option — that's what the setup below assumes. An old
Android phone or a spare laptop running Linux would also work with
minor adjustments.

## About the wake word

openWakeWord ships several real, working pretrained models —
`hey_jarvis`, `alexa`, `hey_mycroft`, `hey_rhasspy`, `timer`. This
listener defaults to **`hey_jarvis`** so it works out of the box the
moment you set it up — this is not a placeholder, it's a real detector.

There is no pretrained "hey FrameCraft" model. To get your own wake
word:

1. Follow openWakeWord's [custom model training guide](https://github.com/dscripka/openWakeWord#training-new-models)
   — it uses synthetic speech generation and a training notebook
   (Google Colab, free tier works) to train a model on your exact
   phrase without needing you to record hundreds of samples yourself.
2. Drop the resulting `.tflite`/`.onnx` model file somewhere on the
   device and point `WAKE_WORD_MODEL` in `.env` at its path instead of
   a built-in model name.

This is real work (expect an hour or two, not five minutes), so start
with `hey_jarvis` to confirm the whole pipeline works end to end, then
swap in a custom word once you've trained one.

## Setup

### 1. On the Raspberry Pi (or your chosen device)

```bash
sudo apt update
sudo apt install -y python3-venv python3-pip portaudio19-dev ffmpeg

git clone https://github.com/joshuatxtcllc/framecraft-voice-assistant.git
cd framecraft-voice-assistant/listener

python3 -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
```

### 2. Configure

```bash
cp .env.example .env
```

Edit `.env`:
- `BACKEND_URL` → your Railway server URL (from `/server` setup)
- `ASSISTANT_API_KEY` → same value as the iOS app's `Config.swift`
- `OPENAI_API_KEY` → an OpenAI API key (used for Whisper transcription + TTS)
- Leave `WAKE_WORD_MODEL=hey_jarvis` for now

### 3. Find your audio devices (if the defaults don't work)

```bash
python -m sounddevice
```

Note the index of your USB mic/speaker and set `INPUT_DEVICE` /
`OUTPUT_DEVICE` in `.env` if the system default isn't picking them up.

### 4. Run it once by hand to test

```bash
cd src
python main.py
```

Say "Hey Jarvis" (or whatever `WAKE_WORD_MODEL` is set to), wait for
the terminal to print "Wake word detected...", then ask something.

### 5. Make it always-on (systemd)

Edit the paths in `framecraft-listener.service` to match your actual
clone location and username, then:

```bash
sudo cp framecraft-listener.service /etc/systemd/system/
sudo systemctl daemon-reload
sudo systemctl enable --now framecraft-listener
sudo systemctl status framecraft-listener   # confirm it's running
journalctl -u framecraft-listener -f        # watch live logs
```

Now it starts on boot and restarts itself if it ever crashes.

## Known limitations (real ones)

- **Network dependency.** Transcription, the assistant call, and TTS
  all need internet. If the shop's connection drops, this device goes
  quiet until it's back — there's no offline fallback here.
- **One phrase at a time.** It doesn't handle interruptions or
  follow-up questions in the same breath; say the wake word again for
  each new request.
- **Simple silence detection**, not a smart VAD model — `record.py`'s
  `SILENCE_RMS_THRESHOLD` may need tuning for a noisy shop floor (cutting
  tools, customers talking). Turn it up if it cuts you off mid-sentence,
  down if it waits too long after you finish.
- **Same real-action caveat as the iOS app**: this can send emails and
  touch real orders through your FrameKraft tools. Treat `.env`'s
  `ASSISTANT_API_KEY` like a password — it's the same value the backend
  already trusts for the iOS app too.
