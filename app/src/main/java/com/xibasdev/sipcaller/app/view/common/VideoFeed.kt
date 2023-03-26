package com.xibasdev.sipcaller.app.view.common

import android.view.SurfaceView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter

@Composable
fun VideoFeed(
    onUpdateSurfaceView: (surfaceView: SurfaceView) -> Unit,
    modifier: Modifier
) {

    var showSurface by remember { mutableStateOf(false) }

    BoxWithConstraints(
        contentAlignment = Alignment.Center,
        modifier = modifier.fillMaxSize()
    ) {
        AnimatedVisibility(
            visible = showSurface,
            enter = fadeIn(
                animationSpec = tween(
                    durationMillis = 50, delayMillis = 0
                )
            ),
            modifier = Modifier.fillMaxSize()
        ) {

            NativeSurface(onUpdateSurfaceView)
        }

        AnimatedVisibility(
            visible = !showSurface,
            exit = fadeOut(
                animationSpec = tween(
                    durationMillis = 800, delayMillis = 600
                )
            ),
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxSize()
        ) {

            Image(
                painter = ColorPainter(Color.Black),
                contentDescription = null,
                modifier = Modifier.fillMaxSize()
            )

            showSurface = true
        }
    }
}
