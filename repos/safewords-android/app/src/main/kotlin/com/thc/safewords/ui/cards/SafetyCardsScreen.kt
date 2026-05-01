package com.thc.safewords.ui.cards

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Lock
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import com.thc.safewords.crypto.RecoveryPhrase
import com.thc.safewords.print.CardRenderer
import com.thc.safewords.print.SafetyCardCopy
import com.thc.safewords.service.BiometricService
import com.thc.safewords.service.GroupRepository
import com.thc.safewords.service.QRCodeService
import com.thc.safewords.ui.theme.Ink

/**
 * v1.3 Safety Cards browser. Lists every printable card available for the
 * active group; high-sensitivity templates require biometric unlock before
 * the system print sheet appears.
 */
@Composable
fun SafetyCardsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val activity = context as? FragmentActivity
    val groups by GroupRepository.groups.collectAsState()
    val activeId by GroupRepository.activeGroupId.collectAsState()
    val group = groups.firstOrNull { it.id == activeId } ?: groups.firstOrNull()
    var error by remember { mutableStateOf<String?>(null) }

    Box(modifier = Modifier.fillMaxSize().background(Ink.bg)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(top = 62.dp, bottom = 60.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Ink.bgElev)
                        .border(0.5.dp, Ink.rule, RoundedCornerShape(10.dp))
                        .clickable(onClick = onBack)
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Outlined.ArrowBack, null, tint = Ink.fg, modifier = Modifier.size(16.dp))
                }
                Spacer(Modifier.size(10.dp))
                Text(
                    "Safety cards",
                    color = Ink.fg,
                    style = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.4).sp)
                )
            }

            if (group == null) {
                EmptyState("No active group. Create or join one to print cards.")
                return@Column
            }

            val primitives = group.primitivesOrDefault()
            val groupName = group.name

            CardRow(
                title = "Protocol card",
                subtitle = "Ask. Listen. Hang up if it's wrong.",
                sensitive = false,
                onPrint = {
                    val copy = SafetyCardCopy.card(context, "protocol")
                    val rules = SafetyCardCopy.strList(copy, "rules")
                    val bmp = CardRenderer.renderProtocolCard(
                        context = context,
                        groupName = groupName,
                        rules = rules,
                        title = SafetyCardCopy.str(copy, "title"),
                        subtitle = SafetyCardCopy.str(copy, "subtitle"),
                        footer = SafetyCardCopy.str(copy, "footer"),
                    )
                    CardRenderer.print(context, bmp, "Safewords Protocol")
                }
            )

            if (primitives.staticOverride.enabled) {
                CardRow(
                    title = "Static override card",
                    subtitle = "$groupName · sensitive",
                    sensitive = true,
                    onPrint = {
                        gateThen(activity) {
                            val word = GroupRepository.getStaticOverride(group.id) ?: return@gateThen
                            val copy = SafetyCardCopy.card(context, "staticOverride")
                            val vars = mapOf("groupName" to groupName)
                            val bmp = CardRenderer.renderOverrideCard(
                                groupName = groupName,
                                word = word,
                                warningHeading = SafetyCardCopy.str(copy, "warningHeading", vars),
                                warningBody = SafetyCardCopy.str(copy, "warningBody", vars),
                                footer = SafetyCardCopy.str(copy, "footer", vars),
                            )
                            CardRenderer.print(context, bmp, "Safewords Override")
                            GroupRepository.markStaticOverridePrinted(group.id)
                        }
                    }
                )
            }

            if (primitives.challengeAnswer.enabled) {
                CardRow(
                    title = "Challenge / answer · wallet",
                    subtitle = "First 24 rows · sensitive",
                    sensitive = true,
                    onPrint = {
                        gateThen(activity) {
                            val rows = GroupRepository.getChallengeAnswerTable(group.id, 24) ?: return@gateThen
                            val copy = SafetyCardCopy.card(context, "challengeAnswerWallet")
                            val vars = mapOf("groupName" to groupName)
                            val bmp = CardRenderer.renderChallengeAnswerCard(
                                groupName = groupName,
                                rows = rows.map { it.ask to it.expect },
                                title = SafetyCardCopy.str(copy, "title", vars),
                                subtitle = SafetyCardCopy.str(copy, "subtitle"),
                                warningHeading = SafetyCardCopy.str(copy, "warningHeading", vars),
                                warningBody = SafetyCardCopy.str(copy, "warningBody", vars),
                            )
                            CardRenderer.print(context, bmp, "Safewords Challenge Answer Wallet")
                        }
                    }
                )
                CardRow(
                    title = "Challenge / answer · full",
                    subtitle = "100-row protocol table · sensitive",
                    sensitive = true,
                    onPrint = {
                        gateThen(activity) {
                            val rows = GroupRepository.getChallengeAnswerTable(group.id, primitives.challengeAnswer.rowCount)
                                ?: return@gateThen
                            val copy = SafetyCardCopy.card(context, "challengeAnswerProtocol")
                            val vars = mapOf("groupName" to groupName)
                            val bmp = CardRenderer.renderChallengeAnswerCard(
                                groupName = groupName,
                                rows = rows.map { it.ask to it.expect },
                                title = SafetyCardCopy.str(copy, "title", vars),
                                subtitle = SafetyCardCopy.str(copy, "subtitle"),
                                warningHeading = SafetyCardCopy.str(copy, "warningHeading", vars),
                                warningBody = SafetyCardCopy.str(copy, "warningBody", vars),
                            )
                            CardRenderer.print(context, bmp, "Safewords Challenge Answer Full")
                        }
                    }
                )
            }

            CardRow(
                title = "Recovery phrase card",
                subtitle = "24-word BIP39 · sensitive",
                sensitive = true,
                onPrint = {
                    gateThen(activity) {
                        val seed = GroupRepository.getGroupSeed(group.id) ?: return@gateThen
                        val phrase = try {
                            RecoveryPhrase.encode(seed)
                        } catch (t: Throwable) {
                            error = "Recovery encoding failed: ${t.message}"
                            return@gateThen
                        }
                        val words = phrase.split(' ')
                        val copy = SafetyCardCopy.card(context, "recoveryPhrase")
                        val vars = mapOf("groupName" to groupName)
                        val bmp = CardRenderer.renderRecoveryCard(
                            groupName = groupName,
                            words = words,
                            warningHeading = SafetyCardCopy.str(copy, "warningHeading", vars),
                            warningBody = SafetyCardCopy.str(copy, "warningBody", vars),
                            footer = SafetyCardCopy.str(copy, "footer", vars),
                        )
                        CardRenderer.print(context, bmp, "Safewords Recovery")
                    }
                }
            )

            CardRow(
                title = "Group invite card",
                subtitle = "QR · seed-equivalent · sensitive",
                sensitive = true,
                onPrint = {
                    gateThen(activity) {
                        val qr = QRCodeService.generateQRBitmap(group, size = 1500) ?: return@gateThen
                        val copy = SafetyCardCopy.card(context, "groupInvite")
                        val vars = mapOf("groupName" to groupName)
                        val bmp = CardRenderer.renderInviteCard(
                            groupName = groupName,
                            qrBitmap = qr,
                            title = SafetyCardCopy.str(copy, "title", vars),
                            subtitle = SafetyCardCopy.str(copy, "subtitle"),
                            warningHeading = SafetyCardCopy.str(copy, "warningHeading", vars),
                            warningBody = SafetyCardCopy.str(copy, "warningBody", vars),
                            footer = SafetyCardCopy.str(copy, "footer", vars),
                        )
                        CardRenderer.print(context, bmp, "Safewords Invite")
                    }
                }
            )

            error?.let {
                Text(
                    it,
                    color = Ink.fg,
                    modifier = Modifier.padding(20.dp),
                    style = TextStyle(fontSize = 14.sp)
                )
            }
        }
    }
}

