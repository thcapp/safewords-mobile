package com.thc.safewords.ui.home

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.QrCode
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.thc.safewords.crypto.TOTPDerivation
import com.thc.safewords.model.Group
import com.thc.safewords.service.GroupRepository
import com.thc.safewords.ui.components.CountdownRing
import com.thc.safewords.ui.components.GroupDot
import com.thc.safewords.ui.components.SectionLabel
import com.thc.safewords.ui.theme.DotPalette
import com.thc.safewords.ui.theme.Ink
import kotlinx.coroutines.delay
import kotlin.math.abs

@Composable
fun HomeScreen(
    onNavigateToGroups: () -> Unit,
    onShareInvite: (groupId: String) -> Unit = {}
) {
    val groups by GroupRepository.groups.collectAsState()
    val activeId by GroupRepository.activeGroupId.collectAsState()
    val selected = groups.firstOrNull { it.id == activeId } ?: groups.firstOrNull()
    var phrase by remember { mutableStateOf("") }
    var remaining by remember { mutableLongStateOf(0L) }
    var progress by remember { mutableFloatStateOf(0f) }

    // Promote first group to active if none selected.
    LaunchedEffect(groups, activeId) {
        if (activeId == null && groups.isNotEmpty()) {
            GroupRepository.setActiveGroup(groups.first().id)
        }
    }

    LaunchedEffect(selected?.id) {
        val g = selected ?: return@LaunchedEffect
        while (true) {
            val seed = GroupRepository.getGroupSeed(g.id)
            if (seed != null) {
                val now = System.currentTimeMillis() / 1000
                phrase = TOTPDerivation.deriveSafeword(seed, g.interval.seconds, now)
                remaining = TOTPDerivation.getTimeRemaining(g.interval.seconds)
                progress = 1f - (remaining.toFloat() / g.interval.seconds.toFloat())
            }
            delay(1000L)
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Ink.bg)) {
        val g = selected ?: run { EmptyState(onNavigateToGroups); return }

        // Top bar pills
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 0.dp)
                .padding(top = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Ink.bgElev)
                    .border(0.5.dp, Ink.rule, CircleShape)
                    .testTag("home.group-pill")
                    .clickable { onNavigateToGroups() }
                    .padding(start = 7.dp, end = 12.dp)
                    .padding(vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                GroupDot(
                    initial = g.name.take(1),
                    color = DotPalette[abs(g.id.hashCode()) % DotPalette.size],
                    size = 24.dp
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    g.name,
                    color = Ink.fg,
                    style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Medium, letterSpacing = (-0.1).sp)
                )
            }
            Spacer(Modifier.weight(1f))
            // Share / invite QR
            Row(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Ink.tickFill)
                    .clickable { onShareInvite(g.id) }
                    .padding(start = 12.dp, end = 14.dp, top = 7.dp, bottom = 7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.QrCode,
                    contentDescription = "Invite",
                    tint = Ink.accent,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    "Invite",
                    color = Ink.accent,
                    style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.1).sp)
                )
            }
        }

        // Hero
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 60.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Spacer(Modifier.height(30.dp))
            CountdownRing(
                progress = progress,
                size = 340.dp,
                modifier = Modifier.testTag("home.countdown-ring"),
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(Ink.accent)
                        )
                        Spacer(Modifier.width(6.dp))
                        SectionLabel("LIVE · ${g.name.uppercase()}", color = Ink.accent)
                    }
                    Spacer(Modifier.height(14.dp))
                    if (phrase.isNotEmpty()) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.testTag("home.word-display"),
                        ) {
                            phrase.split(" ").forEach { w ->
                                Text(
                                    w,
                                    color = Ink.fg,
                                    style = TextStyle(
                                        fontSize = 46.sp,
                                        fontWeight = FontWeight.Normal,
                                        letterSpacing = (-1.5).sp
                                    ),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    Text(
                        "SEQ · ${"%04d".format(sequenceFor(g))}",
                        color = Ink.fgFaint,
                        style = TextStyle(fontSize = 11.sp, letterSpacing = 1.5.sp)
                    )
                }
            }
            Spacer(Modifier.height(28.dp))
            Text(
                formatCountdown(remaining),
                color = Ink.fg,
                style = TextStyle(fontSize = 28.sp, letterSpacing = 2.sp),
                modifier = Modifier.testTag("home.countdown-text"),
            )
            Spacer(Modifier.height(8.dp))
            val previewEnabled = remember { GroupRepository.isPreviewNextWord() }
            val nextPreview = remember(g.id, remaining) {
                if (!previewEnabled) "•••••"
                else {
                    val seed = GroupRepository.getGroupSeed(g.id)
                    if (seed == null) "•••••"
                    else {
                        val nextTs = (System.currentTimeMillis() / 1000) + remaining + 1
                        TOTPDerivation.deriveSafeword(seed, g.interval.seconds, nextTs)
                    }
                }
            }
            Text(
                "rotates in ${friendlyInterval(remaining)} · next: $nextPreview",
                color = Ink.fgMuted,
                style = TextStyle(fontSize = 12.sp, letterSpacing = 0.2.sp)
            )
        }
    }
}

@Composable
private fun EmptyState(onNavigateToGroups: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            "Safewords",
            color = Ink.accent,
            style = TextStyle(fontSize = 36.sp, letterSpacing = (-1.4).sp)
        )
        Spacer(Modifier.height(16.dp))
        Text(
            "Create a group or join one to start\nsharing rotating safewords.",
            color = Ink.fgMuted,
            style = TextStyle(fontSize = 15.sp),
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(24.dp))
        Box(
            modifier = Modifier
                .clip(CircleShape)
                .background(Ink.accent)
                .testTag("home.empty-create-cta")
                .clickable { onNavigateToGroups() }
                .padding(horizontal = 22.dp, vertical = 14.dp)
        ) {
            Text(
                "Get started",
                color = Ink.accentInk,
                style = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            )
        }
    }
}

private fun formatCountdown(seconds: Long): String {
    val h = (seconds / 3600).toInt()
    val m = ((seconds % 3600) / 60).toInt()
    val s = (seconds % 60).toInt()
    return "%02d:%02d:%02d".format(h, m, s)
}

private fun friendlyInterval(seconds: Long): String {
    val h = (seconds / 3600).toInt()
    val m = ((seconds % 3600) / 60).toInt()
    val s = (seconds % 60).toInt()
    return if (h > 0) "${h}h ${m}m" else "${m}m ${s}s"
}

private fun sequenceFor(group: Group): Int {
    val counter = (System.currentTimeMillis() / 1000) / group.interval.seconds
    return (counter % 10_000).toInt()
}
