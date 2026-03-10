package com.thc.safewords.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.thc.safewords.ui.theme.Teal
import com.thc.safewords.ui.theme.TextSecondary
import java.util.Locale

/**
 * Displays a safeword phrase with proper capitalization.
 *
 * The phrase "breezy rocket 75" is displayed as "Breezy Rocket 75".
 */
@Composable
fun SafewordDisplay(
    phrase: String,
    modifier: Modifier = Modifier,
    isLarge: Boolean = false,
    showLabel: Boolean = false,
    label: String = "Current Safeword"
) {
    val capitalizedPhrase = capitalizeSafeword(phrase)

    Column(
        modifier = modifier.padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (showLabel) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = TextSecondary,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }

        Text(
            text = capitalizedPhrase,
            style = if (isLarge) {
                MaterialTheme.typography.displayMedium
            } else {
                MaterialTheme.typography.headlineMedium
            },
            fontWeight = FontWeight.Bold,
            color = Teal,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Capitalize each word in a safeword phrase, except the trailing number.
 * "breezy rocket 75" -> "Breezy Rocket 75"
 */
private fun capitalizeSafeword(phrase: String): String {
    return phrase.split(" ").joinToString(" ") { word ->
        if (word.all { it.isDigit() }) {
            word
        } else {
            word.replaceFirstChar { it.titlecase(Locale.ROOT) }
        }
    }
}
