package com.thc.safewords.ui.groups

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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.thc.safewords.crypto.TOTPDerivation
import com.thc.safewords.model.Group
import com.thc.safewords.model.Member
import com.thc.safewords.model.RotationInterval
import com.thc.safewords.service.GroupRepository
import com.thc.safewords.ui.components.GroupDot
import com.thc.safewords.ui.components.SectionLabel
import com.thc.safewords.ui.theme.DotPalette
import com.thc.safewords.ui.theme.Ink

@Composable
fun GroupsScreen(
    onGroupClick: (String) -> Unit,
    onScanQR: () -> Unit,
    onAddMember: () -> Unit
) {
    val groups by GroupRepository.groups.collectAsState()
    val selectedId by GroupRepository.activeGroupId.collectAsState()
    val active = groups.firstOrNull { it.id == selectedId } ?: groups.firstOrNull()
    val scroll = rememberScrollState()

    Box(modifier = Modifier.fillMaxSize().background(Ink.bg)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scroll)
                .padding(top = 62.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                Column(Modifier.weight(1f)) {
                    SectionLabel("Groups")
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Your circles",
                        color = Ink.fg,
                        style = TextStyle(fontSize = 34.sp, letterSpacing = (-1.1).sp)
                    )
                }
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Ink.bgElev)
                        .border(0.5.dp, Ink.rule, CircleShape)
                        .clickable { onAddMember() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Outlined.Add, null, tint = Ink.fg, modifier = Modifier.size(17.dp))
                }
            }

            Spacer(Modifier.height(24.dp))
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                groups.forEachIndexed { idx, g ->
                    GroupCard(
                        group = g,
                        active = g.id == selectedId,
                        idx = idx,
                        onClick = {
                            GroupRepository.setActiveGroup(g.id)
                            onGroupClick(g.id)
                        }
                    )
                }
                active?.let { a ->
                    Spacer(Modifier.height(10.dp))
                    SectionLabel("Members · ${a.name}", modifier = Modifier.padding(horizontal = 4.dp))
                    Spacer(Modifier.height(8.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(Ink.bgElev)
                            .border(0.5.dp, Ink.rule, RoundedCornerShape(20.dp))
                    ) {
                        a.members.forEachIndexed { i, m ->
                            if (i > 0) Box(
                                modifier = Modifier.fillMaxWidth().height(0.5.dp).padding(start = 60.dp).background(Ink.rule)
                            )
                            MemberRow(m)
                        }
                    }
                }
                Spacer(Modifier.height(140.dp))
            }
        }
    }
}

@Composable
private fun GroupCard(group: Group, active: Boolean, idx: Int, onClick: () -> Unit) {
    val currentWord = remember(group.id) {
        val seed = GroupRepository.getGroupSeed(group.id)
        if (seed != null) {
            TOTPDerivation.deriveSafeword(
                seed,
                group.interval.seconds,
                System.currentTimeMillis() / 1000
            )
        } else "—"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Ink.bgElev)
            .border(0.5.dp, if (active) Ink.accent else Ink.rule, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        GroupDot(
            initial = group.name.take(1),
            color = DotPalette[idx % DotPalette.size],
            size = 44.dp
        )
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    group.name,
                    color = Ink.fg,
                    style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.2).sp)
                )
                if (active) {
                    Spacer(Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Ink.tickFill)
                            .padding(horizontal = 7.dp, vertical = 2.dp)
                    ) {
                        Text("ACTIVE", color = Ink.accent, style = TextStyle(fontSize = 10.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.5.sp))
                    }
                }
            }
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("${group.members.size} members", color = Ink.fgMuted, style = TextStyle(fontSize = 12.5.sp))
                Spacer(Modifier.width(10.dp))
                Text("·", color = Ink.fgFaint)
                Spacer(Modifier.width(10.dp))
                Text("rotates ${intervalLabel(group.interval)}", color = Ink.fgMuted, style = TextStyle(fontSize = 12.5.sp))
            }
            Spacer(Modifier.height(8.dp))
            Text(
                currentWord,
                color = if (active) Ink.accent else Ink.fg.copy(alpha = 0.7f),
                style = TextStyle(fontSize = 12.5.sp, letterSpacing = 0.3.sp)
            )
        }
        Text(
            "SEQ ${sequenceFor(group)}",
            color = Ink.fgFaint,
            style = TextStyle(fontSize = 10.sp, letterSpacing = 1.sp),
            modifier = Modifier.rotate(-90f)
        )
    }
}

@Composable
private fun MemberRow(member: Member) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        GroupDot(
            initial = member.name.take(1),
            color = DotPalette[kotlin.math.abs(member.id.hashCode()) % DotPalette.size],
            size = 32.dp
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                member.name,
                color = Ink.fg,
                style = TextStyle(fontSize = 14.5.sp, fontWeight = FontWeight.Medium)
            )
            Spacer(Modifier.height(1.dp))
            Text("Last seen just now", color = Ink.fgMuted, style = TextStyle(fontSize = 11.5.sp))
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Ink.ok))
            Spacer(Modifier.width(5.dp))
            Text("SYNCED", color = Ink.ok, style = TextStyle(fontSize = 11.5.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.3.sp))
        }
    }
}

private fun intervalLabel(i: RotationInterval): String = when (i) {
    RotationInterval.HOURLY -> "1 hour"
    RotationInterval.DAILY -> "1 day"
    RotationInterval.WEEKLY -> "1 week"
    RotationInterval.MONTHLY -> "1 month"
}

private fun sequenceFor(group: Group): Int {
    val counter = (System.currentTimeMillis() / 1000) / group.interval.seconds
    return (counter % 10_000).toInt()
}
