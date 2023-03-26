package com.xibasdev.sipcaller.app.view.call

import android.view.SurfaceView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.xibasdev.sipcaller.app.view.common.VideoFeed

@Composable
fun CallScreen(
    onUpdateLocalSurfaceView: (surfaceView: SurfaceView) -> Unit,
    onUpdateRemoteSurfaceView: (surfaceView: SurfaceView) -> Unit
) {

    Box(
        modifier = Modifier.fillMaxSize()
    ) {

        VideoFeed(
            onUpdateSurfaceView = onUpdateRemoteSurfaceView,
            modifier = Modifier.fillMaxSize()
        )

        VideoFeed(
            onUpdateSurfaceView = onUpdateLocalSurfaceView,
            modifier = Modifier.fillMaxSize(0.3f)
                .align(Alignment.CenterEnd)
        )
    }
}
