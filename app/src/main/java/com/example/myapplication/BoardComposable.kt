package com.example.myapplication

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.shape.RoundedCornerShape

@Composable
fun Board(board: List<List<Int>>) {
    val cellSize = 70.dp
    val spacing = 8.dp

    Column(
        modifier = Modifier.background(Color(0xFFBBADA0), RoundedCornerShape(8.dp)).padding(spacing),
        verticalArrangement = Arrangement.spacedBy(spacing)
    ) {
        for (row in board) {
            Row(horizontalArrangement = Arrangement.spacedBy(spacing)) {
                for (tile in row) {
                    TileView(tile, cellSize)
                }
            }
        }
    }
}

@Composable
fun TileView(value: Int, size: Dp) {
    val tileColor = if (value == 0) Color(0xFFCDC1B4) else getTileColor(value)
    Box(
        modifier = Modifier.size(size).background(tileColor, RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center
    ) {
        if (value != 0) {
            Text(text = "$value", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.DarkGray)
        }
    }
}

fun getTileColor(value: Int): Color = when (value) {
    2 -> Color(0xFFEEE4DA)
    4 -> Color(0xFFEDE0C8)
    8 -> Color(0xFFF2B179)
    16 -> Color(0xFFF59563)
    32 -> Color(0xFFF67C5F)
    64 -> Color(0xFFF65E3B)
    128 -> Color(0xFFEDCF72)
    256 -> Color(0xFFEDCC61)
    512 -> Color(0xFFEDC850)
    1024 -> Color(0xFFEDC53F)
    2048 -> Color(0xFFEDC22E)
    else -> Color(0xFF3C3A32)
}