package com.mahdi.voiceassistant

import android.content.Context
import android.os.Handler
import android.os.Looper
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object ClaudeApiHelper {

    private const val PREFS_NAME = "voice_assistant_prefs"
    private const val KEY_API_KEY = "anthropic_api_key"

    fun saveApiKey(context: Context, apiKey: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_API_KEY, apiKey).apply()
    }

    fun getApiKey(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_API_KEY, "") ?: ""
    }

    /**
     * سوال رو به Claude API می‌فرسته و جواب رو تو thread پس‌زمینه می‌گیره،
     * بعد نتیجه رو تو Main Thread برمی‌گردونه (برای این‌که بشه مستقیم UI رو آپدیت کرد).
     */
    fun ask(context: Context, question: String, onResult: (String) -> Unit) {
        val apiKey = getApiKey(context)
        if (apiKey.isBlank()) {
            onResult("برای جواب دادن به سوال‌های عمومی، اول باید کلید API رو تو تنظیمات (دکمه‌ی چرخ‌دنده بالای صفحه) وارد کنی")
            return
        }

        Thread {
            try {
                val url = URL("https://api.anthropic.com/v1/messages")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.setRequestProperty("x-api-key", apiKey)
                connection.setRequestProperty("anthropic-version", "2023-06-01")
                connection.doOutput = true
                connection.connectTimeout = 15000
                connection.readTimeout = 20000

                val messages = JSONArray()
                val userMsg = JSONObject()
                userMsg.put("role", "user")
                userMsg.put(
                    "content",
                    "به فارسی و خیلی کوتاه (حداکثر ۲-۳ جمله) جواب بده چون قراره با صدا خونده بشه: $question"
                )
                messages.put(userMsg)

                val body = JSONObject()
                body.put("model", "claude-sonnet-4-6")
                body.put("max_tokens", 300)
                body.put("messages", messages)

                connection.outputStream.use { os ->
                    os.write(body.toString().toByteArray(Charsets.UTF_8))
                }

                val responseCode = connection.responseCode
                val stream = if (responseCode in 200..299) connection.inputStream else connection.errorStream
                val responseText = stream?.bufferedReader()?.use { it.readText() } ?: ""

                val resultText = if (responseCode in 200..299) {
                    val json = JSONObject(responseText)
                    val content = json.getJSONArray("content")
                    val sb = StringBuilder()
                    for (i in 0 until content.length()) {
                        val block = content.getJSONObject(i)
                        if (block.optString("type") == "text") {
                            sb.append(block.optString("text"))
                        }
                    }
                    sb.toString().ifBlank { "جوابی دریافت نشد" }
                } else {
                    "خطا از سرور هوش مصنوعی (کد $responseCode). کلید API رو چک کن."
                }

                Handler(Looper.getMainLooper()).post {
                    onResult(resultText)
                }
            } catch (e: Exception) {
                Handler(Looper.getMainLooper()).post {
                    onResult("خطا در اتصال به اینترنت: ${e.message}")
                }
            }
        }.start()
    }
}
