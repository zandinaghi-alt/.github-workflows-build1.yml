package com.mahdi.voiceassistant

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log

/**
 * این receiver با رسیدن پیامک جدید صدا زده میشه.
 * فعلاً فقط لاگ می‌کنه؛ می‌تونیم بعداً بهش اضافه کنیم که
 * خودش با TTS اعلام کنه "پیامک جدید از فلانی رسید".
 */
class SmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
            for (msg in messages) {
                val from = msg.originatingAddress ?: "ناشناس"
                val body = msg.messageBody ?: ""
                Log.d("SmsReceiver", "پیامک جدید از $from: $body")
                // TODO: در صورت تمایل اینجا سرویس TTS رو صدا بزن تا با صدا اعلام کنه
            }
        }
    }
}
