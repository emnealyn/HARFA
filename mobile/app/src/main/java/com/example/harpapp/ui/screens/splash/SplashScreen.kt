package com.example.harpapp.ui.screens.splash

import com.example.harpapp.R
import androidx.compose.animation.graphics.res.animatedVectorResource
import androidx.compose.animation.graphics.res.rememberAnimatedVectorPainter
import androidx.compose.animation.graphics.vector.AnimatedImageVector
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.Image
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onAnimationFinished: () -> Unit) {
    val image = AnimatedImageVector.animatedVectorResource(R.drawable.harp)
    var atEnd by remember { mutableStateOf(false) }
    val painter = rememberAnimatedVectorPainter(image, atEnd)

    LaunchedEffect(Unit) {
        atEnd = true

        delay(3000)
        onAnimationFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.primary),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painter,
            contentDescription = "Laser Harp Splash",
            modifier = Modifier.fillMaxSize(0.8f)
        )
    }
}