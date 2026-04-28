package com.thc.safewords.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.thc.safewords.ui.theme.Ink

// Uppercase eyebrow label (matches iOS SectionLabel).
@Composable
fun SectionLabel(
    text: String,
    color: Color = Ink.fgMuted,
    modifier: Modifier = Modifier
) {
    Text(
        text = text.uppercase(),
        color = color,
        style = TextStyle(
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 1.4.sp
        ),
        modifier = modifier
    )
}

// Solid circle with a capped initial inside — matches iOS GroupDot.
@Composable
fun GroupDot(
    initial: String,
    color: Color,
    size: Dp = 36.dp,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.size(size).clip(CircleShape).background(color),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initial.take(1),
            color = Color.White,
            style = TextStyle(
                fontSize = (size.value * 0.42f).sp,
                fontWeight = FontWeight.SemiBold
            )
        )
    }
}

// Rounded ghost card wrapper used across screens.
@Composable
fun ElevatedCard(
    cornerRadius: Dp = 20.dp,
    modifier: Modifier = Modifier,
    borderColor: Color = Ink.rule,
    background: Color = Ink.bgElev,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(background)
            .border(0.5.dp, borderColor, RoundedCornerShape(cornerRadius))
    ) {
        content()
    }
}

// Display font style helper (serif for "Ink" theme feel).
fun displayStyle(sizeSp: Float, tracking: Float = -1.2f): TextStyle = TextStyle(
    fontSize = sizeSp.sp,
    fontWeight = FontWeight.Normal,
    letterSpacing = tracking.sp
)

fun monoStyle(sizeSp: Float, tracking: Float = 0.3f): TextStyle = TextStyle(
    fontSize = sizeSp.sp,
    fontWeight = FontWeight.Normal,
    letterSpacing = tracking.sp
)
