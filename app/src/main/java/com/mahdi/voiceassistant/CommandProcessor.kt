package com.mahdi.voiceassistant

import android.content.Context

object CommandProcessor {

    /**
     * پردازش دستور. چون بعضی مسیرها (مثل سوال از هوش مصنوعی) نیاز به اینترنت
     * و زمان دارن، این تابع از callback استفاده می‌کنه به‌جای return مستقیم.
     */
    fun process(context: Context, spokenText: String, onResult: (String) -> Unit) {
        val text = spokenText.trim()

        val readKeywords = listOf("بخون", "بخوان", "بگو چی", "چی نوشته", "چی گفته")
        val sendSmsKeywords = listOf("پیامک بده", "پیامک بفرست", "اس ام اس بده", "اس ام اس بفرست", "پیام بده", "پیام بفرست")
        val openAppKeywords = listOf("باز کن")
        val callKeywords = listOf("زنگ بزن", "تماس بگیر")
        val searchKeywords = listOf("گوگل کن", "جستجو کن", "بگرد")
        val flashlightKeywords = listOf("چراغ قوه")
        val wifiKeywords = listOf("وایفای", "وای فای", "وای‌فای")
        val soundSettingsKeywords = listOf("تنظیمات صدا")
        val volumeUpKeywords = listOf("صدا رو زیاد کن", "صدا رو ببر بالا")
        val volumeDownKeywords = listOf("صدا رو کم کن", "صدا رو ببر پایین")

        when {
            flashlightKeywords.any { text.contains(it) } -> {
                val ok = DeviceSettingsHelper.toggleFlashlight(context)
                onResult(if (ok) "چراغ قوه رو عوض کردم" else "نتونستم چراغ قوه رو کنترل کنم")
            }

            volumeUpKeywords.any { text.contains(it) } -> {
                DeviceSettingsHelper.adjustVolume(context, increase = true)
                onResult("صدا رو زیاد کردم")
            }

            volumeDownKeywords.any { text.contains(it) } -> {
                DeviceSettingsHelper.adjustVolume(context, increase = false)
                onResult("صدا رو کم کردم")
            }

            wifiKeywords.any { text.contains(it) } -> {
                DeviceSettingsHelper.openWifiSettings(context)
                onResult("تنظیمات وای‌فای رو باز کردم")
            }

            soundSettingsKeywords.any { text.contains(it) } -> {
                DeviceSettingsHelper.openSoundSettings(context)
                onResult("تنظیمات صدا رو باز کردم")
            }

            text.contains("پیامک") && readKeywords.any { text.contains(it) } -> {
                onResult(readLatestSms(context))
            }

            sendSmsKeywords.any { text.contains(it) } -> {
                onResult(handleSendSms(context, text))
            }

            callKeywords.any { text.contains(it) } -> {
                onResult(handleCall(context, text))
            }

            searchKeywords.any { text.contains(it) } -> {
                onResult(handleSearch(context, text))
            }

            openAppKeywords.any { text.contains(it) } -> {
                onResult(handleOpenApp(context, text))
            }

            else -> {
                // هیچ دستور از پیش تعریف‌شده‌ای مچ نشد؛ سوال رو می‌فرستیم به هوش مصنوعی
                ClaudeApiHelper.ask(context, text) { aiResponse ->
                    onResult(aiResponse)
                }
            }
        }
    }

    private fun readLatestSms(context: Context): String {
        val messages = SmsHelper.getLatestMessages(context, limit = 3)
        if (messages.isEmpty()) return "پیامک جدیدی نداری"
        val sb = StringBuilder()
        messages.forEachIndexed { i, msg ->
            sb.append("پیامک ${i + 1} از ${msg.address}: ${msg.body}. ")
        }
        return sb.toString()
    }

    private fun handleSendSms(context: Context, text: String): String {
        val simpleRegex = Regex("به (.+?) بگو (.+)")
        val nameRegex = Regex("(به|برای) (.+?) (پیامک|پیام|اس ام اس) (بده|بفرست)")
        val bodyRegex = Regex("(بنویس|بگو|بنویسم|بنویسید)\\s+(.+)")

        var target: String? = null
        var body: String? = null

        val nameMatch = nameRegex.find(text)
        if (nameMatch != null) {
            target = nameMatch.groupValues[2].trim()
            val bodyMatch = bodyRegex.find(text)
            body = bodyMatch?.groupValues?.get(2)?.trim()
        } else {
            val simpleMatch = simpleRegex.find(text)
            if (simpleMatch != null) {
                target = simpleMatch.groupValues[1].trim()
                body = simpleMatch.groupValues[2].trim()
            }
        }

        if (target.isNullOrEmpty() || body.isNullOrEmpty()) {
            return "متوجه نشدم به کی و چی بفرستم. مثلاً بگو: «به علی بگو سلام امروز میای؟»"
        }

        val phoneNumber = if (target.all { it.isDigit() || it == '+' }) {
            target
        } else {
            SmsHelper.findPhoneNumberByName(context, target)
        }

        if (phoneNumber == null) {
            return "شماره‌ای برای $target تو مخاطبینت پیدا نکردم"
        }

        SmsHelper.sendSms(phoneNumber, body)
        return "پیامک به $target فرستاده شد"
    }

    private fun handleCall(context: Context, text: String): String {
        val nameRegex = Regex("(به|با) (.+?) (زنگ بزن|تماس بگیر)")
        val match = nameRegex.find(text)
        val target = match?.groupValues?.get(2)?.trim()

        if (target.isNullOrEmpty()) {
            return "متوجه نشدم می‌خوای با کی تماس بگیرم. مثلاً بگو: «به علی زنگ بزن»"
        }

        val phoneNumber = if (target.all { it.isDigit() || it == '+' }) {
            target
        } else {
            SmsHelper.findPhoneNumberByName(context, target)
        }

        if (phoneNumber == null) {
            return "شماره‌ای برای $target تو مخاطبینت پیدا نکردم"
        }

        PhoneCallHelper.callNumber(context, phoneNumber)
        return "در حال تماس با $target"
    }

    private fun handleSearch(context: Context, text: String): String {
        val query = text
            .replace("تو گوگل بگرد", "")
            .replace("رو گوگل کن", "")
            .replace("گوگل کن", "")
            .replace("جستجو کن", "")
            .replace("بگرد", "")
            .trim()

        if (query.isEmpty()) {
            return "چی رو جستجو کنم؟"
        }

        SearchHelper.searchGoogle(context, query)
        return "دارم \"$query\" رو تو گوگل جستجو می‌کنم"
    }

    private fun handleOpenApp(context: Context, text: String): String {
        val appName = text.replace("باز کن", "").replace("رو", "").trim()
        if (appName.isEmpty()) {
            return "کدوم اپ رو باز کنم؟"
        }
        val opened = AppLauncherHelper.launchAppByName(context, appName)
        return if (opened) "$appName رو باز کردم" else "اپی به اسم $appName پیدا نکردم"
    }
}
