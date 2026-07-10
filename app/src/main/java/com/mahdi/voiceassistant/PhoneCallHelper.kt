package com.mahdi.voiceassistant

import android.content.Context
import android.content.Intent
import android.net.Uri

object PhoneCallHelper {
    fun callNumber(context: Context, phoneNumber: String) {
        val intent = Intent(Intent.ACTION_CALL, Uri.parse("tel:$phoneNumber")).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}
