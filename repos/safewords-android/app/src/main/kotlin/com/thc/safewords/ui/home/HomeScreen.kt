package com.thc.safewords.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.thc.safewords.crypto.TOTPDerivation
import com.thc.safewords.model.Group
import com.thc.safewords.service.GroupRepository
import com.thc.safewords.ui.components.CountdownRing
import com.thc.safewords.ui.components.SafewordDisplay
import com.thc.safewords.ui.theme.Amber
import com.thc.safewords.ui.theme.Background
import com.thc.safewords.ui.theme.Teal
import com.thc.safewords.ui.theme.TextMuted
import com.thc.safewords.ui.theme.TextSecondary
import kotlinx.coroutines.delay

@Composable
fun HomeScreen(
    onNavigateToGroups: () -> Unit
) {
    val groups by GroupRepository.groups.collectAsState()
    var selectedGroup by remember { mutableStateOf<Group?>(null) }
    var currentPhrase by remember { mutableStateOf("") }
    var timeRemaining by remember { mutableLongStateOf(0L) }
    var progress by remember { mutableFloatStateOf(1f) }
    var dropdownExpanded by remember { mutableStateOf(false) }

    // Select the first group by default, or update if groups change
    LaunchedEffect(groups) {
        if (groups.isNotEmpty()) {
            if (selectedGroup == null || groups.none { it.id == selectedGroup?.id }) {
                selectedGroup = groups.first()
            }
        } else {
            selectedGroup = null
        }
    }

    // Update safeword and countdown every second
    LaunchedEffect(selectedGroup) {
        val group = selectedGroup ?: return@LaunchedEffect
        while (true) {
            val seed = GroupRepository.getGroupSeed(group.id)
            if (seed != null) {
                val now = System.currentTimeMillis() / 1000
                currentPhrase = TOTPDerivation.deriveSafeword(seed, group.interval.seconds, now)
                timeRemaining = TOTPDerivation.getTimeRemaining(group.interval.seconds)
                progress = timeRemaining.toFloat() / group.interval.seconds.toFloat()
            }
            delay(1000L)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
    ) {
        if (groups.isEmpty()) {
            // Empty state
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Safewords",
                    style = MaterialTheme.typography.displaySmall,
                    color = Teal
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Create a group or join one to start generating rotating safewords for your family.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(32.dp))
                Button(
                    onClick = onNavigateToGroups,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Amber,
                        contentColor = Background
                    )
                ) {
                    Text("Get Started")
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(24.dp))

                // Group selector
                if (groups.size > 1) {
                    Box {
                        OutlinedButton(
                            onClick = { dropdownExpanded = true },
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = TextSecondary
                            )
                        ) {
                            Text(selectedGroup?.name ?: "Select Group")
                        }
                        DropdownMenu(
                            expanded = dropdownExpanded,
                            onDismissRequest = { dropdownExpanded = false }
                        ) {
                            groups.forEach { group ->
                                DropdownMenuItem(
                                    text = { Text(group.name) },
                                    onClick = {
                                        selectedGroup = group
                                        dropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                } else {
                    Text(
                        text = selectedGroup?.name ?: "",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextSecondary
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                // Countdown ring with safeword
                CountdownRing(
                    progress = progress,
                    size = 280.dp,
                    strokeWidth = 12.dp
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (currentPhrase.isNotEmpty()) {
                            SafewordDisplay(
                                phrase = currentPhrase,
                                isLarge = true
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Time remaining
                Text(
                    text = "Rotates in ${TOTPDerivation.formatTimeRemaining(timeRemaining)}",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextMuted
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Interval label
                Text(
                    text = selectedGroup?.interval?.displayName ?: "",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextMuted
                )

                Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}
