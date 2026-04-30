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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.QrCode
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.thc.safewords.crypto.TOTPDerivation
import com.thc.safewords.service.GroupRepository
import com.thc.safewords.ui.components.SectionLabel
import com.thc.safewords.ui.theme.Ink
import java.security.SecureRandom

@Composable
fun OnboardingScreen(
    onComplete: (createdGroupId: String?) -> Unit,
    onJoinWithQR: () -> Unit,
    onJoinWithRecovery: () -> Unit = {}
) {
    var step by remember { mutableIntStateOf(0) }
    var path by remember { mutableStateOf<String?>(null) }
    var groupName by remember { mutableStateOf("") }
    var creatorName by remember { mutableStateOf("") }
    val scrollState = rememberScrollState()

    Box(modifier = Modifier.fillMaxSize().background(Ink.bg)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 28.dp)
                .padding(top = 24.dp, bottom = 40.dp)
                .verticalScroll(scrollState)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                for (i in 0..2) {
                    Box(
                        modifier = Modifier
                            .height(3.dp)
                            .weight(if (i == step) 2f else 1f)
                            .clip(RoundedCornerShape(2.dp))
                            .background(if (i <= step) Ink.accent else Ink.rule)
                    )
                }
            }
            Spacer(Modifier.height(32.dp))

            when (step) {
                0 -> PanelWelcome(onRestore = onJoinWithRecovery)
                1 -> PanelStart(
                    onCreate = { path = "create"; step = 2 },
                    onJoin = onJoinWithQR,
                    onRecovery = onJoinWithRecovery
                )
                else -> {
                    if (path == "create") {
                        PanelCreateForm(
                            groupName = groupName,
                            onGroupNameChange = { groupName = it },
                            creatorName = creatorName,
                            onCreatorNameChange = { creatorName = it }
                        )
                    }
                }
            }

            Spacer(Modifier.height(28.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                if (step > 0) {
                    Box(
                        modifier = Modifier
                            .width(56.dp)
                            .height(48.dp)
                            .clip(CircleShape)
                            .border(0.5.dp, Ink.rule, CircleShape)
                            .clickable {
                                if (step == 2) path = null
                                step -= 1
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, null, tint = Ink.fgMuted, modifier = Modifier.size(14.dp))
                    }
                }
                val ctaEnabled = when (step) {
                    0 -> true
                    1 -> false  // user must pick a card
                    2 -> path == "create" && groupName.isNotBlank() && creatorName.isNotBlank()
                    else -> true
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(CircleShape)
                        .background(if (ctaEnabled) Ink.accent else Ink.bgInset)
                        .clickable(enabled = ctaEnabled) {
                            when (step) {
                                0 -> step = 1
                                1 -> Unit
                                2 -> {
                                    val seedHex = TOTPDerivation.bytesToHex(
                                        ByteArray(32).also { SecureRandom().nextBytes(it) }
                                    )
                                    val created = GroupRepository.createGroup(
                                        name = groupName.trim(),
                                        creatorName = creatorName.trim(),
                                        seedHex = seedHex
                                    )
                                    if (created != null) {
                                        GroupRepository.setActiveGroup(created.id)
                                        onComplete(created.id)
                                    }
                                }
                            }
                        }
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            when (step) {
                                0 -> "Get started"
                                1 -> "Pick an option above"
                                else -> "Create group"
                            },
                            color = if (ctaEnabled) Ink.accentInk else Ink.fgMuted,
                            style = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.1).sp)
                        )
                        if (step != 1) {
                            Spacer(Modifier.width(8.dp))
                            Icon(
                                Icons.AutoMirrored.Outlined.ArrowForward,
                                null,
                                tint = if (ctaEnabled) Ink.accentInk else Ink.fgMuted,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PanelWelcome(onRestore: () -> Unit) {
    SectionLabel("Safewords · 01")
    Spacer(Modifier.height(28.dp))

    val headline = buildAnnotatedString {
        append("One word between\n")
        withStyle(SpanStyle(color = Ink.accent)) { append("trust") }
        append(" and deception.")
    }
    Text(
        headline,
        color = Ink.fg,
        style = TextStyle(fontSize = 42.sp, letterSpacing = (-1.4).sp, lineHeight = 44.sp)
    )
    Spacer(Modifier.height(20.dp))
    Text(
        "AI clones voices. Accounts get hijacked. Numbers get swapped. A pre-agreed word is the proof that survives all of it.",
        color = Ink.fgMuted,
        modifier = Modifier.widthIn(max = 340.dp),
        style = TextStyle(fontSize = 16.sp, lineHeight = 24.sp)
    )

    Spacer(Modifier.height(32.dp))

    // Dialog example — what it looks like in practice
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Ink.bgElev)
            .border(0.5.dp, Ink.rule, RoundedCornerShape(20.dp))
            .padding(horizontal = 20.dp, vertical = 22.dp)
    ) {
        DialogLine(
            role = "Caller (sounds like your son)",
            line = "\"Mom, I'm in trouble. I need you to wire money right now.\"",
            accent = false
        )
        Spacer(Modifier.height(12.dp))
        DialogLine(
            role = "You",
            line = "\"What's our word?\"",
            accent = true
        )
        Spacer(Modifier.height(12.dp))
        DialogLine(
            role = "Caller",
            line = "\"I... uh... I don't remember.\"",
            accent = false
        )
        Spacer(Modifier.height(16.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(Ink.bgInset)
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            val outcome = buildAnnotatedString {
                withStyle(SpanStyle(color = Ink.fg, fontWeight = FontWeight.SemiBold)) {
                    append("Hang up.")
                }
                append(" A real person would know.")
            }
            Text(outcome, color = Ink.fgMuted, style = TextStyle(fontSize = 13.sp, lineHeight = 19.sp))
        }
    }

    Spacer(Modifier.height(20.dp))

    // Trust pill — FBI / FTC / AARP recommendation
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Ink.bgElev.copy(alpha = 0.6f))
            .border(0.5.dp, Ink.rule, RoundedCornerShape(14.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(
            "Recommended by the FBI, FTC, and AARP for families and trust groups.",
            color = Ink.fgMuted,
            style = TextStyle(fontSize = 12.sp, lineHeight = 18.sp, letterSpacing = 0.1.sp),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }

    Spacer(Modifier.height(18.dp))
    // Restore link — for users coming back after reinstall
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onRestore)
            .padding(vertical = 8.dp)
    ) {
        Text(
            "I already have a backup — restore it",
            color = Ink.accent,
            style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun DialogLine(role: String, line: String, accent: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 14.dp)
            .border(
                width = 0.dp,
                color = Color.Transparent,
                shape = RoundedCornerShape(0.dp)
            )
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Box(
                modifier = Modifier
                    .padding(top = 4.dp, end = 12.dp)
                    .width(3.dp)
                    .height(if (accent) 44.dp else 36.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(if (accent) Ink.accent else Ink.rule)
            )
            Column(Modifier.weight(1f)) {
                Text(
                    role.uppercase(),
                    color = if (accent) Ink.accent else Ink.fgFaint,
                    style = TextStyle(fontSize = 9.5.sp, letterSpacing = 1.0.sp, fontWeight = FontWeight.SemiBold)
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    line,
                    color = if (accent) Ink.fg else Ink.fgMuted,
                    style = TextStyle(fontSize = 14.sp, lineHeight = 20.sp)
                )
            }
        }
    }
}

@Composable
private fun PanelStart(onCreate: () -> Unit, onJoin: () -> Unit, onRecovery: () -> Unit) {
    SectionLabel("Start · 02")
    Spacer(Modifier.height(28.dp))
    Text(
        "Start a group, or\njoin someone else's.",
        color = Ink.fg,
        style = TextStyle(fontSize = 36.sp, letterSpacing = (-1.2).sp, lineHeight = 40.sp)
    )
    Spacer(Modifier.height(28.dp))

    OnboardOption(
        title = "Create a new group",
        sub = "We'll generate a private key for you. No backup needed yet — share via QR.",
        icon = Icons.Outlined.Add, primary = true, onClick = onCreate
    )
    Spacer(Modifier.height(12.dp))
    OnboardOption(
        title = "Join with a QR code",
        sub = "Scan a QR shared by a group member.",
        icon = Icons.Outlined.QrCode, onClick = onJoin
    )
    Spacer(Modifier.height(12.dp))
    OnboardOption(
        title = "Restore from a backup",
        sub = "Paste your seed if you saved one earlier.",
        icon = Icons.Outlined.Refresh, onClick = onRecovery
    )

    Spacer(Modifier.height(24.dp))
    Text(
        "Everything stays on your device.\nNo accounts. No data collection.",
        color = Ink.fgFaint,
        modifier = Modifier.fillMaxWidth(),
        style = TextStyle(fontSize = 11.5.sp, lineHeight = 17.sp),
        textAlign = TextAlign.Center
    )
}

@Composable
private fun OnboardOption(
    title: String,
    sub: String,
    icon: ImageVector,
    primary: Boolean = false,
    onClick: () -> Unit
) {
    val bg = if (primary) Ink.accent else Ink.bgElev
    val fg = if (primary) Ink.accentInk else Ink.fg
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(bg)
            .border(0.5.dp, if (primary) Color.Transparent else Ink.rule, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(if (primary) Color.Black.copy(alpha = 0.12f) else Ink.bgInset),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = fg, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = fg, style = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.2).sp))
            Spacer(Modifier.height(2.dp))
            Text(sub, color = fg.copy(alpha = if (primary) 0.7f else 0.6f), style = TextStyle(fontSize = 12.5.sp, lineHeight = 17.sp))
        }
        Icon(Icons.AutoMirrored.Outlined.ArrowForward, null, tint = fg.copy(alpha = 0.5f), modifier = Modifier.size(16.dp))
    }
}

