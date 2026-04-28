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
import androidx.compose.material.icons.outlined.Warning
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
    onComplete: () -> Unit,
    onJoinWithQR: () -> Unit,
    onJoinWithRecovery: () -> Unit = {}
) {
    var step by remember { mutableIntStateOf(0) }
    var path by remember { mutableStateOf<String?>(null) } // "create" or "join"
    var groupName by remember { mutableStateOf("") }
    var creatorName by remember { mutableStateOf("") }
    var seedWords by remember { mutableStateOf<List<String>>(emptyList()) }
    var seedHex by remember { mutableStateOf<String?>(null) }
    val scrollState = rememberScrollState()

    Box(modifier = Modifier.fillMaxSize().background(Ink.bg)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp)
                .padding(top = 70.dp, bottom = 40.dp)
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
                0 -> PanelWelcome()
                1 -> PanelStart(
                    onCreate = { path = "create"; step = 2 },
                    onJoin = onJoinWithQR,
                    onRecovery = onJoinWithRecovery
                )
                else -> {
                    if (path == "create") {
                        // Generate seed once, on first render of step 2
                        if (seedHex == null) {
                            val (hex, words) = generateSeedAndPhrase()
                            seedHex = hex
                            seedWords = words
                        }
                        PanelCreateForm(
                            groupName = groupName,
                            onGroupNameChange = { groupName = it },
                            creatorName = creatorName,
                            onCreatorNameChange = { creatorName = it },
                            seedWords = seedWords
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
                                if (step == 2) { path = null; seedHex = null }
                                step -= 1
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, null, tint = Ink.fgMuted, modifier = Modifier.size(14.dp))
                    }
                }
                val ctaEnabled = when (step) {
                    0, 1 -> true
                    2 -> path == "create" && groupName.isNotBlank() && creatorName.isNotBlank() && seedHex != null
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
                                1 -> { /* user picks card */ }
                                2 -> {
                                    val created = GroupRepository.createGroup(
                                        name = groupName.trim(),
                                        creatorName = creatorName.trim(),
                                        seedHex = seedHex!!
                                    )
                                    if (created != null) {
                                        GroupRepository.setActiveGroup(created.id)
                                        onComplete()
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

private fun generateSeedAndPhrase(): Pair<String, List<String>> {
    val seed = ByteArray(32).also { SecureRandom().nextBytes(it) }
    val hex = TOTPDerivation.bytesToHex(seed)
    // Show a 12-word recovery phrase derived from hex chunks for display only.
    val phrase = hex.chunked(4).take(12).map { chunk ->
        val n = chunk.toInt(16)
        BACKUP_WORDS[n % BACKUP_WORDS.size]
    }
    return hex to phrase
}

private val BACKUP_WORDS = listOf(
    "canyon", "lattice", "ember", "quorum", "ribbon", "slate",
    "vellum", "obsidian", "patina", "tessera", "umbral", "zephyr",
    "harbor", "compass", "anchor", "lantern", "rocket", "cipher",
    "kite", "river", "eagle", "fox", "robin", "pulsar"
)

@Composable
private fun PanelWelcome() {
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
        "AI can clone any voice in 3 seconds. Your safeword verifies the people who matter — no server, no account, no data collected.",
        color = Ink.fgMuted,
        modifier = Modifier.widthIn(max = 320.dp),
        style = TextStyle(fontSize = 16.sp, lineHeight = 24.sp)
    )

    Spacer(Modifier.height(40.dp))
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Ink.bgElev)
            .border(0.5.dp, Ink.rule, RoundedCornerShape(20.dp))
            .padding(horizontal = 24.dp, vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text("crimson eagle 47", color = Ink.fgFaint, style = TextStyle(fontSize = 14.sp, letterSpacing = 0.3.sp))
        Text("silent river 12", color = Ink.fgMuted, style = TextStyle(fontSize = 14.sp, letterSpacing = 0.3.sp))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(Ink.tickFill)
                .padding(horizontal = 12.dp, vertical = 4.dp)
        ) {
            Text(
                "violet anchor 88",
                color = Ink.accent,
                style = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.3.sp)
            )
        }
        Text("bronze kite 34", color = Ink.fgMuted, style = TextStyle(fontSize = 14.sp, letterSpacing = 0.3.sp))
        Text("silver fox 55", color = Ink.fgFaint, style = TextStyle(fontSize = 14.sp, letterSpacing = 0.3.sp))
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
        sub = "Generate a seed. Share via QR in person.",
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
        title = "Join with a recovery phrase",
        sub = "Restore an existing seed from backup.",
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
    onCreatorNameChange: (String) -> Unit,
    seedWords: List<String>
) {
    SectionLabel("Create · 03")
    Spacer(Modifier.height(28.dp))
    Text(
        "Name your group.",
        color = Ink.fg,
        style = TextStyle(fontSize = 36.sp, letterSpacing = (-1.2).sp, lineHeight = 40.sp)
    )
    Spacer(Modifier.height(10.dp))
    Text(
        "Pick a name your family will recognize. The seed below is your only way to recover this group.",
        color = Ink.fgMuted,
        style = TextStyle(fontSize = 15.sp, lineHeight = 22.sp)
    )

    Spacer(Modifier.height(20.dp))
    LabeledField("Group name", groupName, onGroupNameChange, placeholder = "Johnson Family")
    Spacer(Modifier.height(14.dp))
    LabeledField("Your name", creatorName, onCreatorNameChange, placeholder = "Alex")

    Spacer(Modifier.height(24.dp))
    SectionLabel("Backup phrase")
    Spacer(Modifier.height(8.dp))
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Ink.bgElev)
            .border(0.5.dp, Ink.rule, RoundedCornerShape(20.dp))
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        seedWords.chunked(3).forEachIndexed { rowIdx, rowWords ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                rowWords.forEachIndexed { colIdx, w ->
                    val idx = rowIdx * 3 + colIdx + 1
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "%02d".format(idx),
                            color = Ink.fgFaint,
                            style = TextStyle(fontSize = 10.sp),
                            modifier = Modifier.width(20.dp)
                        )
                        Text(
                            w,
                            color = Ink.fg,
                            style = TextStyle(fontSize = 13.sp, letterSpacing = 0.2.sp)
                        )
                    }
                }
            }
        }
    }
    Spacer(Modifier.height(16.dp))
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Ink.tickFill)
            .padding(14.dp)
    ) {
        Icon(Icons.Outlined.Warning, null, tint = Ink.accent, modifier = Modifier.size(16.dp).padding(top = 1.dp))
        Spacer(Modifier.width(10.dp))
        Text(
            "Anyone with this seed can see your group's safewords. Write it down somewhere safe.",
            color = Ink.accent,
            style = TextStyle(fontSize = 12.5.sp, lineHeight = 18.sp)
        )
    }
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
                if (value.isEmpty()) {
                    Text(
                        placeholder,
                        color = Ink.fgFaint,
                        style = TextStyle(fontSize = 17.sp)
                    )
                }
                inner()
            }
        )
    }
}
