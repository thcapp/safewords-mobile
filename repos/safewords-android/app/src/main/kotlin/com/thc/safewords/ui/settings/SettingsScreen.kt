package com.thc.safewords.ui.settings

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
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import com.thc.safewords.data.WordGenerator
import com.thc.safewords.model.Group
import com.thc.safewords.model.RotationInterval
import com.thc.safewords.service.GroupRepository
import com.thc.safewords.ui.components.SectionLabel
import com.thc.safewords.ui.theme.Ink

@Composable
fun SettingsScreen(
    plainMode: Boolean,
    onPlainModeChange: (Boolean) -> Unit,
    onRunDrill: () -> Unit,
    onDrillHistory: () -> Unit,
    onOpenGenerator: () -> Unit = {}
) {
    val groups by GroupRepository.groups.collectAsState()
    val activeId by GroupRepository.activeGroupId.collectAsState()
    val active = groups.firstOrNull { it.id == activeId } ?: groups.firstOrNull()

    var notify by remember { mutableStateOf(GroupRepository.isNotifyOnRotation()) }
    var preview by remember { mutableStateOf(GroupRepository.isPreviewNextWord()) }
    var lockGlance by remember { mutableStateOf(GroupRepository.isLockScreenGlance()) }
    var hideUntilUnlock by remember { mutableStateOf(GroupRepository.isHideUntilUnlock()) }
    var biometricRequired by remember { mutableStateOf(GroupRepository.isBiometricRequired()) }

    var showOverrideSheet by remember { mutableStateOf(false) }
    var showLeaveConfirm by remember { mutableStateOf(false) }
    var showResetConfirm by remember { mutableStateOf(false) }
    var showRotateConfirm by remember { mutableStateOf(false) }

    val scroll = rememberScrollState()

    Box(modifier = Modifier.fillMaxSize().background(Ink.bg)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scroll)
                .padding(top = 62.dp, bottom = 140.dp)
        ) {
            Column(Modifier.padding(horizontal = 20.dp)) {
                SectionLabel("Settings")
                Spacer(Modifier.height(6.dp))
                Text(
                    "Preferences",
                    color = Ink.fg,
                    style = TextStyle(fontSize = 34.sp, letterSpacing = (-1.1).sp)
                )
            }

            // ─── Rotation ───
            SettingsSection("Rotation · ${active?.name ?: "No group"}") {
                IntervalPicker(
                    current = active?.interval ?: GroupRepository.getDefaultInterval(),
                    onSelect = { interval ->
                        active?.let { GroupRepository.setRotationInterval(it.id, interval) }
                            ?: GroupRepository.setDefaultInterval(interval)
                    }
                )
                Divider()
                ToggleRow("Notify on rotation", notify) {
                    notify = it; GroupRepository.setNotifyOnRotation(it)
                }
                Divider()
                ToggleRow("Include preview of next word", preview) {
                    preview = it; GroupRepository.setPreviewNextWord(it)
                }
            }

            // ─── Accessibility ───
            SettingsSection("Accessibility") {
                ToggleRow("High visibility mode", plainMode, onPlainModeChange)
            }

            // ─── Widget & lock screen ───
            SettingsSection("Widget & Lock Screen") {
                ToggleRow("Lock screen glance", lockGlance) {
                    lockGlance = it; GroupRepository.setLockScreenGlance(it)
                }
                Divider()
                ToggleRow("Hide word until unlock", hideUntilUnlock) {
                    hideUntilUnlock = it; GroupRepository.setHideUntilUnlock(it)
                }
            }

            // ─── Security ───
            SettingsSection("Security") {
                ToggleRow("Require biometrics to open", biometricRequired) {
                    biometricRequired = it; GroupRepository.setBiometricRequired(it)
                }
                Divider()
                ActionRow(
                    label = "Emergency override word",
                    value = if (active != null && GroupRepository.getEmergencyOverrideWord(active.id) != null) "Set" else "Off",
                    accent = active != null && GroupRepository.getEmergencyOverrideWord(active.id) != null,
                    enabled = active != null,
                    onClick = { showOverrideSheet = true }
                )
                Divider()
                ActionRow(
                    label = "Rotate group seed",
                    enabled = active != null,
                    onClick = { showRotateConfirm = true }
                )
            }

            // ─── Tools ───
            SettingsSection("Tools") {
                ActionRow(
                    label = "Single use word generator",
                    value = "Open",
                    onClick = onOpenGenerator
                )
            }

            // ─── Practice ───
            SettingsSection("Practice") {
                ActionRow(
                    label = "Run a scam drill",
                    enabled = active != null,
                    onClick = onRunDrill
                )
                Divider()
                ActionRow(
                    label = "Drill history",
                    value = "${com.thc.safewords.service.DrillService.getHistory().count { it.passed }} passed",
                    onClick = onDrillHistory
                )
            }

            // ─── Danger zone ───
            SettingsSection("Danger zone") {
                DangerRow("Leave this group", enabled = active != null) { showLeaveConfirm = true }
                Divider()
                DangerRow("Reset device") { showResetConfirm = true }
            }

            Spacer(Modifier.height(24.dp))
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 40.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Safewords v1.1.9 · Offline-first",
                    color = Ink.fgFaint,
                    style = TextStyle(fontSize = 11.sp, letterSpacing = 0.3.sp),
                    textAlign = TextAlign.Center
                )
                Text(
                    "No server. No account. No data collection.",
                    color = Ink.fgFaint,
                    style = TextStyle(fontSize = 11.sp, letterSpacing = 0.3.sp),
                    textAlign = TextAlign.Center
                )
            }
        }
    }

    // ─── Confirmation dialogs ───
    if (showLeaveConfirm && active != null) {
        ConfirmDialog(
            title = "Leave \"${active.name}\"?",
            body = "You'll lose your safeword for this group. To rejoin, someone will need to share their QR code with you again.",
            confirmLabel = "Leave",
            destructive = true,
            onConfirm = {
                GroupRepository.deleteGroup(active.id)
                showLeaveConfirm = false
            },
            onDismiss = { showLeaveConfirm = false }
        )
    }
    if (showResetConfirm) {
        ConfirmDialog(
            title = "Reset everything?",
            body = "This deletes every group, every seed, and every preference. You can't undo this.",
            confirmLabel = "Reset",
            destructive = true,
            onConfirm = {
                GroupRepository.resetAllData()
                showResetConfirm = false
            },
            onDismiss = { showResetConfirm = false }
        )
    }
    if (showRotateConfirm && active != null) {
        ConfirmDialog(
            title = "Rotate seed for \"${active.name}\"?",
            body = "This changes the safeword stream for this group. Other members will need to scan a new QR code.",
            confirmLabel = "Rotate",
            destructive = true,
            onConfirm = {
                GroupRepository.rotateGroupSeed(active.id)
                showRotateConfirm = false
            },
            onDismiss = { showRotateConfirm = false }
        )
    }
    if (showOverrideSheet && active != null) {
        EmergencyOverrideSheet(
            group = active,
            currentWord = GroupRepository.getEmergencyOverrideWord(active.id) ?: "",
            onDismiss = { showOverrideSheet = false },
            onSave = {
                GroupRepository.setEmergencyOverrideWord(active.id, it)
                showOverrideSheet = false
            },
            onClear = {
                GroupRepository.setEmergencyOverrideWord(active.id, null)
                showOverrideSheet = false
            }
        )
    }
}

