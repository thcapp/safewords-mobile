package com.thc.safewords.service

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.thc.safewords.model.Group
import com.thc.safewords.model.RotationInterval

/**
 * Sends an SMS invite via the system SMS app. Uses ACTION_SENDTO with sms: URI,
 * which does NOT require the SEND_SMS permission — the user must hit Send manually.
 *
 * The invite includes the current safeword + a link, so non-app family members can
 * still verify by quoting the word back.
 */
object SmsInviteService {

    fun shareViaSms(context: Context, group: Group) {
        val word = GroupRepository.getCurrentSafeword(group.id) ?: "(no word yet)"
        val intervalLabel = intervalLabel(group.interval)
        val body = buildString {
            append("Safewords invite: ")
            append("Today's word for ")
            append(group.name)
            append(" is \"")
            append(word)
            append("\". A new word every ")
            append(intervalLabel)
            append(". If anyone calls and can't say the word, hang up. ")
            append("Get the app: https://safewords.io/app")
        }

        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("smsto:")
            putExtra("sms_body", body)
            // Keeping flags conservative — user picks recipient in their SMS app.
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(intent)
        } catch (_: Exception) {
            // No SMS app installed; fall back to share intent.
            val share = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, body)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(share, "Share invite"))
        }
    }

    private fun intervalLabel(i: RotationInterval): String = when (i) {
        RotationInterval.HOURLY -> "hour"
        RotationInterval.DAILY -> "day"
        RotationInterval.WEEKLY -> "week"
        RotationInterval.MONTHLY -> "month"
    }
}
