package com.thc.safewords.ui.drills

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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.thc.safewords.service.DrillService
import com.thc.safewords.service.GroupRepository
import com.thc.safewords.ui.components.SectionLabel
import com.thc.safewords.ui.theme.Ink
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class Phase { Idle, Prompt, Result }

@Composable
fun DrillsScreen(onBack: () -> Unit) {
    val groups by GroupRepository.groups.collectAsState()
    val activeId by GroupRepository.activeGroupId.collectAsState()
    val active = groups.firstOrNull { it.id == activeId } ?: groups.firstOrNull()

    var phase by remember { mutableStateOf(Phase.Idle) }
    var prompt by remember { mutableStateOf<DrillService.DrillPrompt?>(null) }
    var lastResult by remember { mutableStateOf<Pair<DrillService.DrillPrompt, Boolean>?>(null) }
    var historyTick by remember { mutableStateOf(0) }
    val history = remember(historyTick) { DrillService.getHistory() }

    val scroll = rememberScrollState()

    Box(modifier = Modifier.fillMaxSize().background(Ink.bg)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scroll)
                .padding(top = 62.dp, bottom = 140.dp)
                .padding(horizontal = 20.dp)
        ) {
            // Header
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
                Column {
                    SectionLabel("Practice")
                    Spacer(Modifier.height(2.dp))
                    Text(
                        "Scam drills",
                        color = Ink.fg,
                        style = TextStyle(fontSize = 30.sp, letterSpacing = (-1).sp)
                    )
                }
            }

            Spacer(Modifier.height(20.dp))
            Text(
                "Practice spotting fake calls before they happen. Each drill picks a real-sounding scenario — half use the right word, half use a slight twist.",
                color = Ink.fgMuted,
                style = TextStyle(fontSize = 15.sp, lineHeight = 22.sp)
            )

            Spacer(Modifier.height(20.dp))

            when (phase) {
                Phase.Idle -> IdlePanel(
                    historyCount = history.size,
                    passedCount = history.count { it.passed },
                    onStart = {
                        active?.id?.let { gid ->
                            DrillService.nextDrill(gid)?.let {
                                prompt = it
                                phase = Phase.Prompt
                            }
                        }
                    },
                    enabled = active != null
                )
                Phase.Prompt -> PromptPanel(
                    prompt = prompt!!,
                    onAnswer = { userSaidMatch ->
                        val p = prompt!!
                        DrillService.submit(p, userSaidMatch)
                        lastResult = p to (userSaidMatch == p.isCorrect)
                        phase = Phase.Result
                        historyTick++
                    }
                )
                Phase.Result -> ResultPanel(
                    prompt = lastResult!!.first,
                    passed = lastResult!!.second,
                    onAgain = {
                        active?.id?.let { gid ->
                            DrillService.nextDrill(gid)?.let {
                                prompt = it
                                phase = Phase.Prompt
                            }
                        }
                    },
                    onDone = { phase = Phase.Idle }
                )
            }

            if (phase == Phase.Idle && history.isNotEmpty()) {
                Spacer(Modifier.height(28.dp))
                SectionLabel("History")
                Spacer(Modifier.height(8.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(Ink.bgElev)
                        .border(0.5.dp, Ink.rule, RoundedCornerShape(20.dp))
                ) {
                    history.take(10).forEachIndexed { i, session ->
                        if (i > 0) Box(modifier = Modifier.fillMaxWidth().height(0.5.dp).background(Ink.rule))
                        HistoryRow(session, index = i)
                    }
                }
            }
        }
    }
}

@Composable
private fun IdlePanel(
    historyCount: Int,
    passedCount: Int,
    onStart: () -> Unit,
    enabled: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Ink.bgElev)
            .border(0.5.dp, Ink.rule, RoundedCornerShape(20.dp))
            .padding(20.dp)
    ) {
        if (historyCount > 0) {
            Text(
                "$passedCount of $historyCount passed",
                color = Ink.accent,
                style = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Keep practicing — the goal is to spot the wrong word fast.",
                color = Ink.fgMuted,
                style = TextStyle(fontSize = 13.sp, lineHeight = 18.sp)
            )
        } else {
            Text(
                "Run your first drill",
                color = Ink.fg,
                style = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "We'll show you a fake scenario and the word the caller gave. You decide if it's right.",
                color = Ink.fgMuted,
                style = TextStyle(fontSize = 13.sp, lineHeight = 18.sp)
            )
        }
        Spacer(Modifier.height(16.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(CircleShape)
                .background(if (enabled) Ink.accent else Ink.bgInset)
                .testTag("drills.start")
                .clickable(enabled = enabled, onClick = onStart)
                .padding(vertical = 14.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "Run drill now",
                color = if (enabled) Ink.accentInk else Ink.fgMuted,
                style = TextStyle(fontSize = 14.5.sp, fontWeight = FontWeight.SemiBold)
            )
        }
    }
}

