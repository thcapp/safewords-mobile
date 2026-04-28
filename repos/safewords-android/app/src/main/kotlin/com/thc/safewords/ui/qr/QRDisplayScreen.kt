package com.thc.safewords.ui.qr

import android.graphics.Bitmap
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Message
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import com.thc.safewords.service.GroupRepository
import com.thc.safewords.service.QRCodeService
import com.thc.safewords.service.SmsInviteService
import com.thc.safewords.ui.components.SectionLabel
import com.thc.safewords.ui.theme.Ink

@Composable
fun QRDisplayScreen(groupId: String, onDismiss: () -> Unit) {
    val ctx = LocalContext.current
    val groups by GroupRepository.groups.collectAsState()
    val group = groups.firstOrNull { it.id == groupId }
    var qrBitmap by remember { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(groupId) {
        if (group != null) qrBitmap = QRCodeService.generateQRBitmap(group, 512)
    }

    Box(modifier = Modifier.fillMaxSize().background(Ink.bg)) {
        Column(modifier = Modifier.fillMaxSize().padding(top = 62.dp)) {
            // Header with back button
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Ink.bgElev)
                        .border(0.5.dp, Ink.rule, CircleShape)
                        .clickable { onDismiss() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Outlined.ArrowBack, null, tint = Ink.fg, modifier = Modifier.size(14.dp))
                }
                Spacer(Modifier.width(10.dp))
                Column {
                    SectionLabel("Invite · ${group?.name ?: ""}")
                    Spacer(Modifier.height(2.dp))
                    Text(
                        "Share in person",
                        color = Ink.fg,
                        style = TextStyle(fontSize = 22.sp, letterSpacing = (-0.6).sp)
                    )
                }
            }

            Spacer(Modifier.weight(1f))

            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(28.dp))
                        .background(Ink.bgElev)
                        .border(0.5.dp, Ink.rule, RoundedCornerShape(28.dp))
                        .padding(20.dp)
                ) {
                    qrBitmap?.let { bmp ->
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = "QR code",
                            modifier = Modifier.size(240.dp)
                        )
                    } ?: Box(modifier = Modifier.size(240.dp).background(Ink.bgInset))
                }

                Spacer(Modifier.height(20.dp))

                val tipText = buildAnnotatedString {
                    append("Have them open Safewords, tap ")
                    withStyle(SpanStyle(color = Ink.accent)) { append("Join with QR") }
                    append(", and scan this.")
                }
                Text(
                    tipText,
                    color = Ink.fg,
                    style = TextStyle(fontSize = 14.5.sp, fontWeight = FontWeight.Medium),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.widthIn(max = 280.dp)
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    "Only share this in person. Anyone who scans it joins your group permanently.",
                    color = Ink.fgMuted,
                    style = TextStyle(fontSize = 12.5.sp, lineHeight = 18.sp),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.widthIn(max = 280.dp)
                )

                Spacer(Modifier.height(24.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Lock, null, tint = Ink.fgFaint, modifier = Modifier.size(11.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "256-BIT · ROTATING · OFFLINE",
                        color = Ink.fgFaint,
                        style = TextStyle(fontSize = 11.sp, letterSpacing = 1.sp)
                    )
                }
            }

            Spacer(Modifier.weight(1f))

            // SMS alternative
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 120.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Ink.bgElev)
                    .border(0.5.dp, Ink.rule, RoundedCornerShape(14.dp))
                    .clickable(enabled = group != null) {
                        group?.let { SmsInviteService.shareViaSms(ctx, it) }
                    }
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Outlined.Message, null, tint = Ink.fgMuted, modifier = Modifier.size(17.dp))
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("Invite via SMS instead", color = Ink.fg, style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium))
                    Spacer(Modifier.height(1.dp))
                    Text(
                        "For family without the app — they get the rotating word by text.",
                        color = Ink.fgMuted,
                        style = TextStyle(fontSize = 11.5.sp, lineHeight = 15.sp)
                    )
                }
                Icon(Icons.Outlined.ArrowForward, null, tint = Ink.fgFaint, modifier = Modifier.size(13.dp))
            }
        }
    }
}
