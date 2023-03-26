package com.xibasdev.sipcaller.app.viewmodel.call

import android.view.SurfaceView
import androidx.lifecycle.Lifecycle
import com.xibasdev.sipcaller.app.viewmodel.common.BaseViewModel
import com.xibasdev.sipcaller.app.viewmodel.common.LifecycleAwareSurfaceManager
import com.xibasdev.sipcaller.app.viewmodel.profile.events.LocalSurfaceUpdated
import com.xibasdev.sipcaller.app.viewmodel.profile.events.RemoteSurfaceUpdated
import com.xibasdev.sipcaller.app.viewmodel.profile.events.UpdateLocalSurfaceFailed
import com.xibasdev.sipcaller.app.viewmodel.profile.events.UpdateRemoteSurfaceFailed
import com.xibasdev.sipcaller.app.model.SipEngineClient
import com.xibasdev.sipcaller.sip.calling.CallId
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class CallViewModel @Inject constructor(
    private val sipEngineClient: SipEngineClient,
    private val lifecycleAwareSurfaceManager: LifecycleAwareSurfaceManager
) : BaseViewModel() {

    /**
     * TODO get call ID as input argument
     */
    private val callId: CallId = CallId("")

    init {
        lifecycleAwareSurfaceManager
            .manageSurfaceUpdates(
                surfaceCode = SURFACE_CODE_LOCAL,
                onUseSurface = { surface ->

                    sipEngineClient.setLocalCameraFeedSurface(callId, surface)
                },
                onFreeSurface = {
                    sipEngineClient.unsetLocalCameraFeedSurface(callId)
                }
            )
            .continuouslyPropagateResultAsEvent(LocalSurfaceUpdated) { error ->

                UpdateLocalSurfaceFailed(error)
            }

        lifecycleAwareSurfaceManager
            .manageSurfaceUpdates(
                surfaceCode = SURFACE_CODE_REMOTE,
                onUseSurface = { surface ->

                    sipEngineClient.setRemoteVideoFeedSurface(callId, surface)
                },
                onFreeSurface = {
                    sipEngineClient.unsetRemoteVideoFeedSurface(callId)
                }
            )
            .continuouslyPropagateResultAsEvent(RemoteSurfaceUpdated) { error ->

                UpdateRemoteSurfaceFailed(error)
            }
    }

    fun onUpdateLifecycle(lifecycle: Lifecycle) {
        lifecycleAwareSurfaceManager.onUpdateLifecycle(lifecycle)
    }

    fun onUpdateLocalSurfaceView(surfaceView: SurfaceView) {
        lifecycleAwareSurfaceManager.onUpdateSurfaceView(SURFACE_CODE_LOCAL, surfaceView)
    }

    fun onUpdateRemoteSurfaceView(surfaceView: SurfaceView) {
        lifecycleAwareSurfaceManager.onUpdateSurfaceView(SURFACE_CODE_REMOTE, surfaceView)
    }

    companion object {
        private const val SURFACE_CODE_LOCAL = "LOCAL"
        private const val SURFACE_CODE_REMOTE = "REMOTE"
    }
}
