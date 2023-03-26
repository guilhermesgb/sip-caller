package com.xibasdev.sipcaller.app.viewmodel.common

import android.view.SurfaceHolder
import com.xibasdev.sipcaller.app.view.common.SurfaceUpdate
import io.reactivex.rxjava3.subjects.BehaviorSubject

class SurfaceHolderCallback(
    private val surfaceUpdates: BehaviorSubject<SurfaceUpdate>
) : SurfaceHolder.Callback {

    override fun surfaceCreated(surfaceHolder: SurfaceHolder) {}

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        surfaceUpdates.onNext(SurfaceUpdate(holder.surface, width, height))
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        surfaceUpdates.onNext(SurfaceUpdate(null))
    }
}
