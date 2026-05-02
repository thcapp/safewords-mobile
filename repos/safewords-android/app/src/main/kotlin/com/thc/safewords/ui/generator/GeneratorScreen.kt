package com.thc.safewords.ui.generator

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Refresh
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.thc.safewords.data.WordGenerator
import com.thc.safewords.ui.components.SectionLabel
import com.thc.safewords.ui.theme.Ink

@Composable
fun GeneratorScreen(onBack: () -> Unit) {
    var style by remember { mutableStateOf(WordGenerator.Style.WordNumber) }
    var phrase by remember { mutableStateOf(WordGenerator.phrase(style)) }
    val ctx = LocalContext.current

    Box(modifier = Modifier.fillMaxSize().background(Ink.bg)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 44.dp, bottom = 36.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .testTag("generator.back")
                        .clickable(onClick = onBack),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.AutoMirrored.Outlined.ArrowBack, null,
                        tint = Ink.fg, modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(Modifier.width(4.dp))
                Text(
                    "Generator",
                    color = Ink.fg,
                    style = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.2).sp)
                )
            }

            Column(Modifier.padding(horizontal = 20.dp).padding(top = 14.dp)) {
                SectionLabel("Single use word")
                Spacer(Modifier.height(6.dp))
                Text(
                    "A random word",
                    color = Ink.fg,
                    style = TextStyle(fontSize = 32.sp, letterSpacing = (-1.sp))
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "Pick a one-off safeword. Not tied to any group, not rotated. Use for a single conversation or as a backup.",
                    color = Ink.fgMuted,
                    style = TextStyle(fontSize = 14.sp, lineHeight = 20.sp)
                )
            }

            Spacer(Modifier.height(22.dp))

            // Hero card
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(28.dp))
                    .background(Ink.bgElev)
                    .border(0.5.dp, Ink.rule, RoundedCornerShape(28.dp))
                    .padding(horizontal = 18.dp, vertical = 36.dp)
            ) {
                Text(
                    "YOUR WORD",
                    color = Ink.accent,
                    style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.4.sp)
                )
                Spacer(Modifier.height(20.dp))
                Text(
                    phrase,
                    color = Ink.fg,
                    style = TextStyle(fontSize = 38.sp, fontWeight = FontWeight.Bold, letterSpacing = (-1).sp, lineHeight = 44.sp),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.testTag("generator.word-display"),
                )
                Spacer(Modifier.height(28.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(22.dp))
                            .background(Ink.accent)
                            .testTag("generator.regenerate")
                            .clickable { phrase = WordGenerator.phrase(style) }
                            .padding(horizontal = 18.dp, vertical = 12.dp)
                    ) {
                        Icon(Icons.Outlined.Refresh, null, tint = Ink.accentInk, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Regenerate",
                            color = Ink.accentInk,
                            style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(22.dp))
                            .background(Color.Transparent)
                            .border(0.5.dp, Ink.rule, RoundedCornerShape(22.dp))
                            .clickable {
                                val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                cm.setPrimaryClip(ClipData.newPlainText("safeword", phrase))
                            }
                            .padding(horizontal = 18.dp, vertical = 12.dp)
                    ) {
                        Icon(Icons.Outlined.ContentCopy, null, tint = Ink.fg, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Copy",
                            color = Ink.fg,
                            style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // Style picker
            Column(Modifier.padding(horizontal = 16.dp)) {
                SectionLabel("Style")
                Spacer(Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    StylePill(
                        label = "Word + number",
                        sub = "Adj Noun NN",
                        selected = style == WordGenerator.Style.WordNumber,
                        modifier = Modifier.weight(1f)
                    ) {
                        style = WordGenerator.Style.WordNumber
                        phrase = WordGenerator.phrase(style)
                    }
                    StylePill(
                        label = "Two words",
                        sub = "Adj Noun",
                        selected = style == WordGenerator.Style.TwoWords,
                        modifier = Modifier.weight(1f)
                    ) {
                        style = WordGenerator.Style.TwoWords
                        phrase = WordGenerator.phrase(style)
                    }
                    StylePill(
                        label = "Three words",
                        sub = "Adj Noun Noun",
                        selected = style == WordGenerator.Style.ThreeWords,
                        modifier = Modifier.weight(1f)
                    ) {
                        style = WordGenerator.Style.ThreeWords
                        phrase = WordGenerator.phrase(style)
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            Column(Modifier.padding(horizontal = 20.dp)) {
                Text(
                    "Words come from a frozen list of 197 adjectives × 300 nouns. " +
                            "All generation happens on this device. Nothing leaves it.",
                    color = Ink.fgFaint,
                    style = TextStyle(fontSize = 12.sp, lineHeight = 18.sp)
                )
            }
        }
    }
}

@Composable
private fun StylePill(
    label: String,
    sub: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) Ink.accent else Ink.bgElev)
            .border(0.5.dp, if (selected) Color.Transparent else Ink.rule, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            label,
            color = if (selected) Ink.accentInk else Ink.fg,
            style = TextStyle(fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.1).sp)
        )
        Spacer(Modifier.height(2.dp))
        Text(
            sub,
            color = if (selected) Ink.accentInk.copy(alpha = 0.7f) else Ink.fgMuted,
            style = TextStyle(fontSize = 10.sp, letterSpacing = 0.4.sp)
        )
    }
}
