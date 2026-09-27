package com.jaysframes.framecraftassistant

import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Talks to the same Railway backend the iOS app and the Python /listener
 * use — one Claude + FrameKraft-tools brain, three front ends. This class
 * only sends text and gets text back; all the reasoning happens server-side.
 */
object AssistantClient {

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val JSON = "application/json; charset=utf-8".toMediaType()

    fun ask(transcript: String, onResult: (String) -> Unit, onError: (String) -> Unit) {
        val backendUrl = BuildConfig.BACKEND_URL.trimEnd('/')
        val apiKey = BuildConfig.ASSISTANT_API_KEY

        if (backendUrl.isBlank() || apiKey.isBlank()) {
            onError("BACKEND_URL / ASSISTANT_API_KEY not set — check local.properties.")
            return
        }

        val body = JSONObject().put("transcript", transcript).toString().toRequestBody(JSON)
        val request = Request.Builder()
            .url("$backendUrl/api/assistant/query")
            .addHeader("x-assistant-key", apiKey)
            .post(body)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                onError("Couldn't reach the backend: ${e.message}")
            }

            override fun onResponse(call: Call, response: okhttp3.Response) {
                response.use {
                    val text = it.body?.string().orEmpty()
                    if (!it.isSuccessful) {
                        onError("Backend returned ${it.code}: $text")
                        return
                    }
                    val reply = JSONObject(text).optString("reply", "")
                    onResult(reply)
                }
            }
        })
    }
}
