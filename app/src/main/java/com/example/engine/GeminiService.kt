package com.example.engine

import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class GeminiService {
    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun generateContent(prompt: String, systemInstruction: String? = null): Result<String> = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext Result.failure(IllegalStateException("GEMINI_API_KEY is not configured in Secrets panel"))
        }

        try {
            val root = JSONObject()
            val contents = JSONArray()
            val userContent = JSONObject()
            val parts = JSONArray()
            val textPart = JSONObject()
            textPart.put("text", prompt)
            parts.put(textPart)
            userContent.put("parts", parts)
            contents.put(userContent)
            root.put("contents", contents)

            if (!systemInstruction.isNullOrBlank()) {
                val sysContent = JSONObject()
                val sysParts = JSONArray()
                val sysTextPart = JSONObject()
                sysTextPart.put("text", systemInstruction)
                sysParts.put(sysTextPart)
                sysContent.put("parts", sysParts)
                root.put("systemInstruction", sysContent)
            }

            val requestBody = root.toString().toRequestBody(jsonMediaType)
            val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"

            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()

            if (!response.isSuccessful || responseBody == null) {
                return@withContext Result.failure(Exception("Gemini HTTP error ${response.code}: $responseBody"))
            }

            val responseJson = JSONObject(responseBody)
            val candidates = responseJson.optJSONArray("candidates")
            if (candidates != null && candidates.length() > 0) {
                val firstCandidate = candidates.getJSONObject(0)
                val content = firstCandidate.optJSONObject("content")
                val partsArray = content?.optJSONArray("parts")
                val text = partsArray?.optJSONObject(0)?.optString("text")
                if (!text.isNullOrBlank()) {
                    return@withContext Result.success(text)
                }
            }

            Result.failure(Exception("Empty candidate returned from Gemini API"))
        } catch (e: Exception) {
            Log.e("GeminiService", "API call failed", e)
            Result.failure(e)
        }
    }
}
