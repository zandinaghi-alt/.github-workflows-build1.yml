package com.mahdi.voiceassistant

import android.content.Context
import android.content.Intent
import android.net.Uri
import java.net.URLEncoder

object SearchHelper {
    fun searchGoogle(context: Context, query: String) {
        val encoded = URLEncoder.encode(query, "UTF-8")
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=$encoded")).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}
