package com.mahdi.voiceassistant

import android.content.Context
import android.content.Intent

object AppLauncherHelper {

    // نگاشت اسم فارسی به بخشی از اسم انگلیسی اپ که تو گوشی نصبه
    private val persianAliases = mapOf(
        "اینستاگرام" to "Instagram",
        "تلگرام" to "Telegram",
        "واتساپ" to "WhatsApp",
        "روبیکا" to "Rubika",
        "یوتیوب" to "YouTube",
        "کروم" to "Chrome",
        "دوربین" to "Camera",
        "گالری" to "Gallery",
        "تنظیمات" to "Settings",
        "پیامک" to "Messages",
        "بازار" to "Bazaar",
        "دیجی‌کالا" to "Digikala",
        "اسنپ" to "Snapp"
    )

    /**
     * اسم اپ رو از متن گفته‌شده پیدا می‌کنه، بعد بازش می‌کنه.
     * @return true اگه پیدا و باز شد، false اگه پیدا نشد
     */
    fun launchAppByName(context: Context, spokenName: String): Boolean {
        val pm = context.packageManager
        val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val apps = pm.queryIntentActivities(mainIntent, 0)

        // اول دنبال نام مستعار فارسی می‌گردیم
        val aliasTarget = persianAliases.entries.firstOrNull { spokenName.contains(it.key) }?.value

        val match = apps.firstOrNull { resolveInfo ->
            val label = resolveInfo.loadLabel(pm).toString()
            if (aliasTarget != null) {
                label.contains(aliasTarget, ignoreCase = true)
            } else {
                label.contains(spokenName, ignoreCase = true) || spokenName.contains(label, ignoreCase = true)
            }
        }

        if (match != null) {
            val launchIntent = pm.getLaunchIntentForPackage(match.activityInfo.packageName)
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(launchIntent)
                return true
            }
        }
        return false
    }
}
