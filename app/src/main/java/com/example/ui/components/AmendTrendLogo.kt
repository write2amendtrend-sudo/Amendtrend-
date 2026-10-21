package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.*

@Composable
fun AmendTrendLogo(
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    showTagline: Boolean = true,
    horizontal: Boolean = true
) {
    if (horizontal) {
        Row(
            modifier = modifier,
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            LogoIcon(size = size)
            Column {
                Text(
                    text = "AMEND TREND",
                    color = PrimaryText,
                    fontSize = (size.value * 0.38f).sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp
                )
                if (showTagline) {
                    Text(
                        text = "STYLE SHIFT",
                        color = MintGreen,
                        fontSize = (size.value * 0.22f).sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 2.5.sp
                    )
                }
            }
        }
    } else {
        Column(
            modifier = modifier,
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            LogoIcon(size = size)
            Text(
                text = "AMEND TREND",
                color = PrimaryText,
                fontSize = (size.value * 0.28f).sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
                textAlign = TextAlign.Center
            )
            if (showTagline) {
                Text(
                    text = "STYLE SHIFT",
                    color = MintGreen,
                    fontSize = (size.value * 0.16f).sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 3.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun LogoIcon(
    size: Dp,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(DarkBlueGray),
        contentAlignment = Alignment.Center
    ) {
        // High-fidelity fallback / vector globe with upward mint green trend arrow
        Canvas(modifier = Modifier.size(size * 0.85f)) {
            val center = Offset(this.size.width / 2f, this.size.height / 2f)
            val radius = this.size.minDimension / 2f - 4f

            // Outer Globe
            drawCircle(
                color = SecondaryText.copy(alpha = 0.5f),
                radius = radius,
                center = center,
                style = Stroke(width = 2.5f)
            )

            // Latitude lines
            drawOval(
                color = SecondaryText.copy(alpha = 0.35f),
                topLeft = Offset(center.x - radius, center.y - radius * 0.45f),
                size = androidx.compose.ui.geometry.Size(radius * 2f, radius * 0.9f),
                style = Stroke(width = 1.8f)
            )

            // Longitude line
            drawOval(
                color = SecondaryText.copy(alpha = 0.35f),
                topLeft = Offset(center.x - radius * 0.45f, center.y - radius),
                size = androidx.compose.ui.geometry.Size(radius * 0.9f, radius * 2f),
                style = Stroke(width = 1.8f)
            )

            // Mint Green Ascending Trend Arrow
            val arrowPath = Path().apply {
                val startX = center.x - radius * 0.75f
                val startY = center.y + radius * 0.35f
                val dipX = center.x - radius * 0.25f
                val dipY = center.y - radius * 0.1f
                val pullX = center.x + radius * 0.15f
                val pullY = center.y + radius * 0.05f
                val endX = center.x + radius * 0.8f
                val endY = center.y - radius * 0.65f

                moveTo(startX, startY)
                lineTo(dipX, dipY)
                lineTo(pullX, pullY)
                lineTo(endX, endY)
            }

            drawPath(
                path = arrowPath,
                color = MintGreen,
                style = Stroke(width = 4.5f, cap = StrokeCap.Round)
            )

            // Arrow head
            val endX = center.x + radius * 0.8f
            val endY = center.y - radius * 0.65f
            val headPath = Path().apply {
                moveTo(endX - 12f, endY + 2f)
                lineTo(endX, endY)
                lineTo(endX - 2f, endY + 12f)
            }
            drawPath(
                path = headPath,
                color = MintGreen,
                style = Stroke(width = 4.5f, cap = StrokeCap.Round)
            )
        }
    }
}
