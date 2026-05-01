package com.thc.safewords.ui.verify

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.thc.safewords.crypto.Primitives
import com.thc.safewords.service.GroupRepository
import com.thc.safewords.ui.theme.Ink
import kotlin.random.Random

/**
 * v1.3 challenge/answer sheet. Picks a row deterministically per session,
 * shows "Ask: APPLE" + "Expect: ORANGE", and gives the user two big buttons:
 * Match (they answered correctly) or Doesn't match (they didn't).
 *
 * No free-text input — the answer is on the screen and the only decision is
 * whether what the other side said equals the expected response.
 */
@Composable
fun ChallengeSheet(groupId: String, onDone: () -> Unit) {
    val rowCount = remember {
        GroupRepository.getGroup(groupId)?.primitivesOrDefault()?.challengeAnswer?.rowCount ?: 100
    }
    var rowIndex by remember { mutableIntStateOf(Random.nextInt(rowCount)) }
    val row = remember(groupId, rowIndex) {
        GroupRepository.getGroup(groupId)?.let { g ->
            val table = GroupRepository.getChallengeAnswerTable(groupId, rowCount)
            table?.firstOrNull { it.rowIndex == rowIndex } ?: table?.firstOrNull()
        }
    }
    var phase by remember { mutableStateOf(Phase.Asking) }

    Box(modifier = Modifier.fillMaxSize().background(Ink.bg).padding(20.dp)) {
        Column(modifier = Modifier.fillMaxSize().padding(top = 60.dp)) {
            when (phase) {
                Phase.Asking -> AskingPanel(
                    row = row,
                    onMatch = { phase = Phase.Match },
                    onMismatch = { phase = Phase.Mismatch },
                    onAnotherRow = { rowIndex = Random.nextInt(rowCount) },
                    onClose = onDone,
                )
                Phase.Match -> Result(match = true, onClose = onDone)
                Phase.Mismatch -> Result(match = false, onClose = onDone)
            }
        }
    }
}

private enum class Phase { Asking, Match, Mismatch }

@Composable
private fun AskingPanel(
    row: Primitives.ChallengeAnswerRow?,
    onMatch: () -> Unit,
    onMismatch: () -> Unit,
    onAnotherRow: () -> Unit,
    onClose: () -> Unit,
) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Ink.bgElev)
                .border(0.5.dp, Ink.rule, RoundedCornerShape(10.dp))
                .clickable(onClick = onClose)
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Outlined.Close, null, tint = Ink.fg, modifier = Modifier.size(16.dp))
        }
        Spacer(Modifier.size(10.dp))
        Text(
            "Verify",
            color = Ink.fg,
            style = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.4).sp)
        )
    }

    Spacer(Modifier.height(28.dp))
    Text(
        "Ask them",
        color = Ink.fgMuted,
        style = TextStyle(fontSize = 13.sp, letterSpacing = 0.6.sp, fontWeight = FontWeight.SemiBold)
    )
    Spacer(Modifier.height(8.dp))
    Text(
        row?.ask ?: "—",
        color = Ink.fg,
        style = TextStyle(fontSize = 36.sp, lineHeight = 42.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.6).sp)
    )

    Spacer(Modifier.height(36.dp))
    Text(
        "They should answer",
        color = Ink.fgMuted,
        style = TextStyle(fontSize = 13.sp, letterSpacing = 0.6.sp, fontWeight = FontWeight.SemiBold)
    )
    Spacer(Modifier.height(8.dp))
    Text(
        row?.expect ?: "—",
        color = Ink.fg,
        style = TextStyle(fontSize = 28.sp, lineHeight = 34.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.4).sp)
    )

    Spacer(Modifier.height(40.dp))
    BigButton(
        label = "They said it",
        icon = Icons.Outlined.Check,
        accent = true,
        onClick = onMatch,
    )
    Spacer(Modifier.height(12.dp))
    BigButton(
        label = "Doesn't match",
        icon = Icons.Outlined.Close,
        accent = false,
        onClick = onMismatch,
    )
    Spacer(Modifier.height(20.dp))
    Text(
        "Use a different row",
        color = Ink.fgMuted,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onAnotherRow)
            .padding(8.dp),
        style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    )
}

@Composable
private fun Result(match: Boolean, onClose: () -> Unit) {
    val color = if (match) Color(0xFF18A058) else Color(0xFFD9534F)
    Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(60.dp))
        Box(
            modifier = Modifier.size(80.dp).clip(RoundedCornerShape(40.dp)).background(color),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                if (match) Icons.Outlined.Check else Icons.Outlined.Close,
                null,
                tint = Color.White,
                modifier = Modifier.size(40.dp)
            )
        }
        Spacer(Modifier.height(24.dp))
        Text(
            if (match) "Match" else "Don't trust this caller",
            color = Ink.fg,
            style = TextStyle(fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.4).sp)
        )
        Spacer(Modifier.height(12.dp))
        Text(
            if (match) "You can keep talking, but stay alert."
            else "Hang up. Call them back on a number you already know.",
            color = Ink.fgMuted,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 30.dp),
            style = TextStyle(fontSize = 15.sp, lineHeight = 22.sp)
        )
        Spacer(Modifier.height(36.dp))
        BigButton(label = "Done", icon = null, accent = true, onClick = onClose)
    }
}

@Composable
private fun BigButton(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector?, accent: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(if (accent) Ink.fg else Ink.bgElev)
            .border(if (accent) 0.dp else 1.dp, Ink.rule, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 22.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        icon?.let {
            Icon(it, null, tint = if (accent) Ink.bg else Ink.fg, modifier = Modifier.size(22.dp))
            Spacer(Modifier.size(12.dp))
        }
        Text(
            label,
            color = if (accent) Ink.bg else Ink.fg,
            style = TextStyle(fontSize = 17.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.2).sp),
            modifier = Modifier.weight(1f)
        )
    }
}
