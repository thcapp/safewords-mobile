package com.thc.safewords.ui.verify

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.thc.safewords.crypto.TOTPDerivation
import com.thc.safewords.service.GroupRepository
import com.thc.safewords.ui.components.SectionLabel
import com.thc.safewords.ui.theme.Ink

private enum class Phase { Ready, Listening, Match, Mismatch }

@Composable
fun VerifyScreen() {
    val groups by GroupRepository.groups.collectAsState()
    val activeId by GroupRepository.activeGroupId.collectAsState()
    val group = groups.firstOrNull { it.id == activeId } ?: groups.firstOrNull()
    var phase by remember { mutableStateOf(Phase.Ready) }
    var typed by remember { mutableStateOf("") }

    val currentWord = remember(group?.id) {
        val g = group ?: return@remember ""
        val seed = GroupRepository.getGroupSeed(g.id) ?: return@remember ""
        TOTPDerivation.deriveSafeword(seed, g.interval.seconds, System.currentTimeMillis() / 1000)
    }

    Box(modifier = Modifier.fillMaxSize().background(Ink.bg)) {
        Column(
            modifier = Modifier.fillMaxSize().padding(top = 62.dp),
        ) {
            Column(Modifier.padding(horizontal = 20.dp)) {
                SectionLabel("Verify")
                Spacer(Modifier.height(6.dp))
                Text(
                    "Are they who\nthey say they are?",
                    color = Ink.fg,
                    style = TextStyle(fontSize = 30.sp, letterSpacing = (-1).sp, lineHeight = 33.sp)
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    "Ask them for today's ${group?.name ?: "group"} word. Don't read it to them.",
                    color = Ink.fgMuted,
                    style = TextStyle(fontSize = 14.sp, lineHeight = 20.sp)
                )
            }

            Spacer(Modifier.height(30.dp))

            Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 140.dp)) {
                when (phase) {
                    Phase.Ready -> ReadyPanel(
                        typed = typed,
                        onTypedChange = { typed = it },
                        onCheck = {
                            phase = if (typed.trim().equals(currentWord, ignoreCase = true)) Phase.Match else Phase.Mismatch
                        },
                        onListen = { phase = Phase.Listening }
                    )
                    Phase.Listening -> ListeningPanel(
                        onMatch = { phase = Phase.Match },
                        onMismatch = { phase = Phase.Mismatch },
                        onCancel = { phase = Phase.Ready }
                    )
                    Phase.Match -> ResultCard(match = true) { phase = Phase.Ready; typed = "" }
                    Phase.Mismatch -> ResultCard(match = false) { phase = Phase.Ready; typed = "" }
                }
            }
        }
    }
}

@Composable
private fun ReadyPanel(
    typed: String,
    onTypedChange: (String) -> Unit,
    onCheck: () -> Unit,
    onListen: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Ink.bgElev)
            .border(0.5.dp, Ink.rule, RoundedCornerShape(20.dp))
            .padding(20.dp)
    ) {
        SectionLabel("Their answer")
        Spacer(Modifier.height(10.dp))
        BasicTextField(
            value = typed,
            onValueChange = onTypedChange,
            textStyle = TextStyle(fontSize = 22.sp, color = Ink.fg, letterSpacing = 0.3.sp),
            cursorBrush = androidx.compose.ui.graphics.SolidColor(Ink.accent),
            decorationBox = { inner ->
                if (typed.isEmpty()) {
                    Text(
                        "type what they said",
                        color = Ink.fgMuted,
                        style = TextStyle(fontSize = 22.sp, letterSpacing = 0.3.sp)
                    )
                }
                inner()
            }
        )
        Spacer(Modifier.height(14.dp))
        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Ink.rule))
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(CircleShape)
                    .background(if (typed.isEmpty()) Ink.bgInset else Ink.accent)
                    .clickable(enabled = typed.isNotEmpty(), onClick = onCheck)
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Check",
                    color = if (typed.isEmpty()) Ink.fgMuted else Ink.accentInk,
                    style = TextStyle(fontSize = 14.5.sp, fontWeight = FontWeight.SemiBold)
                )
            }
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Ink.bgInset)
                    .clickable { onListen() },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.Mic, null, tint = Ink.fg, modifier = Modifier.size(17.dp))
            }
        }
    }

    Spacer(Modifier.height(20.dp))
    SectionLabel("If they can't give it", modifier = Modifier.padding(horizontal = 4.dp))
    Spacer(Modifier.height(10.dp))
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Ink.bgElev)
            .border(0.5.dp, Ink.rule, RoundedCornerShape(20.dp))
    ) {
        TipRow(1, "Hang up immediately.", "A real person will understand.")
        Box(modifier = Modifier.fillMaxWidth().height(0.5.dp).background(Ink.rule))
        TipRow(2, "Call them back on a known number.", "Don't dial what they gave you.")
        Box(modifier = Modifier.fillMaxWidth().height(0.5.dp).background(Ink.rule))
        TipRow(3, "Try an emergency override word.", "If they know the fallback, trust cautiously.")
    }
}

