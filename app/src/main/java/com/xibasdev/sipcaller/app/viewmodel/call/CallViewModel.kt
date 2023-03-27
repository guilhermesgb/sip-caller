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
import com.xibasdev.sipcaller.app.viewmodel.events.calling.surfaces.local.destroy.DestroyLocalSurfaceFailed
import com.xibasdev.sipcaller.app.viewmodel.events.calling.surfaces.local.destroy.DestroyingLocalSurface
import com.xibasdev.sipcaller.app.viewmodel.events.calling.surfaces.local.destroy.LocalSurfaceDestroyed
import com.xibasdev.sipcaller.app.viewmodel.events.calling.surfaces.local.update.LocalSurfaceUpdated
import com.xibasdev.sipcaller.app.viewmodel.events.calling.surfaces.local.update.UpdateLocalSurfaceFailed
import com.xibasdev.sipcaller.app.viewmodel.events.calling.surfaces.local.update.UpdatingLocalSurface
import com.xibasdev.sipcaller.app.viewmodel.events.calling.surfaces.remote.destroy.DestroyRemoteSurfaceFailed
import com.xibasdev.sipcaller.app.viewmodel.events.calling.surfaces.remote.destroy.DestroyingRemoteSurface
import com.xibasdev.sipcaller.app.viewmodel.events.calling.surfaces.remote.destroy.RemoteSurfaceDestroyed
import com.xibasdev.sipcaller.app.viewmodel.events.calling.surfaces.remote.update.RemoteSurfaceUpdated
import com.xibasdev.sipcaller.app.viewmodel.events.calling.surfaces.remote.update.UpdateRemoteSurfaceFailed
import com.xibasdev.sipcaller.app.viewmodel.events.calling.surfaces.remote.update.UpdatingRemoteSurface
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
import io.reactivex.rxjava3.kotlin.addTo
import javax.inject.Inject

@HiltViewModel
class CallViewModel @Inject constructor(
    private val sipEngineClient: SipEngineClient,
    private val lifecycleAwareSurfaceManager: LifecycleAwareSurfaceManager
) : BaseViewModel() {

    fun onUpdateLifecycle(lifecycle: Lifecycle) {
        lifecycleAwareSurfaceManager.onUpdateLifecycle(lifecycle)
    }

    fun observeCallInProgress(callId: CallId): Observable<Boolean> {
        lifecycleAwareSurfaceManager
            .manageSurfaceUpdates(
                surfaceCode = SURFACE_CODE_LOCAL,
                onUseSurface = { surface ->

                    sipEngineClient.setLocalCameraFeedSurface(callId, surface)
                        .doOnSubscribe { events.onNext(UpdatingLocalSurface) }
                        .doOnComplete { events.onNext(LocalSurfaceUpdated) }
                        .doOnError { error -> events.onNext(UpdateLocalSurfaceFailed(error)) }
                },
                onFreeSurface = {
                    sipEngineClient.unsetLocalCameraFeedSurface(callId)
                        .doOnSubscribe { events.onNext(DestroyingLocalSurface) }
                        .doOnComplete { events.onNext(LocalSurfaceDestroyed) }
                        .doOnError { error -> events.onNext(DestroyLocalSurfaceFailed(error)) }
                }
            )
            .subscribe()
            .addTo(disposables)

        lifecycleAwareSurfaceManager
            .manageSurfaceUpdates(
                surfaceCode = SURFACE_CODE_REMOTE,
                onUseSurface = { surface ->

                    sipEngineClient.setRemoteVideoFeedSurface(callId, surface)
                        .doOnSubscribe { events.onNext(UpdatingRemoteSurface) }
                        .doOnComplete { events.onNext(RemoteSurfaceUpdated) }
                        .doOnError { error -> events.onNext(UpdateRemoteSurfaceFailed(error)) }
                },
                onFreeSurface = {
                    sipEngineClient.unsetRemoteVideoFeedSurface(callId)
                        .doOnSubscribe { events.onNext(DestroyingRemoteSurface) }
                        .doOnComplete { events.onNext(RemoteSurfaceDestroyed) }
                        .doOnError { error -> events.onNext(DestroyRemoteSurfaceFailed(error)) }
                }
            )
            .subscribe()
            .addTo(disposables)

        return observeCallDetails(callId)
            .map { update ->

                when (update) {
                    is CallInvitationUpdate -> !update.call.status.isTerminal
                    is CallSessionUpdate -> !update.call.status.isTerminal
                    NoCallUpdateAvailable -> false
                }
            }
            .distinctUntilChanged { previous, next -> previous == next }
    }

    fun observeCallDetails(callId: CallId): Observable<CallUpdate> {
        return sipEngineClient.observeCallDetails(callId)
    }

    fun observeIdentity(): Observable<IdentityUpdate> {
        return sipEngineClient.observeIdentity()
    }

    fun onUpdateLocalSurfaceView(surfaceView: SurfaceView?) {
        if (surfaceView == null) {
            lifecycleAwareSurfaceManager.onSurfaceViewDestroyed(SURFACE_CODE_LOCAL)

        } else {
            lifecycleAwareSurfaceManager.onUpdateSurfaceView(SURFACE_CODE_LOCAL, surfaceView)
        }
    }

    fun onUpdateRemoteSurfaceView(surfaceView: SurfaceView?) {
        if (surfaceView == null) {
            lifecycleAwareSurfaceManager.onSurfaceViewDestroyed(SURFACE_CODE_REMOTE)

        } else {
            lifecycleAwareSurfaceManager.onUpdateSurfaceView(SURFACE_CODE_REMOTE, surfaceView)
        }
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
