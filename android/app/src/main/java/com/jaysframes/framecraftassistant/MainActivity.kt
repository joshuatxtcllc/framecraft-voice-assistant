package com.jaysframes.framecraftassistant

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity

/**
 * Minimal control screen: this app has no chat UI of its own — all the
 * intelligence lives on the backend. This screen only starts/stops the
 * always-on WakeWordService and handles the permission dance Android
 * requires (mic, notifications, and — critically for a phone left
 * running unattended on a counter — an exemption from battery
 * optimization, which would otherwise kill the service to save power).
 */
class MainActivity : AppCompatActivity() {

    private lateinit var statusText: TextView

    private val requestPermissions =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results ->
            if (results.values.all { it }) {
                startListeningService()
            } else {
                Toast.makeText(this, "Microphone and notification permissions are required.", Toast.LENGTH_LONG).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        statusText = findViewById(R.id.statusText)

        findViewById<Button>(R.id.startButton).setOnClickListener { requestPermissionsAndStart() }
        findViewById<Button>(R.id.stopButton).setOnClickListener { stopListeningService() }
        findViewById<Button>(R.id.batteryButton).setOnClickListener { requestBatteryExemption() }
    }

    private fun requestPermissionsAndStart() {
        val needed = mutableListOf(Manifest.permission.RECORD_AUDIO)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            needed.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        val missing = needed.filter {
            checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isEmpty()) {
            startListeningService()
        } else {
            requestPermissions.launch(missing.toTypedArray())
        }
    }

    private fun startListeningService() {
        val intent = Intent(this, WakeWordService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
        statusText.text = "Listening — say the wake word (default: \"Jarvis\")"
    }

    private fun stopListeningService() {
        stopService(Intent(this, WakeWordService::class.java))
        statusText.text = "Not listening"
    }

    private fun requestBatteryExemption() {
        val powerManager = getSystemService(POWER_SERVICE) as PowerManager
        if (!powerManager.isIgnoringBatteryOptimizations(packageName)) {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:$packageName")
            }
            startActivity(intent)
        } else {
            Toast.makeText(this, "Already exempted from battery optimization.", Toast.LENGTH_SHORT).show()
        }
    }
}
