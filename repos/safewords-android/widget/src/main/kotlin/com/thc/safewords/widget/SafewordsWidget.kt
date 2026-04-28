package com.thc.safewords.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.color.ColorProvider
import com.thc.safewords.crypto.TOTPDerivation
import com.thc.safewords.service.GroupRepository
import java.util.Locale

class SafewordsWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val groups = GroupRepository.groups.value
        val firstGroup = groups.firstOrNull()

        val groupName: String
        val phrase: String
        val timeRemainingText: String

        if (firstGroup != null) {
            val seed = GroupRepository.getGroupSeed(firstGroup.id)
            if (seed != null) {
                val now = System.currentTimeMillis() / 1000
                val rawPhrase = TOTPDerivation.deriveSafeword(seed, firstGroup.interval.seconds, now)
                phrase = capitalizeSafeword(rawPhrase)
                groupName = firstGroup.name
                val remaining = TOTPDerivation.getTimeRemaining(firstGroup.interval.seconds)
                timeRemainingText = "Rotates in ${TOTPDerivation.formatTimeRemaining(remaining)}"
            } else {
                groupName = firstGroup.name
                phrase = "No seed"
                timeRemainingText = ""
            }
        } else {
            groupName = "Safewords"
            phrase = "No groups"
            timeRemainingText = "Open app to create one"
        }

        provideContent {
            WidgetContent(
                groupName = groupName,
                phrase = phrase,
                timeRemaining = timeRemainingText
            )
        }
    }

    private fun capitalizeSafeword(phrase: String): String {
        return phrase.split(" ").joinToString(" ") { word ->
            if (word.all { it.isDigit() }) {
                word
            } else {
                word.replaceFirstChar { it.titlecase(Locale.ROOT) }
            }
        }
    }
}

@Composable
private fun WidgetContent(
    groupName: String,
    phrase: String,
    timeRemaining: String
) {
    // Ink theme tokens (duplicated — widget is its own module).
    val surfaceColor = ColorProvider(
        day = androidx.compose.ui.graphics.Color(0xFF151517),
        night = androidx.compose.ui.graphics.Color(0xFF151517)
    )
    val tealColor = ColorProvider(
        day = androidx.compose.ui.graphics.Color(0xFFE8553A),
        night = androidx.compose.ui.graphics.Color(0xFFE8553A)
    )
    val textPrimary = ColorProvider(
        day = androidx.compose.ui.graphics.Color(0xFFF5F2EC),
        night = androidx.compose.ui.graphics.Color(0xFFF5F2EC)
    )
    val textMuted = ColorProvider(
        day = androidx.compose.ui.graphics.Color(0x8CF5F2EC),
        night = androidx.compose.ui.graphics.Color(0x8CF5F2EC)
    )

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(surfaceColor)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = groupName,
            style = TextStyle(
                color = textMuted,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        )

        Spacer(modifier = GlanceModifier.height(4.dp))

        Text(
            text = phrase,
            style = TextStyle(
                color = tealColor,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
        )

        Spacer(modifier = GlanceModifier.height(4.dp))

        Text(
            text = timeRemaining,
            style = TextStyle(
                color = textMuted,
                fontSize = 11.sp
            )
        )
    }
}
