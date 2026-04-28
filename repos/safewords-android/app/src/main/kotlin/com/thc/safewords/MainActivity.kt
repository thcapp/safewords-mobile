package com.thc.safewords

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import com.thc.safewords.service.BiometricService
import com.thc.safewords.service.GroupRepository
import com.thc.safewords.ui.navigation.SafewordsNavigation
import com.thc.safewords.ui.theme.Ink
import com.thc.safewords.ui.theme.SafewordsTheme

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SafewordsTheme {
                BiometricGate(activity = this) {
                    SafewordsNavigation()
                }
            }
        }
    }
}

@Composable
private fun BiometricGate(activity: FragmentActivity, content: @Composable () -> Unit) {
    val required = remember { GroupRepository.isBiometricRequired() }
    if (!required) {
        content()
        return
    }

    var unlocked by remember { mutableStateOf(false) }
    var failed by remember { mutableStateOf(false) }

    if (unlocked) {
        content()
        return
    }

    Box(
        modifier = Modifier.fillMaxSize().background(Ink.bg),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(28.dp)
        ) {
            Box(
                modifier = Modifier
                    .padding(bottom = 18.dp)
                    .clip(CircleShape)
                    .background(Ink.tickFill)
                    .padding(24.dp)
            ) {
                Icon(Icons.Outlined.Lock, null, tint = Ink.accent, modifier = Modifier.size(36.dp))
            }
            Text(
                "Unlock Safewords",
                color = Ink.fg,
                style = TextStyle(fontSize = 30.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.7).sp)
            )
            Spacer(Modifier.height(8.dp))
            Text(
                if (failed) "Authentication failed. Tap unlock to try again."
                else "Use your fingerprint or face to open the app.",
                color = if (failed) Ink.warn else Ink.fgMuted,
                style = TextStyle(fontSize = 14.sp),
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(24.dp))
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Ink.accent)
                    .clickable {
                        BiometricService.authenticate(activity) { ok ->
                            if (ok) unlocked = true else failed = true
                        }
                    }
                    .padding(horizontal = 28.dp, vertical = 14.dp)
            ) {
                Text(
                    "Unlock",
                    color = Ink.accentInk,
                    style = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                )
            }
        }
    }

    LaunchedEffect(Unit) {
        BiometricService.authenticate(activity) { ok ->
            if (ok) unlocked = true else failed = true
        }
    }
}