// ─── Reusable rows ────────────────────────────────────────────────

@Composable
private fun SettingsSection(label: String, content: @Composable () -> Unit) {
    Column(Modifier.padding(top = 22.dp)) {
        SectionLabel(label, modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp))
        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(Ink.bgElev)
                .border(0.5.dp, Ink.rule, RoundedCornerShape(20.dp))
        ) { content() }
    }
}

@Composable
private fun Divider() {
    Box(modifier = Modifier.fillMaxWidth().height(0.5.dp).padding(start = 16.dp).background(Ink.rule))
}

@Composable
private fun ActionRow(
    label: String,
    value: String? = null,
    accent: Boolean = false,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            color = if (enabled) Ink.fg else Ink.fgMuted,
            style = TextStyle(fontSize = 14.5.sp),
            modifier = Modifier.weight(1f)
        )
        if (value != null) {
            Text(
                value,
                color = if (accent) Ink.accent else Ink.fgMuted,
                style = TextStyle(fontSize = 13.5.sp)
            )
        }
        Spacer(Modifier.width(8.dp))
        Icon(
            Icons.AutoMirrored.Outlined.KeyboardArrowRight,
            null,
            tint = Ink.fgFaint,
            modifier = Modifier.size(16.dp)
        )
    }
}

@Composable
private fun ToggleRow(label: String, value: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = Ink.fg, style = TextStyle(fontSize = 14.5.sp), modifier = Modifier.weight(1f))
        Switch(
            checked = value,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Ink.accentInk,
                checkedTrackColor = Ink.accent,
                uncheckedThumbColor = Ink.fgMuted,
                uncheckedTrackColor = Ink.bgInset,
                uncheckedBorderColor = Ink.rule
            )
        )
    }
}

