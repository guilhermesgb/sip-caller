package com.xibasdev.sipcaller.sip.linphone.calling.features

import android.view.Surface
import com.xibasdev.sipcaller.sip.calling.Call
import com.xibasdev.sipcaller.sip.calling.CallId
import com.xibasdev.sipcaller.sip.calling.details.CallInvitationUpdate
import com.xibasdev.sipcaller.sip.calling.details.CallSessionUpdate
import com.xibasdev.sipcaller.sip.calling.CallStatus.*
import com.xibasdev.sipcaller.sip.calling.details.NoCallUpdateAvailable
import com.xibasdev.sipcaller.sip.calling.features.CallFeaturesManagerApi
import com.xibasdev.sipcaller.sip.linphone.calling.details.LinphoneCallDetailsObserver
import com.xibasdev.sipcaller.sip.linphone.context.LinphoneContextApi
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Scheduler
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class LinphoneCallFeaturesManager @Inject constructor(
    @Named("LinphoneRxScheduler") private val scheduler: Scheduler,
    private val linphoneContext: LinphoneContextApi,
    private val callDetailsObserver: LinphoneCallDetailsObserver
) : CallFeaturesManagerApi {

    override fun setLocalCameraFeedSurface(callId: CallId, surface: Surface): Completable {
        return doSetSurfaceForCall(
            callId = callId,
            surface = surface,
            surfaceSetterOperation = linphoneContext::setLocalSurface
        )
    }

    override fun unsetLocalCameraFeedSurface(callId: CallId): Completable {
        return doUnsetSurfaceForCall(
            callId = callId,
            surfaceUnsetterOperation = linphoneContext::unsetLocalSurface
        )
    }

    override fun setRemoteVideoFeedSurface(callId: CallId, surface: Surface): Completable {
        return doSetSurfaceForCall(
            callId = callId,
            surface = surface,
            surfaceSetterOperation = linphoneContext::setRemoteSurface
        )
    }

    override fun unsetRemoteVideoFeedSurface(callId: CallId): Completable {
        return doUnsetSurfaceForCall(
            callId = callId,
            surfaceUnsetterOperation = linphoneContext::unsetRemoteSurface
        )
    }

    private fun doSetSurfaceForCall(
        callId: CallId,
        surface: Surface,
        surfaceSetterOperation: (surface: Surface) -> Boolean
    ): Completable {

        return callDetailsObserver.observeCallDetails(callId)
            .take(1)
            .switchMapCompletable { update ->

                when (update) {
                    is CallInvitationUpdate -> doSetSurfaceIfCallNotOver(
                        call = update.call,
                        surface = surface,
                        surfaceSetterOperation = surfaceSetterOperation
                    )
                    is CallSessionUpdate -> doSetSurfaceIfCallNotOver(
                        call = update.call,
                        surface = surface,
                        surfaceSetterOperation = surfaceSetterOperation
                    )
                    NoCallUpdateAvailable -> Completable.never()
                }
            }
            .subscribeOn(scheduler)
    }

    private fun doSetSurfaceIfCallNotOver(
        call: Call,
        surface: Surface,
        surfaceSetterOperation: (surface: Surface) -> Boolean
    ): Completable {

        return if (call.status.isTerminal) {
            Completable.complete()

        } else {
            Completable.fromCallable {
                val surfaceSet = surfaceSetterOperation(surface)

                if (!surfaceSet) {
                    throw IllegalStateException("Linphone failed to set surface!")
                }
            }
        }
    }

    private fun doUnsetSurfaceForCall(
        callId: CallId,
        surfaceUnsetterOperation: () -> Boolean
    ): Completable {

        return callDetailsObserver.observeCallDetails(callId)
            .take(1)
            .switchMapCompletable { update ->

                when (update) {
                    is CallInvitationUpdate -> doUnsetSurface(
                        surfaceUnsetterOperation = surfaceUnsetterOperation
                    )
                    is CallSessionUpdate -> doUnsetSurface(
                        surfaceUnsetterOperation = surfaceUnsetterOperation
                    )
                    NoCallUpdateAvailable -> Completable.never()
                }
            }
            .subscribeOn(scheduler)
    }

    private fun doUnsetSurface(surfaceUnsetterOperation: () -> Boolean): Completable {
        return Completable.fromCallable {
            val surfaceUnset = surfaceUnsetterOperation()

            if (!surfaceUnset) {
                throw IllegalStateException("Linphone failed to unset surface!")
            }
        }
    }
}