@Composable
private fun PanelCreateForm(
    groupName: String,
    onGroupNameChange: (String) -> Unit,
    creatorName: String,
    onCreatorNameChange: (String) -> Unit
) {
    SectionLabel("Create · 03")
    Spacer(Modifier.height(28.dp))
    Text(
        "Almost done.",
        color = Ink.fg,
        style = TextStyle(fontSize = 36.sp, letterSpacing = (-1.2).sp, lineHeight = 40.sp)
    )
    Spacer(Modifier.height(10.dp))
    Text(
        "Pick a name your family will recognize. We'll generate a private key for you — you can back it up later in Settings.",
        color = Ink.fgMuted,
        style = TextStyle(fontSize = 15.sp, lineHeight = 22.sp)
    )

    Spacer(Modifier.height(24.dp))
    LabeledField("Group name", groupName, onGroupNameChange, placeholder = "Johnson Family")
    Spacer(Modifier.height(14.dp))
    LabeledField("Your name", creatorName, onCreatorNameChange, placeholder = "Alex")
}

@Composable
private fun LabeledField(
    label: String,
    value: String,
    onChange: (String) -> Unit,
    placeholder: String
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
            singleLine = true,
            textStyle = TextStyle(fontSize = 17.sp, color = Ink.fg),
            cursorBrush = SolidColor(Ink.accent),
            decorationBox = { inner ->
                if (value.isEmpty()) Text(placeholder, color = Ink.fgFaint, style = TextStyle(fontSize = 17.sp))
                inner()
            }
        )
    }
}
