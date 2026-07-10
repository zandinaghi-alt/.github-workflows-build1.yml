package com.mahdi.voiceassistant

import android.content.Context

object CommandProcessor {

    fun process(context: Context, spokenText: String): String {
        val text = spokenText.trim()

        val readKeywords = listOf("بخون", "بخوان", "بگو چی", "چی نوشته", "چی گفته")
        val sendKeywords = listOf("پیامک بده", "پیامک بفرست", "اس ام اس بده", "اس ام اس بفرست", "پیام بده", "پیام بفرست")

        return when {
            text.contains("پیامک") && readKeywords.any { text.contains(it) } -> {
                readLatestSms(context)
            }
            sendKeywords.any { text.contains(it) } -> {
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
}
