package com.example.myapplication

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape

@Composable
fun Header(score: Int, bestScore: Int, onRestart: () -> Unit, onHint: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Box(
            modifier = Modifier.size(110.dp)
                .background(Color(0xFFEDC403), RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text("2048", fontSize = 38.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                ScoreBox("SCORE", score)
                Spacer(modifier = Modifier.height(6.dp))
                ActionButton("RESTART", onRestart)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                ScoreBox("BEST", bestScore)
                Spacer(modifier = Modifier.height(6.dp))
                ActionButton("HINT", onHint)
            }
        }
    }
}

@Composable
fun ActionButton(text: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier.clickable { onClick() }
            .background(Color(0xFFF67C5F), RoundedCornerShape(4.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(text = text, fontSize = 14.sp, color = Color.White)
    }
}

@Composable
fun ScoreBox(label: String, score: Int) {
    Column(
        modifier = Modifier.width(80.dp).background(Color(0xFFBBADA0), RoundedCornerShape(4.dp)).padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(label, fontSize = 12.sp, color = Color.White)
        Text("$score", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
    }
}