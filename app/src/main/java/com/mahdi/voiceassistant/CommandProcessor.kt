package com.mahdi.voiceassistant

import android.content.Context

/**
 * پردازشگر ساده و مبتنی بر قانون برای دستورهای فارسی.
 * این نسخه‌ی اولیه است؛ بعداً می‌شه به‌جاش یک مدل زبانی (مثل Claude API)
 * برای تشخیص دقیق‌تر منظور جایگزین کرد.
 */
object CommandProcessor {

    // نتیجه‌ی پردازش: متنی که باید با TTS خونده بشه
    fun process(context: Context, spokenText: String): String {
        val text = spokenText.trim()

        return when {
            // "پیامک های جدید رو بخون" / "آخرین پیامک چیه"
            text.contains("پیامک") && (text.contains("بخون") || text.contains("جدید") || text.contains("آخرین")) -> {
                readLatestSms(context)
            }

            // "به علی پیامک بده بنویس سلام حالت خوبه" یا "به 0912... پیامک بده بگو ..."
            text.contains("پیامک بده") || text.contains("پیامک بفرست") -> {
                handleSendSms(context, text)
            }

            else -> "متوجه دستور نشدم. می‌تونی بگی «پیامک‌های جدید رو بخون» یا «به [اسم] پیامک بده بنویس [متن]»"
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
        // الگوی ساده: "به <نام/شماره> پیامک بده بنویس <متن>"
        val nameRegex = Regex("به (.+?) پیامک (بده|بفرست)")
        val bodyRegex = Regex("(بنویس|بگو) (.+)")

        val nameMatch = nameRegex.find(text)
        val bodyMatch = bodyRegex.find(text)

        val target = nameMatch?.groupValues?.get(1)?.trim()
        val body = bodyMatch?.groupValues?.get(2)?.trim()

        if (target == null || body.isNullOrEmpty()) {
            return "لطفاً بگو: به [اسم یا شماره] پیامک بده بنویس [متن پیام]"
        }

        // اگر شماره باشه مستقیم استفاده می‌کنیم، وگرنه از مخاطب‌ها پیدا می‌کنیم
        val phoneNumber = if (target.all { it.isDigit() || it == '+' }) {
            target
        } else {
            SmsHelper.findPhoneNumberByName(context, target)
        }

        if (phoneNumber == null) {
            return "شماره‌ای برای $target پیدا نکردم"
        }

        SmsHelper.sendSms(phoneNumber, body)
        return "پیامک به $target فرستاده شد"
    }
}
