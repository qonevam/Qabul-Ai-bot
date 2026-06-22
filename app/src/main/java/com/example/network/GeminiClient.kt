package com.example.network

import android.util.Log
import com.example.BuildConfig
import com.example.data.ChatMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object GeminiClient {
    private const val TAG = "GeminiClient"
    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    suspend fun generateResponse(
        systemInstruction: String,
        history: List<ChatMessage>
    ): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext "DIQQAT: Gemini API kaliti kiritilmagan. Iltimos, AI Studio Secrets panelidan GEMINI_API_KEY kalitini kiriting va dasturni qayta ishga tushiring."
        }

        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"
        
        try {
            val root = JSONObject()
            
            // Build contents history
            val contentsArray = JSONArray()
            history.forEach { msg ->
                val role = if (msg.sender == "user") "user" else "model"
                val contentObj = JSONObject()
                contentObj.put("role", role)
                
                val partsArray = JSONArray()
                val partObj = JSONObject()
                partObj.put("text", msg.message)
                partsArray.put(partObj)
                
                contentObj.put("parts", partsArray)
                contentsArray.put(contentObj)
            }
            root.put("contents", contentsArray)

            // Build system instruction
            if (systemInstruction.isNotEmpty()) {
                val sysInstrObj = JSONObject()
                val sysPartsArray = JSONArray()
                val sysPartObj = JSONObject()
                sysPartObj.put("text", systemInstruction)
                sysPartsArray.put(sysPartObj)
                sysInstrObj.put("parts", sysPartsArray)
                root.put("systemInstruction", sysInstrObj)
            }

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val requestBody = root.toString().toRequestBody(mediaType)
            
            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val errBody = response.body?.string() ?: ""
                    Log.e(TAG, "Gemini API error response: $errBody")
                    return@withContext "Google Gemini API xatoligi: ${response.code}\n$errBody"
                }

                val resBodyBytes = response.body?.string() ?: return@withContext "Serverdan bo'sh javob keldi"
                val jsonRes = JSONObject(resBodyBytes)
                val candidates = jsonRes.optJSONArray("candidates")
                if (candidates != null && candidates.length() > 0) {
                    val firstCandidate = candidates.getJSONObject(0)
                    val contentObj = firstCandidate.optJSONObject("content")
                    if (contentObj != null) {
                        val parts = contentObj.optJSONArray("parts")
                        if (parts != null && parts.length() > 0) {
                            return@withContext parts.getJSONObject(0).optString("text", "Javob bo'sh")
                        }
                    }
                }
                return@withContext "Gemini API kutilgan formatda javob qaytarmadi."
            }
        } catch (e: Exception) {
            Log.e(TAG, "Connection failed", e)
            return@withContext "Sizda internet aloqasi yoki tarmoq xatosi bordek ko'rinadi: ${e.localizedMessage ?: e.message}"
        }
    }
}
