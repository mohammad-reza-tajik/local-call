package com.localcall.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Palette = listOf(
    Color(0xFF2DD4BF) to Color(0xFF0EA5E9),
    Color(0xFFA78BFA) to Color(0xFF6366F1),
    Color(0xFFFB923C) to Color(0xFFEF4444),
    Color(0xFF4ADE80) to Color(0xFF14B8A6),
    Color(0xFFF472B6) to Color(0xFFA855F7),
)

/** Initial-letter avatar whose gradient is picked from the name, so each peer looks distinct. */
@Composable
fun Avatar(name: String, size: Dp = 48.dp, textSize: TextUnit = 20.sp) {
    val (from, to) = Palette[Math.floorMod(name.hashCode(), Palette.size)]
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(Brush.linearGradient(listOf(from, to))),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = name.trim().firstOrNull()?.uppercase() ?: "?",
            color = Color.White,
            fontSize = textSize,
            fontWeight = FontWeight.SemiBold,
            style = MaterialTheme.typography.titleLarge,
        )
    }
}
