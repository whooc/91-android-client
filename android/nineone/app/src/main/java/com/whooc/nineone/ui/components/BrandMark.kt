package com.whooc.nineone.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.whooc.nineone.data.Brand

/**
 * The app's name, or its logo once one has been picked in 设置.
 *
 * Reads [Brand] directly instead of taking the name and logo as parameters on
 * purpose: both are Compose state, so every screen that draws the mark repaints
 * the instant the setting changes — no restart, no manual invalidation.
 *
 * The name is rendered as text rather than forced into a square, because a
 * custom name is usually wider than it is tall and letterboxing it would look
 * wrong. Only a real logo gets a fixed box.
 */
@Composable
fun BrandMark(
    modifier: Modifier = Modifier,
    logoSize: Dp = 72.dp,
    fontSize: TextUnit = 44.sp,
    letterSpacing: TextUnit = 4.sp,
    color: Color = Color.Unspecified,
    maxLines: Int = 1,
    cornerRadius: Dp = logoSize / 4
) {
    val logo = Brand.logo
    if (logo != null) {
        AsyncImage(
            model = logo,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = modifier
                .size(logoSize)
                .clip(RoundedCornerShape(cornerRadius))
        )
    } else {
        Text(
            text = Brand.name,
            modifier = modifier,
            fontSize = fontSize,
            fontWeight = FontWeight.Bold,
            color = color,
            letterSpacing = letterSpacing,
            // A custom name can be any length; the slots that draw this are
            // narrow, so it gets clipped rather than reflowing the layout.
            maxLines = maxLines,
            overflow = TextOverflow.Ellipsis
        )
    }
}