@Composable
private fun PromptPanel(
    prompt: DrillService.DrillPrompt,
    onAnswer: (userSaidMatch: Boolean) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Ink.bgElev)
            .border(0.5.dp, Ink.rule, RoundedCornerShape(20.dp))
            .padding(20.dp)
    ) {
        SectionLabel("Scenario", color = Ink.accent)
        Spacer(Modifier.height(10.dp))
        Text(
            prompt.scenario,
            color = Ink.fg,
            style = TextStyle(fontSize = 17.sp, lineHeight = 24.sp),
            modifier = Modifier.testTag("drills.scenario"),
        )
        Spacer(Modifier.height(20.dp))
        Text(
            "Is the word right or wrong?",
            color = Ink.fgMuted,
            style = TextStyle(fontSize = 14.sp)
        )
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            AnswerButton("Right word", true, onClick = { onAnswer(true) }, testTagId = "drills.passed")
            AnswerButton("Wrong word", false, onClick = { onAnswer(false) }, testTagId = "drills.failed")
        }
    }
}

@Composable
private fun AnswerButton(label: String, positive: Boolean, onClick: () -> Unit, testTagId: String? = null) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (positive) Ink.ok.copy(alpha = 0.18f) else Ink.tickFill)
            .border(1.dp, if (positive) Ink.ok else Ink.accent, RoundedCornerShape(12.dp))
            .then(if (testTagId != null) Modifier.testTag(testTagId) else Modifier)
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 14.dp)
    ) {
        Text(
            label,
            color = if (positive) Ink.ok else Ink.accent,
            style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        )
    }
}

@Composable
private fun ResultPanel(
    prompt: DrillService.DrillPrompt,
    passed: Boolean,
    onAgain: () -> Unit,
    onDone: () -> Unit
) {
    val tone = if (passed) Ink.ok else Ink.accent
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Ink.bgElev)
            .border(1.dp, tone, RoundedCornerShape(20.dp))
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(tone.copy(alpha = 0.15f))
                .border(1.dp, tone, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                if (passed) Icons.Outlined.Check else Icons.Outlined.Close,
                null,
                tint = tone,
                modifier = Modifier.size(32.dp)
            )
        }
        Spacer(Modifier.height(12.dp))
        Text(
            if (passed) "Nice — you got it." else "Watch out for that one.",
            color = tone,
            style = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
        )
        Spacer(Modifier.height(8.dp))
        Text(
            if (prompt.isCorrect)
                "The word in the scenario was correct, so you should have said \"Right word\"."
            else
                "The word in the scenario was wrong — the right word is something else. You should have said \"Wrong word\".",
            color = Ink.fgMuted,
            style = TextStyle(fontSize = 14.sp, lineHeight = 20.sp),
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 320.dp)
        )

        Spacer(Modifier.height(20.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Ink.accent)
                    .clickable(onClick = onAgain)
                    .padding(horizontal = 22.dp, vertical = 12.dp)
            ) {
                Text("Run another", color = Ink.accentInk, style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.SemiBold))
            }
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .border(0.5.dp, Ink.rule, CircleShape)
                    .clickable(onClick = onDone)
                    .padding(horizontal = 22.dp, vertical = 12.dp)
            ) {
                Text("Done", color = Ink.fgMuted, style = TextStyle(fontSize = 14.sp))
            }
        }
    }
}

@Composable
private fun HistoryRow(session: DrillService.DrillSession, index: Int = 0) {
    val tone = if (session.passed) Ink.ok else Ink.accent
    val date = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
        .format(Date(session.timestamp * 1000L))

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("drills.history-row.$index")
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(28.dp).clip(CircleShape).background(tone.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                if (session.passed) Icons.Outlined.Check else Icons.Outlined.Close,
                null,
                tint = tone,
                modifier = Modifier.size(14.dp)
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                if (session.passed) "Passed" else "Missed",
                color = tone,
                style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            )
            Text(
                date,
                color = Ink.fgMuted,
                style = TextStyle(fontSize = 11.5.sp)
            )
        }
        Text(
            "saw \"${session.presentedWord}\"",
            color = Ink.fgFaint,
            style = TextStyle(fontSize = 11.5.sp, letterSpacing = 0.3.sp)
        )
    }
}
