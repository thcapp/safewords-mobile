package com.thc.safewords.ui.plain

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.ArrowForward
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.content.Intent
import android.net.Uri
import androidx.compose.ui.platform.LocalContext
import com.thc.safewords.crypto.TOTPDerivation
import com.thc.safewords.model.Group
import com.thc.safewords.service.GroupRepository
import com.thc.safewords.ui.theme.A11y
import kotlinx.coroutines.delay

// Plain / accessibility mode for elderly + children.
// Large type, WCAG AAA contrast, plain language, big hit targets.

private enum class PlainScreen { Home, Verify, Help, Onboarding }

@Composable
fun PlainRoot(onExitPlain: () -> Unit = {}, onSetupReal: () -> Unit = {}) {
    val onboarded = rememberSaveable { mutableStateOf(false) }
    if (!onboarded.value) {
        PlainOnboarding(onDone = { onboarded.value = true })
        return
    }

    var screen by rememberSaveable { mutableStateOf(PlainScreen.Home) }
    Box(modifier = Modifier.fillMaxSize().background(A11y.bg)) {
        when (screen) {
            PlainScreen.Home -> PlainHome(
                onVerify = { screen = PlainScreen.Verify },
                onExitPlain = onExitPlain,
                onSetupReal = onSetupReal,
            )
            PlainScreen.Verify -> PlainVerify(onDone = { screen = PlainScreen.Home })
            PlainScreen.Help -> PlainHelp(onExitPlain = onExitPlain)
            PlainScreen.Onboarding -> PlainOnboarding(onDone = { screen = PlainScreen.Home })
        }

        PlainTabBar(
            active = screen,
            onChange = { screen = it },
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
private fun PlainTabBar(
    active: PlainScreen,
    onChange: (PlainScreen) -> Unit,
    modifier: Modifier = Modifier
) {
    data class PlainTab(val key: PlainScreen, val label: String, val icon: ImageVector, val tag: String)
    val tabs = listOf(
        PlainTab(PlainScreen.Home,   "Word",  Icons.Outlined.Shield,        "plain-home.tab-word"),
        PlainTab(PlainScreen.Verify, "Check", Icons.Outlined.Phone,         "plain-home.tab-check"),
        PlainTab(PlainScreen.Help,   "Help",  Icons.Outlined.Notifications, "plain-home.tab-help"),
    )
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp)
            .padding(bottom = 34.dp)
            .clip(RoundedCornerShape(26.dp))
            .background(A11y.bgElev)
            .border(2.dp, A11y.rule, RoundedCornerShape(26.dp))
            .padding(6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        tabs.forEach { tab ->
            val on = active == tab.key
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (on) A11y.accent else Color.Transparent)
                    .testTag(tab.tag)
                    .clickable { onChange(tab.key) }
                    .heightIn(min = 60.dp)
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(tab.icon, null, tint = if (on) A11y.accentInk else A11y.fg, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.height(4.dp))
                    Text(
                        tab.label,
                        color = if (on) A11y.accentInk else A11y.fg,
                        style = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.1.sp)
                    )
                }
            }
        }
    }
}

@Composable
private fun BigButton(
    label: String,
    onClick: () -> Unit,
    primary: Boolean = true,
    icon: ImageVector? = null,
    testTagId: String? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 72.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(if (primary) A11y.accent else A11y.bgElev)
            .border(
                2.dp,
                if (primary) Color.Transparent else A11y.rule,
                RoundedCornerShape(18.dp)
            )
            .then(if (testTagId != null) Modifier.testTag(testTagId) else Modifier)
            .clickable(onClick = onClick)
            .padding(horizontal = 22.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(icon, null, tint = if (primary) A11y.accentInk else A11y.fg, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(14.dp))
        }
        Text(
            label,
            color = if (primary) A11y.accentInk else A11y.fg,
            style = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.2).sp)
        )
    }
}

