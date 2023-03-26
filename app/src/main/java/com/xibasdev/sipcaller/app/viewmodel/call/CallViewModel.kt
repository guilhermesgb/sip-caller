package com.xibasdev.sipcaller.app.viewmodel.call

import android.view.SurfaceView
import androidx.lifecycle.Lifecycle
import com.xibasdev.sipcaller.app.model.SipEngineClient
import com.xibasdev.sipcaller.app.viewmodel.common.BaseViewModel
import com.xibasdev.sipcaller.app.viewmodel.common.LifecycleAwareSurfaceManager
import com.xibasdev.sipcaller.app.viewmodel.events.calling.accept.AcceptCallInvitationFailed
import com.xibasdev.sipcaller.app.viewmodel.events.calling.accept.AcceptingCallInvitation
import com.xibasdev.sipcaller.app.viewmodel.events.calling.accept.CallInvitationAccepted
import com.xibasdev.sipcaller.app.viewmodel.events.calling.cancel.CallInvitationCanceled
import com.xibasdev.sipcaller.app.viewmodel.events.calling.cancel.CancelCallInvitationFailed
import com.xibasdev.sipcaller.app.viewmodel.events.calling.cancel.CancelingCallInvitation
import com.xibasdev.sipcaller.app.viewmodel.events.calling.decline.CallInvitationDeclined
import com.xibasdev.sipcaller.app.viewmodel.events.calling.decline.DeclineCallInvitationFailed
import com.xibasdev.sipcaller.app.viewmodel.events.calling.decline.DecliningCallInvitation
import com.xibasdev.sipcaller.app.viewmodel.events.calling.surfaces.local.LocalSurfaceUpdated
import com.xibasdev.sipcaller.app.viewmodel.events.calling.surfaces.local.UpdateLocalSurfaceFailed
import com.xibasdev.sipcaller.app.viewmodel.events.calling.surfaces.local.UpdatingLocalSurface
import com.xibasdev.sipcaller.app.viewmodel.events.calling.surfaces.remote.RemoteSurfaceUpdated
import com.xibasdev.sipcaller.app.viewmodel.events.calling.surfaces.remote.UpdateRemoteSurfaceFailed
import com.xibasdev.sipcaller.app.viewmodel.events.calling.surfaces.remote.UpdatingRemoteSurface
import com.xibasdev.sipcaller.app.viewmodel.events.calling.terminate.CallSessionTerminated
import com.xibasdev.sipcaller.app.viewmodel.events.calling.terminate.TerminateCallSessionFailed
import com.xibasdev.sipcaller.app.viewmodel.events.calling.terminate.TerminatingCallSession
import com.xibasdev.sipcaller.sip.calling.CallId
import com.xibasdev.sipcaller.sip.calling.details.CallInvitationUpdate
import com.xibasdev.sipcaller.sip.calling.details.CallSessionUpdate
import com.xibasdev.sipcaller.sip.calling.details.CallUpdate
import com.xibasdev.sipcaller.sip.calling.details.NoCallUpdateAvailable
import com.xibasdev.sipcaller.sip.identity.IdentityUpdate
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.rxjava3.core.Observable
import javax.inject.Inject

@HiltViewModel
class CallViewModel @Inject constructor(
    private val sipEngineClient: SipEngineClient,
    private val lifecycleAwareSurfaceManager: LifecycleAwareSurfaceManager
) : BaseViewModel() {

    fun observeCallInProgress(callId: CallId): Observable<Boolean> {
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
            .continuouslyPropagateResultAsEvent(
                UpdatingLocalSurface, LocalSurfaceUpdated
            ) { error ->

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
            .continuouslyPropagateResultAsEvent(
                UpdatingRemoteSurface, RemoteSurfaceUpdated
            ) { error ->

                UpdateRemoteSurfaceFailed(error)
            }

        return observeCallDetails(callId)
            .map { update ->

                when (update) {
                    is CallInvitationUpdate -> !update.call.status.isTerminal
                    is CallSessionUpdate -> !update.call.status.isTerminal
                    NoCallUpdateAvailable -> false
                }
            }
            .distinctUntilChanged()
    }

    fun observeCallDetails(callId: CallId): Observable<CallUpdate> {
        return sipEngineClient.observeCallDetails(callId)
    }

    fun observeIdentity(): Observable<IdentityUpdate> {
        return sipEngineClient.observeIdentity()
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

    fun cancelCallInvitation(callId: CallId) {
        sipEngineClient.cancelCallInvitation(callId)
            .propagateResultAsEvent(CancelingCallInvitation, CallInvitationCanceled) { error ->

                CancelCallInvitationFailed(error)
            }
    }

    fun acceptCallInvitation(callId: CallId) {
        sipEngineClient.acceptCallInvitation(callId)
            .propagateResultAsEvent(AcceptingCallInvitation, CallInvitationAccepted) { error ->

                AcceptCallInvitationFailed(error)
            }
    }

    fun declineCallInvitation(callId: CallId) {
        sipEngineClient.declineCallInvitation(callId)
            .propagateResultAsEvent(DecliningCallInvitation, CallInvitationDeclined) { error ->

                DeclineCallInvitationFailed(error)
            }
    }

    fun terminateCallSession(callId: CallId) {
        sipEngineClient.terminateCallSession(callId)
            .propagateResultAsEvent(TerminatingCallSession, CallSessionTerminated) { error ->

                TerminateCallSessionFailed(error)
            }
    }

    companion object {
        private const val SURFACE_CODE_LOCAL = "LOCAL"
        private const val SURFACE_CODE_REMOTE = "REMOTE"
    }
}
