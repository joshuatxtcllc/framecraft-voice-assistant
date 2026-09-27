# FrameCraft Voice Assistant — Android (Path B, on your old phone)

A native Android app that turns an old phone into the always-on counter
device: real wake-word detection, screen off, no unlocking, no tapping.
This is the more robust alternative to the Raspberry Pi version in
`/listener` — same idea, different (recycled) hardware.

## Why native instead of reusing the Python `/listener` code

Android lets an app keep listening with the screen off *if* it runs as a
proper **foreground service** (a persistent notification, the OS's
sanctioned way to do exactly this) — which is real engineering Android
supports, unlike iOS. A Python/Termux port of `/listener` would fight
Android's process-killing more than it needed to; a real Kotlin app
using Android's own APIs is the reliable way to do this.

## What it does

1. **Wake-word detection** — [Porcupine](https://picovoice.ai/platform/porcupine/)
   runs on-device inside a foreground service, always listening. Ships
   with `Jarvis` as the default (a real, working built-in keyword — see
   below for a custom word).
2. On wake, hands off to **Android's built-in `SpeechRecognizer`**
   (the same engine behind Google Assistant) to capture and transcribe
   your request — it has its own silence detection, so no manual VAD
   code is needed here (unlike the Python listener).
3. Sends the transcript to the **same Railway backend** (`/server`) the
   iOS app and Pi listener use — one Claude + FrameKraft-tools brain,
   three front ends, nothing duplicated.
4. Speaks the reply back with Android's built-in **TextToSpeech**.
5. Resumes wake-word listening.

No OpenAI key needed here — Android's own speech APIs handle STT/TTS
for free, same rationale as the iOS app.

## About the wake word

Porcupine ships several real built-in keywords for free: `Jarvis`,
`Computer`, `Alexa`, `Hey Google`, `Hey Siri`, `Ok Google`, `Picovoice`,
`Porcupine`, `Terminator`, `Americano`, `Blueberry`, `Bumblebee`,
`Grapefruit`, `Grasshopper`. The app defaults to **`Jarvis`** — a real,
working detector, not a placeholder.

There's no built-in "FrameCraft" keyword. To train your own:

1. Go to [console.picovoice.ai](https://console.picovoice.ai), sign up
   (free tier), and use their Porcupine keyword trainer — pick your
   phrase, it trains a `.ppn` model file in a couple of minutes.
2. Drop the downloaded `.ppn` file into `app/src/main/assets/`.
3. In `WakeWordService.kt`, change the `PorcupineManager.Builder()` call
   from `.setKeyword(Porcupine.BuiltInKeyword.JARVIS)` to
   `.setKeywordPath("your_file.ppn")`.

This takes a few minutes (much faster than openWakeWord's training flow
for the Pi version), so it's worth doing once you've confirmed the app
works end to end with "Jarvis".

## Setup

### 1. Get a free Picovoice AccessKey

Sign up at [console.picovoice.ai](https://console.picovoice.ai) and copy
your AccessKey from the dashboard.

### 2. Open the project

Open the `android/` folder directly in **Android Studio** (Open →
select the `android` folder). It will offer to generate the Gradle
wrapper and sync automatically — let it.

### 3. Configure secrets

```bash
cp local.properties.example local.properties
```

Edit `local.properties`:
- `PICOVOICE_ACCESS_KEY` → from step 1
- `BACKEND_URL` → your Railway server URL (from `/server/README.md`)
- `ASSISTANT_API_KEY` → same value used by the iOS app and Pi listener

`local.properties` is gitignored — never commit it.

### 4. Install on the phone

1. On the old Android phone: Settings → About phone → tap "Build
   number" 7 times to enable Developer Options, then Settings →
   Developer Options → enable USB debugging.
2. Plug it into your computer, select it as the run target in Android
   Studio, hit **Run**.

### 5. Grant permissions and start listening

1. Open the app. Tap **Start listening** — it'll ask for microphone and
   notification permissions.
2. Tap **Exempt from battery optimization** — critical for a phone
   that's going to sit on a counter unattended; without this, Android
   will eventually kill the background service to save power.
3. You'll see a persistent notification ("FrameCraft Assistant —
   Listening for the wake word"). That notification is the visible sign
   the foreground service is alive; the phone's screen can now be off.
4. Say "Jarvis" (or your trained wake word), wait a beat, then ask your
   question.

### 6. Leave it plugged in

Since this is a repurposed old phone sitting on a counter, just leave it
on a charger permanently — solves battery life entirely and removes one
more variable Android might use to kill the service under low power.

## Known limitations (real ones)

- **Not literally zero-touch on first boot.** You do need to open the
  app once and tap "Start listening" after a phone reboot — Android
  doesn't auto-launch foreground services on boot without additional
  boot-receiver code, which isn't included here. If you want true
  survive-a-reboot behavior, that's a real (small) follow-up: a
  `BOOT_COMPLETED` broadcast receiver that starts the service.
- **Android's SpeechRecognizer needs network** (it's Google's cloud STT
  under the hood on most devices) — same dependency the OpenAI-based Pi
  listener has, just a different provider.
- **Same real-action caveat as the other two front ends**: this can
  send emails and touch real orders through your FrameKraft tools.
  `local.properties`'s `ASSISTANT_API_KEY` is the same value the backend
  already trusts elsewhere — treat it like a password.
- **Older phones vary a lot** in how aggressively they kill background
  apps (manufacturer-specific battery managers — Samsung, Xiaomi, etc.
  are notoriously aggressive beyond stock Android's battery optimization
  setting). If the notification disappears and it stops responding
  after a while, check the phone brand's own "auto-start"/"protected
  apps" battery settings, not just Android's standard one.