@Composable
private fun PlainHome(onVerify: () -> Unit, onExitPlain: () -> Unit, onSetupReal: () -> Unit = {}) {
    val groups by GroupRepository.groups.collectAsState()
    val activeId by GroupRepository.activeGroupId.collectAsState()
    val demoMode by GroupRepository.demoMode.collectAsState()
    val g = groups.firstOrNull { it.id == activeId } ?: groups.firstOrNull()
    var phrase by remember { mutableStateOf("") }
    var remaining by remember { mutableLongStateOf(0L) }

    LaunchedEffect(g) {
        val group = g ?: return@LaunchedEffect
        val intervalSeconds = group.primitivesOrDefault().rotatingWord.intervalSeconds
        while (true) {
            phrase = GroupRepository.getCurrentSafeword(group.id) ?: ""
            remaining = TOTPDerivation.getTimeRemaining(intervalSeconds)
            delay(1000L)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 18.dp)
            .padding(top = 62.dp, bottom = 120.dp)
    ) {
        if (demoMode) {
            DemoBanner(onSetupReal = onSetupReal)
            Spacer(Modifier.height(8.dp))
        }
        // Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp)
        ) {
            Box(
                modifier = Modifier.size(52.dp).clip(CircleShape).background(A11y.accent),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    g?.name?.take(1) ?: "F",
                    color = A11y.accentInk,
                    style = TextStyle(fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    "Your circle",
                    color = A11y.fgMuted,
                    style = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.2.sp)
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    g?.name ?: "Family",
                    color = A11y.fg,
                    style = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.3).sp),
                    modifier = Modifier.testTag("plain-home.group-name"),
                )
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(A11y.bgElev)
                    .border(2.dp, A11y.rule, RoundedCornerShape(14.dp))
                    .testTag("plain-home.gear-button")
                    .clickable(onClick = onExitPlain)
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Text(
                    "Standard view",
                    color = A11y.fg,
                    style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.2.sp)
                )
            }
        }

        Spacer(Modifier.height(16.dp))
        // Hero card
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(28.dp))
                .background(A11y.bgElev)
                .border(2.dp, A11y.rule, RoundedCornerShape(28.dp))
                .padding(horizontal = 22.dp, vertical = 32.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(A11y.accent))
                Spacer(Modifier.width(8.dp))
                Text(
                    "YOUR WORD TODAY",
                    color = A11y.accent,
                    style = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.3.sp)
                )
            }
            Spacer(Modifier.height(28.dp))
            if (phrase.isNotEmpty()) {
                // Numeric format renders as a single block with looser tracking
                // (digits are visually denser than words); word format keeps the
                // line-per-word layout that emphasizes each part.
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.testTag("plain-home.word-display"),
                ) {
                    if (phrase.contains(' ')) {
                        phrase.split(" ").forEach { w ->
                            Text(
                                w,
                                color = A11y.fg,
                                style = TextStyle(fontSize = 48.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-1.5).sp),
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        Text(
                            phrase,
                            color = A11y.fg,
                            style = TextStyle(fontSize = 56.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 6.sp),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
            Spacer(Modifier.height(28.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(A11y.bgInset)
                    .testTag("plain-home.countdown")
                    .padding(horizontal = 22.dp, vertical = 16.dp)
            ) {
                Icon(Icons.Outlined.Refresh, null, tint = A11y.accent, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(10.dp))
                Text(
                    "New word in ${humanTime(remaining)}",
                    color = A11y.fg,
                    style = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold)
                )
            }
            Spacer(Modifier.height(20.dp))
            Text(
                "Share this word only with your family.\nA new one comes tomorrow.",
                color = A11y.fgMuted,
                style = TextStyle(fontSize = 16.sp, lineHeight = 24.sp),
                textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(max = 300.dp)
            )
        }

        Spacer(Modifier.height(14.dp))
        BigButton(
            label = "Someone is calling me",
            onClick = onVerify,
            icon = Icons.Outlined.Phone
        )
    }
}

private fun humanTime(seconds: Long): String {
    val h = (seconds / 3600).toInt()
    val m = ((seconds % 3600) / 60).toInt()
    return if (h > 0) "$h hour${if (h == 1) "" else "s"} left"
    else "$m minute${if (m == 1) "" else "s"} left"
}

@Composable
private fun PlainVerify(onDone: () -> Unit) {
    var phase by remember { mutableStateOf("ask") }

    when (phase) {
        "match" -> PlainResult(
            safe = true,
            title = "Safe to talk.",
            body = "They said the right word. This is really them.",
            primaryLabel = "All done",
            onPrimary = { phase = "ask"; onDone() },
            resultTagId = "plain-verify.result-safe",
        )
        "nomatch" -> PlainResult(
            safe = false,
            title = "Hang up now.",
            body = "They did not know the word. This is not your family. Do not send money. Do not share anything.",
            primaryLabel = "I hung up",
            onPrimary = { phase = "ask"; onDone() },
            secondaryLabel = "Call them back on a trusted number",
            onSecondary = {},
            resultTagId = "plain-verify.result-hangup",
        )
        else -> PlainAsk(
            onMatch = { phase = "match" },
            onMismatch = { phase = "nomatch" },
            onCancel = onDone
        )
    }
}

@Composable
private fun PlainAsk(onMatch: () -> Unit, onMismatch: () -> Unit, onCancel: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 18.dp)
            .padding(top = 62.dp, bottom = 120.dp)
    ) {
        Text(
            "STEP 1 OF 2",
            color = A11y.accent,
            style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.3.sp),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)
        )

        val headline = buildAnnotatedString {
            append("Ask them:\n")
            withStyle(SpanStyle(color = A11y.accent)) { append("\"What is our word?\"") }
        }
        Text(
            headline,
            color = A11y.fg,
            style = TextStyle(fontSize = 34.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.8).sp, lineHeight = 38.sp),
            modifier = Modifier.padding(horizontal = 8.dp).padding(top = 10.dp, bottom = 24.dp)
        )

        val tip = buildAnnotatedString {
            withStyle(SpanStyle(color = A11y.fg, fontWeight = FontWeight.Bold)) {
                append("Do not read the word to them.")
            }
            append(" They must say it themselves.")
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(A11y.bgElev)
                .border(2.dp, A11y.rule, RoundedCornerShape(22.dp))
                .padding(22.dp)
        ) {
            Text(tip, color = A11y.fgMuted, style = TextStyle(fontSize = 19.sp, lineHeight = 27.sp))
        }

        Spacer(Modifier.weight(1f))

        Text(
            "Did they say the right word?",
            color = A11y.fg,
            style = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.ExtraBold),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 14.dp)
        )

        AnswerButton(
            label = "Yes, it matched",
            bg = A11y.ok, fg = Color(0xFF052E14),
            icon = Icons.Outlined.Check, onClick = onMatch,
            testTagId = "plain-verify.match-yes",
        )
        Spacer(Modifier.height(14.dp))
        AnswerButton(
            label = "No, wrong word",
            bg = A11y.danger, fg = Color(0xFF3A0A0A),
            icon = Icons.Outlined.Close, onClick = onMismatch,
            testTagId = "plain-verify.match-no",
        )

        Text(
            "Cancel",
            color = A11y.fgMuted,
            style = TextStyle(
                fontSize = 17.sp, fontWeight = FontWeight.SemiBold,
                textDecoration = TextDecoration.Underline
            ),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().clickable(onClick = onCancel).padding(14.dp)
        )
    }
}

