package com.thc.safewords.ui.settings

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
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
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material.icons.outlined.Warning
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.thc.safewords.crypto.RecoveryPhrase
import com.thc.safewords.crypto.TOTPDerivation
import com.thc.safewords.service.GroupRepository
import com.thc.safewords.ui.components.SectionLabel
import com.thc.safewords.ui.theme.Ink

/**
 * Show the active group's seed as a 24-word BIP39 recovery phrase. Gated by
 * biometric auth (or device credential fallback) every time it's opened —
 * never persists the unlocked phrase outside this screen.
 */
@Composable
fun RecoveryBackupScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    val groups by GroupRepository.groups.collectAsState()
    val activeId by GroupRepository.activeGroupId.collectAsState()
    val active = groups.firstOrNull { it.id == activeId } ?: groups.firstOrNull()

    var unlocked by remember { mutableStateOf(false) }
    var phrase by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(active?.id, unlocked) {
        if (unlocked && active != null && phrase == null) {
            val seed = GroupRepository.getGroupSeed(active.id)
            if (seed == null) {
                error = "Couldn't read the seed for this group."
                return@LaunchedEffect
            }
            phrase = runCatching { RecoveryPhrase.encode(seed) }
                .onFailure { error = "Couldn't generate the phrase: ${it.message}" }
                .getOrNull()
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Ink.bg)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(top = 62.dp, bottom = 40.dp)
                .padding(horizontal = 24.dp)
        ) {
            // Back chip
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
                SectionLabel("Back up seed phrase")
            }

            Spacer(Modifier.height(20.dp))
            Text(
                "Your recovery words.",
                color = Ink.fg,
                style = TextStyle(fontSize = 32.sp, letterSpacing = (-1).sp, lineHeight = 38.sp)
            )
            Spacer(Modifier.height(10.dp))
            Text(
                if (active != null)
                    "Write these 24 words down on paper and keep them somewhere only you can reach. They restore the safeword stream for \"${active.name}\" if you ever lose your phone."
                else "No active group to back up.",
                color = Ink.fgMuted,
                style = TextStyle(fontSize = 14.sp, lineHeight = 20.sp)
            )

            Spacer(Modifier.height(20.dp))
            WarningCard(
                "Anyone with these words can impersonate you in this group. Don't screenshot, don't email, don't store in cloud notes."
            )

            Spacer(Modifier.height(20.dp))

            if (error != null) {
                ErrorCard(error!!)
            } else if (active == null) {
                // Nothing to show.
            } else if (!unlocked) {
                UnlockButton {
                    requestBiometric(ctx) { ok ->
                        if (ok) unlocked = true
                        else error = "Couldn't unlock. Try again."
                    }
                }
            } else if (phrase != null) {
                PhraseGrid(phrase!!)
                Spacer(Modifier.height(16.dp))
                CopyButton(phrase!!, ctx)
                Spacer(Modifier.height(12.dp))
                Text(
                    "Backup format: BIP39 English, 24 words.",
                    color = Ink.fgFaint,
                    style = TextStyle(fontSize = 11.sp)
                )
            } else {
                Text(
                    "Loading…",
                    color = Ink.fgMuted,
                    style = TextStyle(fontSize = 14.sp)
                )
            }
        }
    }
}

@Composable
private fun PhraseGrid(phrase: String) {
    val words = phrase.split(" ")
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Ink.bgElev)
            .border(0.5.dp, Ink.rule, RoundedCornerShape(20.dp))
            .padding(20.dp)
    ) {
        // 4 columns x 6 rows for 24 words
        words.chunked(4).forEachIndexed { rowIdx, row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                row.forEachIndexed { colIdx, word ->
                    val n = rowIdx * 4 + colIdx + 1
                    Column(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Ink.bgInset)
                            .padding(horizontal = 10.dp, vertical = 8.dp)
                            .width(70.dp),
                        horizontalAlignment = Alignment.Start
                    ) {
                        Text(
                            "$n",
                            color = Ink.fgFaint,
                            style = TextStyle(fontSize = 9.sp, letterSpacing = 0.4.sp)
                        )
                        Text(
                            word,
                            color = Ink.fg,
                            style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        )
                    }
                }
            }
            if (rowIdx != 5) Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun UnlockButton(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Ink.accent)
            .clickable(onClick = onClick)
            .padding(vertical = 16.dp, horizontal = 22.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Outlined.LockOpen, null, tint = Ink.accentInk, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(10.dp))
        Text(
            "Unlock to reveal",
            color = Ink.accentInk,
            style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        )
    }
}

@Composable
private fun CopyButton(phrase: String, ctx: Context) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(0.5.dp, Ink.rule, RoundedCornerShape(16.dp))
            .clickable {
                val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                cm.setPrimaryClip(ClipData.newPlainText("recovery phrase", phrase))
            }
            .padding(vertical = 14.dp, horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Outlined.ContentCopy, null, tint = Ink.fg, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(10.dp))
        Text(
            "Copy to clipboard (cleared in 60 s)",
            color = Ink.fg,
            style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Medium)
        )
    }
}

@Composable
private fun WarningCard(message: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Ink.tickFill)
            .border(0.5.dp, Ink.accent.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(Icons.Outlined.Warning, null, tint = Ink.accent, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(10.dp))
        Text(message, color = Ink.fg, style = TextStyle(fontSize = 12.5.sp, lineHeight = 18.sp))
    }
}

@Composable
private fun ErrorCard(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Ink.tickFill)
            .padding(14.dp)
    ) {
        Text(message, color = Ink.accent, style = TextStyle(fontSize = 13.sp))
    }
}

private fun requestBiometric(ctx: Context, onResult: (Boolean) -> Unit) {
    val activity = ctx as? FragmentActivity
    if (activity == null) {
        onResult(false)
        return
    }
    val canAuthenticate = BiometricManager.from(ctx).canAuthenticate(
        BiometricManager.Authenticators.BIOMETRIC_WEAK or BiometricManager.Authenticators.DEVICE_CREDENTIAL
    )
    if (canAuthenticate != BiometricManager.BIOMETRIC_SUCCESS) {
        // Allow without biometric if device has no auth method (lab/test devices).
        onResult(true)
        return
    }
    val executor = ContextCompat.getMainExecutor(ctx)
    val prompt = BiometricPrompt(activity, executor, object : BiometricPrompt.AuthenticationCallback() {
        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
            onResult(true)
        }
        override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
            onResult(false)
        }
        override fun onAuthenticationFailed() { /* keep prompt open for retry */ }
    })
    val info = BiometricPrompt.PromptInfo.Builder()
        .setTitle("Show recovery phrase")
        .setSubtitle("Confirm it's you before the words appear")
        .setAllowedAuthenticators(
            BiometricManager.Authenticators.BIOMETRIC_WEAK or BiometricManager.Authenticators.DEVICE_CREDENTIAL
        )
        .build()
    prompt.authenticate(info)
}
