package com.example.harpapp.presentation.splash

import android.app.Activity
import android.os.SystemClock
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen

private const val MIN_SPLASH_DURATION_MS = 2000L
private const val SPLASH_EXIT_FADE_MS = 200L

fun Activity.installHarpSplash(isFreshStart: Boolean, isLoading: () -> Boolean) {
    val minVisibleUntil = if (isFreshStart) {
        SystemClock.uptimeMillis() + MIN_SPLASH_DURATION_MS
    } else {
        0L
    }
    val splashScreen = installSplashScreen()
    splashScreen.setKeepOnScreenCondition {
        isLoading() || SystemClock.uptimeMillis() < minVisibleUntil
    }
    splashScreen.setOnExitAnimationListener { provider ->
        provider.view.animate()
            .alpha(0f)
            .setDuration(SPLASH_EXIT_FADE_MS)
            .withEndAction { provider.remove() }
            .start()
    }
}