@Composable
private fun AnswerButton(label: String, bg: Color, fg: Color, icon: ImageVector, onClick: () -> Unit, testTagId: String? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 80.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(bg)
            .then(if (testTagId != null) Modifier.testTag(testTagId) else Modifier)
            .clickable(onClick = onClick)
            .padding(22.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(48.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = fg, modifier = Modifier.size(28.dp))
        }
        Spacer(Modifier.width(14.dp))
        Text(label, color = fg, style = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.ExtraBold))
    }
}

@Composable
private fun PlainResult(
    safe: Boolean,
    title: String,
    body: String,
    primaryLabel: String,
    onPrimary: () -> Unit,
    secondaryLabel: String? = null,
    onSecondary: () -> Unit = {},
    resultTagId: String? = null,
) {
    val tone = if (safe) A11y.ok else A11y.danger
    val iconColor = if (safe) Color(0xFF052E14) else Color(0xFF3A0A0A)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 18.dp)
            .padding(top = 62.dp, bottom = 120.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(28.dp))
                .background(if (safe) A11y.ok.copy(alpha = 0.15f) else A11y.danger.copy(alpha = 0.15f))
                .border(2.dp, tone, RoundedCornerShape(28.dp))
                .then(if (resultTagId != null) Modifier.testTag(resultTagId) else Modifier)
                .padding(horizontal = 22.dp, vertical = 40.dp)
        ) {
            Box(
                modifier = Modifier.size(120.dp).clip(CircleShape).background(tone),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (safe) Icons.Outlined.Check else Icons.Outlined.Close,
                    null,
                    tint = iconColor,
                    modifier = Modifier.size(60.dp)
                )
            }
            Spacer(Modifier.height(28.dp))
            Text(
                title,
                color = tone,
                style = TextStyle(fontSize = 44.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-1).sp),
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(20.dp))
            Text(
                body,
                color = A11y.fg,
                style = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Medium, lineHeight = 29.sp),
                textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(max = 320.dp)
            )
        }
        Spacer(Modifier.height(16.dp))
        BigButton(label = primaryLabel, onClick = onPrimary, testTagId = "plain-verify.done")
        if (secondaryLabel != null) {
            Spacer(Modifier.height(10.dp))
            BigButton(label = secondaryLabel, onClick = onSecondary, primary = false, icon = Icons.Outlined.Phone)
        }
    }
}