@Composable
private fun DangerRow(label: String, enabled: Boolean = true, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Text(
            label,
            color = if (enabled) Ink.accent else Ink.fgMuted,
            style = TextStyle(fontSize = 14.5.sp)
        )
    }
}

// ─── Interval picker ───────────────────────────────────────────────

@Composable
private fun IntervalPicker(current: RotationInterval, onSelect: (RotationInterval) -> Unit) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
        Text("Interval", color = Ink.fgMuted, style = TextStyle(fontSize = 13.sp))
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
            RotationInterval.entries.forEach { i ->
                val on = i == current
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (on) Ink.accent else Ink.bgInset)
                        .clickable { onSelect(i) }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        intervalShort(i),
                        color = if (on) Ink.accentInk else Ink.fg,
                        style = TextStyle(fontSize = 12.5.sp)
                    )
                }
            }
        }
    }
}

private fun intervalShort(i: RotationInterval): String = when (i) {
    RotationInterval.HOURLY -> "1 hour"
    RotationInterval.DAILY -> "1 day"
    RotationInterval.WEEKLY -> "1 week"
    RotationInterval.MONTHLY -> "1 month"
}

// ─── Confirmation dialog ───────────────────────────────────────────

@Composable
private fun ConfirmDialog(
    title: String,
    body: String,
    confirmLabel: String,
    destructive: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, color = Ink.fg) },
        text = { Text(body, color = Ink.fgMuted) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(confirmLabel, color = if (destructive) Ink.accent else Ink.fg)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = Ink.fgMuted) }
        },
        containerColor = Ink.bgElev,
        titleContentColor = Ink.fg,
        textContentColor = Ink.fgMuted
    )
}

// ─── Emergency override sheet ──────────────────────────────────────

@Composable
private fun EmergencyOverrideSheet(
    group: Group,
    currentWord: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
    onClear: () -> Unit
) {
    var typed by remember { mutableStateOf(currentWord) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Emergency override · ${group.name}", color = Ink.fg) },
        text = {
            Column {
                Text(
                    "If a family member can't reach today's safeword, they can use this fallback word instead. Pick something only your family would know.",
                    color = Ink.fgMuted,
                    style = TextStyle(fontSize = 13.sp, lineHeight = 18.sp)
                )
                Spacer(Modifier.height(16.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Ink.bgInset)
                        .padding(horizontal = 14.dp, vertical = 12.dp)
                ) {
                    BasicTextField(
                        value = typed,
                        onValueChange = { typed = it },
                        singleLine = true,
                        textStyle = TextStyle(fontSize = 17.sp, color = Ink.fg),
                        cursorBrush = SolidColor(Ink.accent),
                        decorationBox = { inner ->
                            if (typed.isEmpty()) Text("e.g. blue duck", color = Ink.fgFaint, style = TextStyle(fontSize = 17.sp))
                            inner()
                        }
                    )
                }
                Spacer(Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(Ink.bgInset)
                        .clickable { typed = WordGenerator.phrase(WordGenerator.Style.TwoWords).lowercase() }
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Text(
                        "Generate one for me",
                        color = Ink.accent,
                        style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(typed.trim()) }, enabled = typed.isNotBlank()) {
                Text("Save", color = if (typed.isNotBlank()) Ink.accent else Ink.fgMuted)
            }
        },
        dismissButton = {
            Row {
                if (currentWord.isNotBlank()) {
                    TextButton(onClick = onClear) { Text("Clear", color = Ink.fgMuted) }
                }
                TextButton(onClick = onDismiss) { Text("Cancel", color = Ink.fgMuted) }
            }
        },
        containerColor = Ink.bgElev,
        titleContentColor = Ink.fg,
        textContentColor = Ink.fgMuted
    )
}
