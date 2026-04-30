package com.thc.safewords.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.thc.safewords.crypto.Bip39
import com.thc.safewords.crypto.RecoveryPhrase
import com.thc.safewords.crypto.TOTPDerivation
import com.thc.safewords.model.RotationInterval
import com.thc.safewords.service.GroupRepository
import com.thc.safewords.ui.components.SectionLabel
import com.thc.safewords.ui.theme.Ink

/**
 * Restore a group from either a 24-word BIP39 recovery phrase (preferred) or
 * a 64-character hex seed (legacy backup format).
 */
@Composable
fun RecoveryPhraseScreen(
    onBack: () -> Unit,
    onJoined: (groupId: String) -> Unit
) {
    var phrase by remember { mutableStateOf("") }
    var groupName by remember { mutableStateOf("") }
    var memberName by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    val scroll = rememberScrollState()

    val parseResult = remember(phrase) { parseSeedOrPhrase(phrase) }
    val parsedHex = (parseResult as? ParseResult.Ok)?.hex
    val parseError = (parseResult as? ParseResult.Err)?.message
    val canSubmit = parsedHex != null && groupName.isNotBlank() && memberName.isNotBlank()

    Box(modifier = Modifier.fillMaxSize().background(Ink.bg)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scroll)
                .padding(top = 62.dp, bottom = 40.dp)
                .padding(horizontal = 28.dp)
        ) {
            // Back chip
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Ink.bgElev)
                        .border(0.5.dp, Ink.rule, CircleShape)
                        .clickable(onClick = onBack),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, null, tint = Ink.fg, modifier = Modifier.size(14.dp))
                }
                Spacer(Modifier.width(12.dp))
                SectionLabel("Restore · 04")
            }

            Spacer(Modifier.height(20.dp))
            Text(
                "Paste your seed.",
                color = Ink.fg,
                style = TextStyle(fontSize = 36.sp, letterSpacing = (-1.2).sp, lineHeight = 40.sp)
            )
            Spacer(Modifier.height(10.dp))
            Text(
                "Paste either a 24-word recovery phrase or a 64-character hex seed from your backup. The group name and your name are local labels — they don't have to match the original.",
                color = Ink.fgMuted,
                style = TextStyle(fontSize = 15.sp, lineHeight = 22.sp)
            )

            Spacer(Modifier.height(24.dp))
            LabeledField(
                "Recovery phrase or seed",
                phrase,
                { phrase = it; error = null },
                placeholder = "abandon abandon … art   OR   0102…1f20",
                monospace = true
            )
            Spacer(Modifier.height(14.dp))
            LabeledField("Group name", groupName, { groupName = it }, placeholder = "Johnson Family")
            Spacer(Modifier.height(14.dp))
            LabeledField("Your name", memberName, { memberName = it }, placeholder = "Alex")

            Spacer(Modifier.height(20.dp))
            if (error != null) {
                ErrorBanner(error!!)
            } else if (parseError != null && phrase.isNotBlank()) {
                ErrorBanner(parseError)
            }

            Spacer(Modifier.height(20.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(CircleShape)
                    .background(if (canSubmit) Ink.accent else Ink.bgInset)
                    .clickable(enabled = canSubmit) {
                        val hex = parsedHex ?: return@clickable
                        val joined = GroupRepository.joinGroup(
                            name = groupName.trim(),
                            seedHex = hex,
                            interval = RotationInterval.DAILY,
                            memberName = memberName.trim()
                        )
                        if (joined == null) {
                            error = "Couldn't save the seed. Please try again."
                        } else {
                            GroupRepository.setActiveGroup(joined.id)
                            onJoined(joined.id)
                        }
                    }
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Restore group",
                    color = if (canSubmit) Ink.accentInk else Ink.fgMuted,
                    style = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                )
            }
        }
    }
}

private sealed class ParseResult {
    data class Ok(val hex: String) : ParseResult()
    data class Err(val message: String) : ParseResult()
    data object Empty : ParseResult()
}

private fun parseSeedOrPhrase(input: String): ParseResult {
    val trimmed = input.trim()
    if (trimmed.isEmpty()) return ParseResult.Empty

    // First try a BIP39 phrase if it looks word-like (contains letters and at
    // least one space).
    if (trimmed.any { it.isLetter() } && trimmed.contains(Regex("\\s"))) {
        return try {
            val seed = RecoveryPhrase.decode(trimmed)
            ParseResult.Ok(TOTPDerivation.bytesToHex(seed))
        } catch (e: Bip39.Error) {
            ParseResult.Err(e.userMessage)
        } catch (e: Throwable) {
            ParseResult.Err("Couldn't read that recovery phrase.")
        }
    }

    // Fall back to hex seed.
    val cleaned = trimmed.lowercase().replace("\\s".toRegex(), "")
    if (cleaned.length != 64) {
        return ParseResult.Err("Recovery phrase must be exactly 24 words, or seed must be 64 hex characters.")
    }
    if (!cleaned.all { it in '0'..'9' || it in 'a'..'f' }) {
        return ParseResult.Err("Hex seed contains characters other than 0–9 and a–f.")
    }
    return ParseResult.Ok(cleaned)
}

@Composable
private fun LabeledField(
    label: String,
    value: String,
    onChange: (String) -> Unit,
    placeholder: String,
    monospace: Boolean = false
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Ink.bgElev)
            .border(0.5.dp, Ink.rule, RoundedCornerShape(14.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(
            label.uppercase(),
            color = Ink.fgMuted,
            style = TextStyle(fontSize = 10.sp, letterSpacing = 1.4.sp, fontWeight = FontWeight.Medium)
        )
        Spacer(Modifier.height(6.dp))
        BasicTextField(
            value = value,
            onValueChange = onChange,
            singleLine = !monospace,
            textStyle = TextStyle(fontSize = if (monospace) 13.sp else 17.sp, color = Ink.fg, letterSpacing = if (monospace) 0.5.sp else 0.sp),
            cursorBrush = SolidColor(Ink.accent),
            decorationBox = { inner ->
                if (value.isEmpty()) Text(placeholder, color = Ink.fgFaint, style = TextStyle(fontSize = if (monospace) 13.sp else 17.sp))
                inner()
            }
        )
    }
}

@Composable
private fun ErrorBanner(message: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Ink.tickFill)
            .padding(14.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(Icons.Outlined.Warning, null, tint = Ink.accent, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(10.dp))
        Text(message, color = Ink.accent, style = TextStyle(fontSize = 12.5.sp, lineHeight = 18.sp))
    }
}
