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
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import com.thc.safewords.service.BiometricService
import com.thc.safewords.service.GroupRepository
import com.thc.safewords.ui.theme.Ink

/**
 * Biometric-gated reveal of a group's static override word. Word is computed
 * lazily from the seed and shown only after biometric/credential auth.
 */
@Composable
fun OverrideRevealScreen(groupId: String, onBack: () -> Unit) {
    val context = LocalContext.current
    val activity = context as? FragmentActivity
    var unlocked by remember { mutableStateOf(false) }
    var word by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(groupId) {
        if (activity == null) {
            error = "Cannot prompt for biometric auth (no activity)."
            return@LaunchedEffect
        }
        if (!BiometricService.canAuthenticate(activity)) {
            // No usable biometric — set up device lock first.
            error = "Set a screen lock or biometric to view this."
            return@LaunchedEffect
        }
        BiometricService.authenticate(
            activity = activity,
            title = "Reveal override",
            subtitle = "Confirm it's you to view the override word.",
        ) { success ->
            if (success) {
                word = GroupRepository.getStaticOverride(groupId)
                unlocked = true
            } else {
                error = "Authentication cancelled."
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Ink.bg)) {
        Column(modifier = Modifier.fillMaxSize().padding(top = 62.dp)) {
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
                    "Override word",
                    color = Ink.fg,
                    style = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.4).sp)
                )
            }

            Column(
                modifier = Modifier.fillMaxWidth().padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(40.dp))
                if (unlocked && word != null) {
                    Text(
                        "Anyone with this word can act as you in this group.",
                        color = Ink.fgMuted,
                        textAlign = TextAlign.Center,
                        style = TextStyle(fontSize = 13.sp, lineHeight = 18.sp)
                    )
                    Spacer(Modifier.height(40.dp))
                    word!!.split(' ').forEach { w ->
                        Text(
                            w,
                            color = Ink.fg,
                            style = TextStyle(fontSize = 38.sp, lineHeight = 44.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.6).sp)
                        )
                    }
                    Spacer(Modifier.height(40.dp))
                    Text(
                        "This word is fixed by your group's seed. Rotate the seed to replace it; printing it is recoverable from the recovery phrase, so don't lose either.",
                        color = Ink.fgMuted,
                        textAlign = TextAlign.Center,
                        style = TextStyle(fontSize = 12.sp, lineHeight = 17.sp)
                    )
                } else if (error != null) {
                    Text(error!!, color = Ink.fg, textAlign = TextAlign.Center)
                } else {
                    Text(
                        "Authenticating…",
                        color = Ink.fgMuted,
                        textAlign = TextAlign.Center,
                        style = TextStyle(fontSize = 14.sp)
                    )
                }
            }
        }
    }
}