@Composable
private fun PlainHelp(onExitPlain: () -> Unit = {}) {
    data class HelpItem(
        val icon: ImageVector,
        val label: String,
        val sub: String,
        val tagId: String? = null,
        val onClick: () -> Unit
    )

    val ctx = LocalContext.current
    val items = listOf(
        HelpItem(
            Icons.Outlined.Phone, "I got a strange call", "What to do right now",
            onClick = {
                // Open the dialer with 911 prefilled — user must hit call.
                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:911"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                runCatching { ctx.startActivity(intent) }
            }
        ),
        HelpItem(
            Icons.Outlined.TextFields, "Change text size", "Make everything bigger",
            tagId = "plain-help.text-size",
            onClick = {
                val intent = Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                runCatching { ctx.startActivity(intent) }
            }
        ),
        HelpItem(
            Icons.Outlined.Settings, "Turn off high visibility", "Use the regular look",
            tagId = "plain-help.exit",
            onClick = onExitPlain
        )
    )
    val scroll = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scroll)
            .padding(horizontal = 18.dp)
            .padding(top = 62.dp, bottom = 120.dp)
    ) {
        Text(
            "HELP",
            color = A11y.accent,
            style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.3.sp),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)
        )
        Text(
            "How can we help?",
            color = A11y.fg,
            style = TextStyle(fontSize = 34.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.8).sp),
            modifier = Modifier.padding(horizontal = 8.dp).padding(top = 8.dp, bottom = 22.dp)
        )

        items.forEach { item ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 80.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(A11y.bgElev)
                    .border(2.dp, A11y.rule, RoundedCornerShape(18.dp))
                    .then(if (item.tagId != null) Modifier.testTag(item.tagId) else Modifier)
                    .clickable(onClick = item.onClick)
                    .padding(18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size(52.dp).clip(CircleShape).background(A11y.bgInset),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(item.icon, null, tint = A11y.accent, modifier = Modifier.size(22.dp))
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        item.label,
                        color = A11y.fg,
                        style = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.2).sp)
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(item.sub, color = A11y.fgMuted, style = TextStyle(fontSize = 15.sp))
                }
                Icon(Icons.Outlined.ArrowForward, null, tint = A11y.fgFaint, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.height(12.dp))
        }

        Spacer(Modifier.height(10.dp))
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(A11y.danger.copy(alpha = 0.12f))
                .border(2.dp, A11y.danger, RoundedCornerShape(18.dp))
                .testTag("plain-help.emergency")
                .clickable {
                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:911"))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    runCatching { ctx.startActivity(intent) }
                }
                .padding(18.dp)
        ) {
            Text(
                "EMERGENCY",
                color = A11y.danger,
                style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "If you feel unsafe, call 911.",
                color = A11y.fg,
                style = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold)
            )
        }
    }
}

