package com.xibasdev.sipcaller.sip.linphone.calling.state

import com.elvishew.xlog.Logger
import com.xibasdev.sipcaller.sip.calling.CallId
import com.xibasdev.sipcaller.sip.calling.features.CallFeature
import com.xibasdev.sipcaller.sip.calling.features.CallFeatures
import com.xibasdev.sipcaller.sip.calling.state.CallStateManagerApi
import com.xibasdev.sipcaller.sip.linphone.context.LinphoneContextApi
import com.xibasdev.sipcaller.sip.registering.account.AccountInfo
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.CompletableEmitter
import io.reactivex.rxjava3.core.Scheduler
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.core.SingleEmitter
import javax.inject.Inject
import javax.inject.Named
import org.linphone.core.Call
import org.linphone.core.Call.State.*

class LinphoneCallStateManager @Inject constructor(
    @Named("LinphoneRxScheduler") private val scheduler: Scheduler,
    private val linphoneContext: LinphoneContextApi,
    @Named("SipEngineLogger") private val logger: Logger
) : CallStateManagerApi {

    override fun sendCallInvitation(account: AccountInfo): Single<CallId> {
        return sendCallInvitationIfLinphoneIsNotProcessingAnyCalls(account)
    }

    override fun cancelCallInvitation(callId: CallId): Completable {
        return triggerCallOperationWorkIfLinphoneIsProcessingCall(
            callId = callId,
            workDescription = "cancel outgoing call invitation",
            operation = ::doCancelCallInvitation
        )
    }

    override fun acceptCallInvitation(callId: CallId): Completable {
        return triggerCallOperationWorkIfLinphoneIsProcessingCall(
            callId = callId,
            workDescription = "accept incoming call invitation",
            operation = ::doAcceptCallInvitation
        )
    }

    override fun declineCallInvitation(callId: CallId): Completable {
        return triggerCallOperationWorkIfLinphoneIsProcessingCall(
            callId = callId,
            workDescription = "decline incoming call invitation",
            operation = ::doDeclineCallInvitation
        )
    }

    override fun terminateCallSession(callId: CallId): Completable {
        return triggerCallOperationWorkIfLinphoneIsProcessingCall(
            callId = callId,
            workDescription = "terminate ongoing call session",
            operation = ::doTerminateCallSession
        )
    }

    private fun sendCallInvitationIfLinphoneIsNotProcessingAnyCalls(
        targetAccount: AccountInfo
    ): Single<CallId> {

        return Single
            .create { emitter ->

                if (linphoneContext.isCurrentlyHandlingCall()) {
                    val error = IllegalStateException(
                        "Cannot send new call invitation - " +
                                "Linphone currently busy handling another call!"
                    )
                    logger.e("Failed to issue call operation!", error)
                    emitter.onError(error)

                } else {
                    doSendCallInvitation(emitter, targetAccount)
                }
            }
            .subscribeOn(scheduler)
    }

    private fun doSendCallInvitation(emitter: SingleEmitter<CallId>, targetAccount: AccountInfo) {
        doTriggerCallOperationWorkAndEmitOutcome(
            singleEmitter = emitter,
            successStates = setOf(OutgoingRinging, OutgoingEarlyMedia, Connected),
            errorStates = setOf(Error, End, Released),
            workDescription = "send outgoing call invitation",
            triggerOperationWork = { linphoneContext.sendCallInvitation(targetAccount) },
            postOperationWork = { call, _ ->

                // TODO respect currently-enabled permissions
                linphoneContext.enableOrDisableCallFeatures(
                    call = call,
                    features = CallFeatures(
                        microphone = CallFeature(enabled = true),
                        speaker = CallFeature(enabled = true),
                        camera = CallFeature(enabled = true)
                    )
                )
            }
        )
    }

    private fun triggerCallOperationWorkIfLinphoneIsProcessingCall(
        callId: CallId,
        workDescription: String,
        operation: (emitter: CompletableEmitter, callId: CallId) -> Unit
    ): Completable {

        return Completable
            .create { emitter ->

                if (linphoneContext.isCurrentlyHandlingCall()) {
                    operation(emitter, callId)

                } else {
                    val error = IllegalStateException("Cannot $workDescription - " +
                            "Linphone currently not handling any calls!")
                    logger.e("Failed to $workDescription - " +
                            "Linphone not handling corresponding call!", error)
                    emitter.onError(error)
                }
            }
            .subscribeOn(scheduler)
    }

    private fun doCancelCallInvitation(emitter: CompletableEmitter, callId: CallId) {
        doTriggerCallOperationWorkAndEmitOutcome(
            completableEmitter = emitter,
            successStates = setOf(End),
            errorStates = setOf(Error, Released),
            workDescription = "cancel outgoing call invitation",
            triggerOperationWork = { linphoneContext.cancelCallInvitation(callId) }
        )
    }

    private fun doAcceptCallInvitation(emitter: CompletableEmitter, callId: CallId) {
        doTriggerCallOperationWorkAndEmitOutcome(
            completableEmitter = emitter,
            successStates = setOf(End),
            errorStates = setOf(Error, Released),
            workDescription = "accept incoming call invitation",
            triggerOperationWork = { linphoneContext.acceptCallInvitation(callId) },
            postOperationWork = { call, _ ->

                // TODO respect currently-enabled permissions
                linphoneContext.enableOrDisableCallFeatures(
                    call = call,
                    features = CallFeatures(
                        microphone = CallFeature(enabled = true),
                        speaker = CallFeature(enabled = true),
                        camera = CallFeature(enabled = true)
                    )
                )
            }
        )
    }

    private fun doDeclineCallInvitation(emitter: CompletableEmitter, callId: CallId) {
        doTriggerCallOperationWorkAndEmitOutcome(
            completableEmitter = emitter,
            successStates = setOf(End),
            errorStates = setOf(Error, Released),
            workDescription = "decline incoming call invitation",
            triggerOperationWork = { linphoneContext.declineCallInvitation(callId) }
        )
    }

    private fun doTerminateCallSession(emitter: CompletableEmitter, callId: CallId) {
        doTriggerCallOperationWorkAndEmitOutcome(
            completableEmitter = emitter,
            successStates = setOf(End),
            errorStates = setOf(Error, Released),
            workDescription = "terminate ongoing call session",
            triggerOperationWork = { linphoneContext.terminateCallSession(callId) }
        )
    }

    private fun doTriggerCallOperationWorkAndEmitOutcome(
        singleEmitter: SingleEmitter<CallId>? = null,
        completableEmitter: CompletableEmitter? = null,
        successStates: Set<Call.State>,
        errorStates: Set<Call.State>,
        workDescription: String,
        triggerOperationWork: () -> Boolean,
        postOperationWork: (call: Call, callId: CallId) -> Boolean = { _, _ -> true }
    ) {

        val callStateChangeListenerId = linphoneContext.createCallStateChangeListener {
                callStateChange, coreListenerId ->

            if (callStateChange.state in errorStates) {
                linphoneContext.disableCoreListener(coreListenerId)

                val error = IllegalStateException(
                    "Failed (async) to $workDescription - ${callStateChange.errorReason}!"
                )
                logger.e("Linphone failed to $workDescription!", error)
                singleEmitter?.onError(error)
                completableEmitter?.onError(error)

            } else if (callStateChange.state in successStates) {
                linphoneContext.disableCoreListener(coreListenerId)

                val callId = CallId(callStateChange.callId)
                val call = callStateChange.call

                if (postOperationWork(call, callId)) {
                    singleEmitter?.onSuccess(callId)
                    completableEmitter?.onComplete()

                } else {
                    val error = IllegalStateException(
                        "Failed to completely perform work to " +
                                "$workDescription, but it was partially executed."
                    )
                    logger.e(
                        "Linphone managed to $workDescription but failed to " +
                                "complete needed work right after this step!", error
                    )
                    singleEmitter?.onError(error)
                    completableEmitter?.onError(error)
                }
            }
        }
        linphoneContext.enableCoreListener(callStateChangeListenerId)

        val operationWorkTriggered = triggerOperationWork()

        if (!operationWorkTriggered) {
            linphoneContext.disableCoreListener(callStateChangeListenerId)

            val error = IllegalStateException("Failed (sync) to $workDescription!")
            logger.e("Linphone failed to $workDescription!", error)
            singleEmitter?.onError(error)
            completableEmitter?.onError(error)
        }
    }
}
