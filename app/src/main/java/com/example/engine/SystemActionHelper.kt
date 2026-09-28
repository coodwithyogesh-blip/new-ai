package com.example.engine

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.AlarmClock
import android.widget.Toast
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

object SystemActionHelper {

    fun dialContact(context: Context, target: String): Boolean {
        return try {
            val cleanNumber = target.replace(Regex("[^0-9+]"), "")
            val intent = Intent(Intent.ACTION_DIAL).apply {
                data = Uri.parse("tel:${if (cleanNumber.isNotEmpty()) cleanNumber else target}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            Toast.makeText(context, "Dialer unavailable: ${e.message}", Toast.LENGTH_SHORT).show()
            false
        }
    }

    fun openWhatsApp(context: Context, phone: String? = null, message: String? = null): Boolean {
        return try {
            val encodedMsg = if (message != null) URLEncoder.encode(message, StandardCharsets.UTF_8.toString()) else ""
            val url = if (!phone.isNullOrBlank()) {
                "https://api.whatsapp.com/send?phone=$phone&text=$encodedMsg"
            } else {
                "https://api.whatsapp.com/send?text=$encodedMsg"
            }
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            Toast.makeText(context, "Cannot open WhatsApp: ${e.message}", Toast.LENGTH_SHORT).show()
            false
        }
    }

    fun openYouTube(context: Context, query: String? = null): Boolean {
        return try {
            val uri = if (!query.isNullOrBlank()) {
                val encoded = URLEncoder.encode(query, StandardCharsets.UTF_8.toString())
                Uri.parse("https://www.youtube.com/results?search_query=$encoded")
            } else {
                Uri.parse("https://www.youtube.com")
            }
            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            Toast.makeText(context, "Cannot open YouTube: ${e.message}", Toast.LENGTH_SHORT).show()
            false
        }
    }

    fun openApp(context: Context, appName: String): Boolean {
        val pm = context.packageManager
        val lower = appName.lowercase()
        val targetPackage = when {
            "whatsapp" in lower -> "com.whatsapp"
            "youtube" in lower -> "com.google.android.youtube"
            "chrome" in lower -> "com.android.chrome"
            "camera" in lower -> "com.google.android.GoogleCamera"
            "settings" in lower -> "com.android.settings"
            else -> null
        }

        if (targetPackage != null) {
            val launchIntent = pm.getLaunchIntentForPackage(targetPackage)
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(launchIntent)
                return true
            }
        }

        // Generic market / search query
        return try {
            val searchIntent = Intent(Intent.ACTION_WEB_SEARCH).apply {
                putExtra("query", appName)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(searchIntent)
            true
        } catch (e: Exception) {
            false
        }
    }

    fun setReminderAlarm(context: Context, message: String, minutesFromNow: Int = 10): Boolean {
        return try {
            val intent = Intent(AlarmClock.ACTION_SET_TIMER).apply {
                putExtra(AlarmClock.EXTRA_MESSAGE, message)
                putExtra(AlarmClock.EXTRA_LENGTH, minutesFromNow * 60)
                putExtra(AlarmClock.EXTRA_SKIP_UI, false)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            false
        }
    }

    fun shareContent(context: Context, text: String, title: String = "Share Artifact") {
        try {
            val sendIntent: Intent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, text)
                type = "text/plain"
            }
            val shareIntent = Intent.createChooser(sendIntent, title).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(shareIntent)
        } catch (e: Exception) {
            Toast.makeText(context, "Unable to share: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