@Composable
private fun TipRow(n: Int, title: String, sub: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier.size(22.dp).clip(CircleShape).background(Ink.tickFill),
            contentAlignment = Alignment.Center
        ) {
            Text("$n", color = Ink.accent, style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.SemiBold))
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(title, color = Ink.fg, style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium))
            Spacer(Modifier.height(2.dp))
            Text(sub, color = Ink.fgMuted, style = TextStyle(fontSize = 12.5.sp))
        }
    }
}

@Composable
private fun ListeningPanel(onMatch: () -> Unit, onMismatch: () -> Unit, onCancel: () -> Unit) {
    val transition = rememberInfiniteTransition(label = "pulse")
    val pulse by transition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2400, easing = LinearEasing), RepeatMode.Restart),
        label = "pulse"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(28.dp),
        modifier = Modifier.fillMaxWidth().padding(top = 20.dp)
    ) {
        Box(modifier = Modifier.size(180.dp), contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier.size(180.dp).scale(pulse).clip(CircleShape)
                    .border(1.dp, Ink.accent.copy(alpha = (0.6f * (1f - pulse + 0.3f)).coerceIn(0f, 1f)), CircleShape)
            )
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(Ink.accent),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.Mic, null, tint = Ink.accentInk, modifier = Modifier.size(32.dp))
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Listening…", color = Ink.fg, style = TextStyle(fontSize = 24.sp, letterSpacing = (-0.6).sp))
            Spacer(Modifier.height(6.dp))
            Text("Ask: \"What's our word?\"", color = Ink.fgMuted, style = TextStyle(fontSize = 13.sp))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            DemoButton("Match (demo)", onMatch)
            DemoButton("Mismatch (demo)", onMismatch)
        }
        Box(modifier = Modifier.clickable(onClick = onCancel).padding(8.dp)) {
            Text("Cancel", color = Ink.fgMuted, style = TextStyle(fontSize = 13.sp))
        }
    }
}

@Composable
private fun DemoButton(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .border(1.dp, Ink.rule, CircleShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 12.dp)
    ) {
        Text(label, color = Ink.fg, style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium))
    }
}

@Composable
private fun ResultCard(match: Boolean, onDone: () -> Unit) {
    val tone = if (match) Ink.ok else Ink.accent
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp),
        modifier = Modifier.fillMaxWidth().padding(top = 40.dp)
    ) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(if (match) Ink.ok.copy(alpha = 0.15f) else Ink.tickFill)
                .border(1.dp, tone, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                if (match) Icons.Outlined.Check else Icons.Outlined.Warning,
                null,
                tint = tone,
                modifier = Modifier.size(if (match) 50.dp else 44.dp)
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                if (match) "Verified." else "Don't trust.",
                color = tone,
                style = TextStyle(fontSize = 30.sp, letterSpacing = (-1).sp)
            )
            Spacer(Modifier.height(10.dp))
            Text(
                if (match) "They gave the correct word. This is them."
                else "They could not produce the word. Hang up and call a known number.",
                color = Ink.fgMuted,
                style = TextStyle(fontSize = 14.sp, lineHeight = 20.sp),
                textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(max = 280.dp)
            )
        }
        if (!match) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Ink.bgElev)
                    .border(0.5.dp, Ink.rule, RoundedCornerShape(14.dp))
                    .padding(14.dp)
            ) {
                Icon(Icons.Outlined.Phone, null, tint = Ink.accent, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(10.dp))
                Text(
                    "Hang up. Call them on their known number.",
                    color = Ink.fg,
                    style = TextStyle(fontSize = 13.sp)
                )
            }
        }
        Box(
            modifier = Modifier
                .clip(CircleShape)
                .border(0.5.dp, Ink.rule, CircleShape)
                .clickable(onClick = onDone)
                .padding(horizontal = 22.dp, vertical = 12.dp)
        ) {
            Text("Done", color = Ink.fgMuted, style = TextStyle(fontSize = 13.sp))
        }
    }
}
