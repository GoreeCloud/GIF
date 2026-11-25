package com.goreecloud.gifs.ui

import android.animation.ValueAnimator
import androidx.compose.runtime.Composable
import com.goreecloud.ui.material3.GoreeCloudExpressiveTheme

/**
 * Product-level wrapper around the canonical GC UI Material 3 Expressive theme.
 * The existing app keeps its stable call site, data flows, and on-device storage.
 * The Android system's disabled animator setting falls back to standard motion.
 */
@Composable
fun GoreeCloudTheme(content: @Composable () -> Unit) {
    GoreeCloudExpressiveTheme(
        dynamicWallpaperColors = false,
        reduceMotion = !ValueAnimator.areAnimatorsEnabled(),
        content = content
    )
}
