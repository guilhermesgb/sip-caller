package com.xibasdev.sipcaller.app.viewmodel.common

import android.util.SparseArray
import android.view.Surface
import android.view.SurfaceView
import android.view.View.GONE
import android.view.View.VISIBLE
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import com.xibasdev.sipcaller.app.view.common.SurfaceUpdate
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.subjects.BehaviorSubject
import java.lang.ref.WeakReference
import javax.inject.Inject

class LifecycleAwareSurfaceManager @Inject constructor() {

    private val lifecycleUpdates = BehaviorSubject.create<Boolean>()

    private val lifecycleObserver = object : DefaultLifecycleObserver {
        override fun onStart(owner: LifecycleOwner) {
            lifecycleUpdates.onNext(true)
        }

        override fun onStop(owner: LifecycleOwner) {
            lifecycleUpdates.onNext(false)
        }
    }

    private var lifecycleInUse: WeakReference<Lifecycle>? = null

    private data class SurfaceStorageEntry(
        val surfaceUpdates: BehaviorSubject<SurfaceUpdate>,
        val surfaceHolderCallback: SurfaceHolderCallback,
        val surfaceViewInUse: WeakReference<SurfaceView>
    )

    private val surfaceStorage = SparseArray<SurfaceStorageEntry>()

    fun manageSurfaceUpdates(
        surfaceCode: String,
        onUseSurface: (Surface) -> Completable,
        onFreeSurface: () -> Completable
    ): Completable {

        val surfaceUpdates = BehaviorSubject.create<SurfaceUpdate>()

        surfaceStorage.put(
            surfaceCode.hashCode(),
            SurfaceStorageEntry(
                surfaceUpdates = surfaceUpdates,
                surfaceHolderCallback = SurfaceHolderCallback(surfaceUpdates),
                surfaceViewInUse = WeakReference(null)
            )
        )

        return processSurfaceUpdates(
            surfaceUpdates = surfaceUpdates,
            onUseSurface = onUseSurface,
            onFreeSurface = onFreeSurface
        )
    }

    fun onUpdateLifecycle(lifecycle: Lifecycle) {
        val lifecyclePreviouslyInUse = lifecycleInUse?.get()

        if (lifecycle != lifecyclePreviouslyInUse) {
            lifecyclePreviouslyInUse?.removeObserver(lifecycleObserver)

            lifecycle.addObserver(lifecycleObserver)

            lifecycleInUse?.clear()
            lifecycleInUse = WeakReference(lifecycle)
        }
    }

    fun onUpdateSurfaceView(surfaceCode: String, surfaceView: SurfaceView) {
        val surfaceIndex = surfaceCode.hashCode()

        surfaceStorage[surfaceIndex] = surfaceStorage[surfaceIndex].copy(
            surfaceViewInUse = onUpdateSurfaceView(
                newSurfaceView = surfaceView,
                oldSurfaceView = surfaceStorage[surfaceIndex].surfaceViewInUse,
                surfaceHolderCallback = surfaceStorage[surfaceIndex].surfaceHolderCallback
            )
        )
    }

    fun onSurfaceViewDestroyed(surfaceCode: String) {
        surfaceStorage[surfaceCode.hashCode()].surfaceUpdates.onNext(SurfaceUpdate(null))
    }

    private fun processSurfaceUpdates(
        surfaceUpdates: BehaviorSubject<SurfaceUpdate>,
        onUseSurface: (Surface) -> Completable,
        onFreeSurface: () -> Completable
    ): Completable {

        return Observable
            .combineLatest(
                lifecycleUpdates,
                surfaceUpdates
            ) { screenIsStarted, surfaceUpdate ->

                if (screenIsStarted) {
                    surfaceUpdate

                } else {
                    surfaceUpdate.copy(surface = null)
                }
            }
            .switchMapCompletable { update ->

                val surface = update.surface

                if (surface == null || !surface.isValid) {
                    onFreeSurface()

                } else {
                    onUseSurface(surface)
                }
            }
    }

    private fun onUpdateSurfaceView(
        newSurfaceView: SurfaceView,
        oldSurfaceView: WeakReference<SurfaceView>?,
        surfaceHolderCallback: SurfaceHolderCallback
    ): WeakReference<SurfaceView> {

        val surfaceViewPreviouslyInUse = oldSurfaceView?.get()

        if (newSurfaceView != surfaceViewPreviouslyInUse) {
            surfaceViewPreviouslyInUse?.let {
                it.holder.removeCallback(surfaceHolderCallback)
                it.visibility = GONE
            }

            newSurfaceView.apply {
                holder.addCallback(surfaceHolderCallback)
                visibility = VISIBLE
            }

            oldSurfaceView?.clear()
            return WeakReference(newSurfaceView)
        }

        return oldSurfaceView
    }
}
