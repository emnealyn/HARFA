package com.example.harpapp.ui.components.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.harpapp.R
import com.example.harpapp.model.Difficulty

@Composable
fun DifficultyBadge(
    difficulty: Difficulty,
    modifier: Modifier = Modifier
) {
    val (difficultyText, difficultyColor) = when (difficulty) {
        Difficulty.EASY -> stringResource(R.string.difficulty_easy) to Color(0xFF4CAF50)
        Difficulty.MEDIUM -> stringResource(R.string.difficulty_medium) to Color(0xFFFFA726)
        Difficulty.HARD -> stringResource(R.string.difficulty_hard) to Color(0xFFEF5350)
    }

    Box(
        modifier = modifier
            .wrapContentWidth()
            .height(26.dp)
            .background(
                color = difficultyColor.copy(alpha = 0.15f),
                shape = RoundedCornerShape(12.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = difficultyText,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = difficultyColor,
            modifier = Modifier.padding(horizontal = 12.dp)
        )
    }
}