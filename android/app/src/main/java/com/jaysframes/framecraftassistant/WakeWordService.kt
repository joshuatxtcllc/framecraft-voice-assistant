package com.jaysframes.framecraftassistant

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import androidx.core.app.NotificationCompat
import ai.picovoice.porcupine.Porcupine
import ai.picovoice.porcupine.PorcupineException
import ai.picovoice.porcupine.PorcupineManager
import ai.picovoice.porcupine.PorcupineManagerCallback
import java.util.Locale

/**
 * The whole point of Path B on Android: a foreground service that keeps
 * the microphone genuinely listening for the wake word with the screen
 * off, which iOS structurally does not allow third-party apps to do.
 *
 * Loop, for as long as this service is alive:
 *   1. Porcupine listens for the wake word (on-device, low power).
 *   2. On detection, hand off to Android's SpeechRecognizer to capture
 *      and transcribe the spoken request (built-in silence detection).
 *   3. Send the transcript to the FrameCraft backend (AssistantClient).
 *   4. Speak the reply with Android's TextToSpeech.
 *   5. Resume Porcupine.
 */
class WakeWordService : Service() {

    companion object {
        private const val TAG = "WakeWordService"
        private const val CHANNEL_ID = "framecraft_listener"
        private const val NOTIFICATION_ID = 1
    }

    private var porcupineManager: PorcupineManager? = null
    private var speechRecognizer: SpeechRecognizer? = null
    private var tts: TextToSpeech? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.US
            } else {
                Log.e(TAG, "TextToSpeech init failed (status=$status)")
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, buildNotification("Listening for the wake word"))

        if (checkSelfPermission(android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "RECORD_AUDIO not granted — stopping. Grant the permission in MainActivity first.")
            stopSelf()
            return START_NOT_STICKY
        }

        startWakeWordListening()
        return START_STICKY
    }

    override fun onDestroy() {
        porcupineManager?.delete()
        porcupineManager = null
        speechRecognizer?.destroy()
        speechRecognizer = null
        tts?.shutdown()
        tts = null
        super.onDestroy()
    }

    // --- Wake word ---------------------------------------------------

    private fun startWakeWordListening() {
        if (BuildConfig.PICOVOICE_ACCESS_KEY.isBlank()) {
            Log.e(TAG, "PICOVOICE_ACCESS_KEY not set — check local.properties.")
            updateNotification("Not configured: missing Picovoice key")
            return
        }

        try {
            val callback = PorcupineManagerCallback { _ ->
                mainHandler.post { onWakeWordDetected() }
            }
            // "jarvis" ships as a real built-in keyword — works immediately,
            // no training required. Train a custom "framecraft" keyword at
            // console.picovoice.ai later and swap it in (see README).
            porcupineManager = PorcupineManager.Builder()
                .setAccessKey(BuildConfig.PICOVOICE_ACCESS_KEY)
                .setKeyword(Porcupine.BuiltInKeyword.JARVIS)
                .build(applicationContext, callback)
            porcupineManager?.start()
            updateNotification("Listening for the wake word")
        } catch (e: PorcupineException) {
            Log.e(TAG, "Porcupine failed to start: ${e.message}", e)
            updateNotification("Wake-word engine failed to start")
        }
    }

    private fun onWakeWordDetected() {
        Log.i(TAG, "Wake word detected")
        porcupineManager?.stop()
        updateNotification("Listening for your request...")
        startSpeechRecognition()
    }

    // --- Speech-to-text ------------------------------------------------

    private fun startSpeechRecognition() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            Log.e(TAG, "SpeechRecognizer not available on this device")
            resumeWakeWordListening()
            return
        }

        speechRecognizer?.destroy()
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this).apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onResults(results: Bundle) {
                    val matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val transcript = matches?.firstOrNull().orEmpty()
                    if (transcript.isBlank()) {
                        Log.i(TAG, "Heard nothing usable")
                        resumeWakeWordListening()
                    } else {
                        Log.i(TAG, "Heard: $transcript")
                        sendToAssistant(transcript)
                    }
                }

                override fun onError(error: Int) {
                    Log.w(TAG, "SpeechRecognizer error: $error")
                    resumeWakeWordListening()
                }

                override fun onReadyForSpeech(params: Bundle?) {}
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {}
                override fun onPartialResults(partialResults: Bundle?) {}
                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.US)
            // Android's own endpointing (silence detection) stops the
            // recognizer automatically — no manual VAD needed here, unlike
            // the Python listener.
        }
        speechRecognizer?.startListening(intent)
    }

    // --- Assistant call + reply --------------------------------------

    private fun sendToAssistant(transcript: String) {
        updateNotification("Thinking...")
        AssistantClient.ask(
            transcript = transcript,
            onResult = { reply ->
                mainHandler.post { speak(reply) }
            },
            onError = { error ->
                Log.e(TAG, "Assistant error: $error")
                mainHandler.post { speak("Sorry, I couldn't reach the assistant.") }
            }
        )
    }

    private fun speak(text: String) {
        updateNotification("Listening for the wake word")
        val utteranceId = "reply-${System.currentTimeMillis()}"
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {}
            override fun onDone(utteranceId: String?) {
                mainHandler.post { resumeWakeWordListening() }
            }
            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                mainHandler.post { resumeWakeWordListening() }
            }
        })
        val params = Bundle()
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
    }

    private fun resumeWakeWordListening() {
        speechRecognizer?.destroy()
        speechRecognizer = null
        try {
            porcupineManager?.start()
            updateNotification("Listening for the wake word")
        } catch (e: PorcupineException) {
            Log.e(TAG, "Failed to resume Porcupine: ${e.message}", e)
        }
    }

    // --- Notification --------------------------------------------------

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "FrameCraft Assistant",
            NotificationManager.IMPORTANCE_LOW
        )
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun buildNotification(text: String): Notification =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("FrameCraft Assistant")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setOngoing(true)
            .build()

    private fun updateNotification(text: String) {
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, buildNotification(text))
    }
}