@Composable
private fun PlainOnboarding(onDone: () -> Unit) {
    data class Panel(val eyebrow: String, val title: String, val body: String, val cta: String)
    val panels = listOf(
        Panel(
            "WELCOME", "One word keeps you safe.",
            "Bad people can copy any voice now. If someone calls and sounds like family, you need a way to be sure it's really them.",
            "Show me how"
        ),
        Panel(
            "HOW IT WORKS", "Your family picks a secret word.",
            "We give you a new word every day. When someone calls, ask them to say it. Only your real family will know it.",
            "Get started"
        )
    )
    var step by remember { mutableIntStateOf(0) }
    val p = panels[step]

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 18.dp)
            .padding(top = 62.dp, bottom = 56.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth().padding(bottom = 28.dp)) {
            panels.forEachIndexed { i, _ ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(if (i <= step) A11y.accent else A11y.bgElev)
                )
            }
        }

        Text(
            p.eyebrow,
            color = A11y.accent,
            style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.3.sp),
            modifier = Modifier.padding(horizontal = 4.dp)
        )
        Text(
            p.title,
            color = A11y.fg,
            style = TextStyle(fontSize = 40.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-1.2).sp, lineHeight = 44.sp),
            modifier = Modifier.padding(horizontal = 4.dp).padding(top = 8.dp)
        )
        Text(
            p.body,
            color = A11y.fgMuted,
            style = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.Medium, lineHeight = 32.sp),
            modifier = Modifier.padding(horizontal = 4.dp).padding(top = 18.dp)
        )

        Spacer(Modifier.height(30.dp))
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(A11y.bgElev)
                .border(2.dp, A11y.rule, RoundedCornerShape(22.dp))
                .padding(22.dp)
        ) {
            Text(
                "EXAMPLE WORD",
                color = A11y.fgMuted,
                style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.4.sp)
            )
            Spacer(Modifier.height(10.dp))
            Text(
                "Golden Robin",
                color = A11y.accent,
                style = TextStyle(fontSize = 38.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-1).sp)
            )
        }

        Spacer(Modifier.weight(1f))
        BigButton(label = p.cta, onClick = {
            if (step < panels.size - 1) step += 1 else onDone()
        })
        if (step > 0) {
            Spacer(Modifier.height(12.dp))
            Text(
                "Back",
                color = A11y.fgMuted,
                style = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.SemiBold),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().clickable { step -= 1 }.padding(14.dp)
            )
        }
    }
}

@Composable
private fun DemoBanner(onSetupReal: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(A11y.accent)
            .testTag("plain-home.demo-banner")
            .clickable(onClick = onSetupReal)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                "DEMO MODE",
                color = A11y.accentInk,
                style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.2.sp)
            )
            Spacer(Modifier.height(2.dp))
            Text(
                "Set up your real group →",
                color = A11y.accentInk,
                style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.2).sp)
            )
        }
    }
}
