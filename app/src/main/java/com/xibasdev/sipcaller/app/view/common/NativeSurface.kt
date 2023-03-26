package com.xibasdev.sipcaller.app.view.common

import android.view.SurfaceView
import androidx.compose.runtime.Composable
import androidx.compose.ui.viewinterop.AndroidView

@Composable
fun NativeSurface(onUpdateSurfaceView: (surfaceView: SurfaceView) -> Unit) {

    AndroidView(
        factory = { context ->

            SurfaceView(context)
        },
        update = { surfaceView ->

            onUpdateSurfaceView(surfaceView)
        }
    )
}