private fun gateThen(activity: FragmentActivity?, action: () -> Unit) {
    if (activity == null) return
    if (!BiometricService.canAuthenticate(activity)) {
        // No usable biometric — proceed (device is presumably already locked
        // by other means; printing is still better than blocking).
        action()
        return
    }
    BiometricService.authenticate(
        activity = activity,
        title = "Confirm to print",
        subtitle = "Sensitive card — anyone with it can verify as you.",
    ) { success -> if (success) action() }
}

@Composable
private fun CardRow(title: String, subtitle: String, sensitive: Boolean, onPrint: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Ink.bgElev)
            .border(0.5.dp, Ink.rule, RoundedCornerShape(16.dp))
            .clickable(onClick = onPrint)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (sensitive) {
                    Icon(Icons.Outlined.Lock, null, tint = Ink.fgMuted, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.size(6.dp))
                }
                Text(
                    title,
                    color = Ink.fg,
                    style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                subtitle,
                color = Ink.fgMuted,
                style = TextStyle(fontSize = 13.sp)
            )
        }
        Text(
            "Print",
            color = Ink.fg,
            style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Bold)
        )
    }
}

@Composable
private fun EmptyState(message: String) {
    Box(
        modifier = Modifier.fillMaxWidth().padding(40.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            message,
            color = Ink.fgMuted,
            style = TextStyle(fontSize = 14.sp)
        )
    }
}
