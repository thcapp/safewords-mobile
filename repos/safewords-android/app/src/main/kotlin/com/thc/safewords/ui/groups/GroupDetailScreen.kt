package com.thc.safewords.ui.groups

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.thc.safewords.crypto.TOTPDerivation
import com.thc.safewords.model.Member
import com.thc.safewords.model.Role
import com.thc.safewords.model.RotationInterval
import com.thc.safewords.service.GroupRepository
import com.thc.safewords.ui.components.SafewordDisplay
import com.thc.safewords.ui.theme.Amber
import com.thc.safewords.ui.theme.AvatarColors
import com.thc.safewords.ui.theme.Background
import com.thc.safewords.ui.theme.Error
import com.thc.safewords.ui.theme.Surface
import com.thc.safewords.ui.theme.SurfaceVariant
import com.thc.safewords.ui.theme.Teal
import com.thc.safewords.ui.theme.TextMuted
import com.thc.safewords.ui.theme.TextPrimary
import com.thc.safewords.ui.theme.TextSecondary
import kotlinx.coroutines.delay
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupDetailScreen(
    groupId: String,
    onBack: () -> Unit,
    onInvite: () -> Unit,
    onDeleted: () -> Unit
) {
    val groups by GroupRepository.groups.collectAsState()
    val group = groups.firstOrNull { it.id == groupId }
    var isEditingName by remember { mutableStateOf(false) }
    var editedName by remember { mutableStateOf("") }
    var showIntervalMenu by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var tick by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(1000L)
            tick++
        }
    }

    if (group == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Background),
            contentAlignment = Alignment.Center
        ) {
            Text("Group not found", color = TextMuted)
        }
        return
    }

    val phrase = remember(groupId, tick) {
        GroupRepository.getCurrentSafeword(groupId) ?: ""
    }
    val timeRemaining = remember(groupId, tick) {
        TOTPDerivation.getTimeRemaining(group.interval.seconds)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
    ) {
        TopAppBar(
            title = {
                if (isEditingName) {
                    OutlinedTextField(
                        value = editedName,
                        onValueChange = { editedName = it },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("group-detail.name-edit"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Teal,
                            unfocusedBorderColor = SurfaceVariant,
                            cursorColor = Teal,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )
                } else {
                    Text(
                        text = group.name,
                        color = TextPrimary,
                        modifier = Modifier.testTag("group-detail.name-edit"),
                    )
                }
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = TextPrimary
                    )
                }
            },
            actions = {
                if (isEditingName) {
                    IconButton(onClick = {
                        if (editedName.isNotBlank()) {
                            GroupRepository.updateGroup(group.copy(name = editedName.trim()))
                        }
                        isEditingName = false
                    }) {
                        Icon(Icons.Default.Check, contentDescription = "Save", tint = Teal)
                    }
                } else {
                    IconButton(onClick = {
                        editedName = group.name
                        isEditingName = true
                    }) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit name", tint = TextSecondary)
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Background
            )
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Current safeword
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Surface),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Current Safeword",
                            style = MaterialTheme.typography.labelMedium,
                            color = TextMuted
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        if (phrase.isNotEmpty()) {
                            SafewordDisplay(phrase = phrase, isLarge = true)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Rotates in ${TOTPDerivation.formatTimeRemaining(timeRemaining)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted
                        )
                    }
                }
            }

            // Rotation interval
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Surface),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Rotation Interval",
                            style = MaterialTheme.typography.titleSmall,
                            color = TextMuted
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Box {
                            OutlinedButton(
                                onClick = { showIntervalMenu = true },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("group-detail.interval-picker"),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = TextPrimary
                                )
                            ) {
                                Text(group.interval.displayName)
                            }
                            DropdownMenu(
                                expanded = showIntervalMenu,
                                onDismissRequest = { showIntervalMenu = false }
                            ) {
                                RotationInterval.entries.forEach { interval ->
                                    DropdownMenuItem(
                                        text = { Text(interval.displayName) },
                                        onClick = {
                                            GroupRepository.updateGroup(group.copy(interval = interval))
                                            showIntervalMenu = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Members
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Surface),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Members",
                                style = MaterialTheme.typography.titleSmall,
                                color = TextMuted
                            )
                            TextButton(
                                onClick = onInvite,
                                modifier = Modifier.testTag("group-detail.invite-cta"),
                            ) {
                                Icon(
                                    Icons.Default.PersonAdd,
                                    contentDescription = null,
                                    tint = Teal,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Invite", color = Teal)
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        group.members.forEachIndexed { index, member ->
                            MemberRow(member = member, colorIndex = index)
                            if (index < group.members.size - 1) {
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }
                    }
                }
            }

            // Delete button
            item {
                Button(
                    onClick = { showDeleteDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("group-detail.danger-leave"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Error.copy(alpha = 0.15f),
                        contentColor = Error
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Delete Group")
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    // Delete confirmation
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Group") },
            text = {
                Text("Are you sure you want to delete \"${group.name}\"? This cannot be undone.")
            },
            confirmButton = {
                TextButton(onClick = {
                    GroupRepository.deleteGroup(groupId)
                    showDeleteDialog = false
                    onDeleted()
                }) {
                    Text("Delete", color = Error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            },
            containerColor = Surface,
            titleContentColor = TextPrimary,
            textContentColor = TextSecondary
        )
    }
}

@Composable
private fun MemberRow(
    member: Member,
    colorIndex: Int
) {
    val avatarColor = AvatarColors[colorIndex % AvatarColors.size]
    val initials = member.name
        .split(" ")
        .take(2)
        .mapNotNull { it.firstOrNull()?.uppercase(Locale.ROOT) }
        .joinToString("")
        .ifEmpty { "?" }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        // Avatar circle
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(avatarColor.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = initials,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = avatarColor
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = member.name,
                style = MaterialTheme.typography.bodyLarge,
                color = TextPrimary
            )
            Text(
                text = if (member.role == Role.CREATOR) "Creator" else "Member",
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted
            )
        }
    }
}
