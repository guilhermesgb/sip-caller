package com.xibasdev.sipcaller.sip.linphone.history

import com.elvishew.xlog.Logger
import com.xibasdev.sipcaller.sip.SipCallDirection
import com.xibasdev.sipcaller.sip.SipCallDirection.INCOMING
import com.xibasdev.sipcaller.sip.SipCallDirection.OUTGOING
import com.xibasdev.sipcaller.sip.SipCallErrorReason
import com.xibasdev.sipcaller.sip.SipCallId
import com.xibasdev.sipcaller.sip.history.CallFailedUpdate
import com.xibasdev.sipcaller.sip.history.CallFinishedUpdate
import com.xibasdev.sipcaller.sip.history.CallHistoryObserverApi
import com.xibasdev.sipcaller.sip.history.CallHistoryUpdate
import com.xibasdev.sipcaller.sip.history.CallInvitationAccepted
import com.xibasdev.sipcaller.sip.history.CallInvitationCanceled
import com.xibasdev.sipcaller.sip.history.CallInvitationDeclined
import com.xibasdev.sipcaller.sip.history.CallInvitationDetected
import com.xibasdev.sipcaller.sip.history.CallInvitationFailed
import com.xibasdev.sipcaller.sip.history.CallInvitationMissed
import com.xibasdev.sipcaller.sip.history.CallInviteAcceptedElsewhere
import com.xibasdev.sipcaller.sip.history.CallSessionFailed
import com.xibasdev.sipcaller.sip.history.CallSessionFinishedByCallee
import com.xibasdev.sipcaller.sip.history.CallSessionFinishedByCaller
import com.xibasdev.sipcaller.sip.history.ConditionalCallHistoryUpdate
import com.xibasdev.sipcaller.sip.linphone.context.LinphoneCallStateChange
import com.xibasdev.sipcaller.sip.linphone.context.LinphoneContextApi
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.subjects.BehaviorSubject
import io.reactivex.rxjava3.subjects.ReplaySubject
import java.util.TreeMap
import javax.inject.Inject
import javax.inject.Named
import kotlin.reflect.KClass
import org.linphone.core.Call.Dir
import org.linphone.core.Call.Dir.Incoming
import org.linphone.core.Call.Dir.Outgoing
import org.linphone.core.Call.State.End
import org.linphone.core.Call.State.Error
import org.linphone.core.Call.State.IncomingReceived
import org.linphone.core.Call.State.OutgoingProgress
import org.linphone.core.Call.State.StreamsRunning
import org.linphone.core.Call.Status.Aborted
import org.linphone.core.Call.Status.AcceptedElsewhere
import org.linphone.core.Call.Status.Declined
import org.linphone.core.Call.Status.DeclinedElsewhere
import org.linphone.core.Call.Status.Missed
import org.linphone.core.Call.Status.Success

class LinphoneCallHistoryObserver @Inject constructor(
    private val linphoneContext: LinphoneContextApi,
    @Named("SipEngineLogger") private val logger: Logger
) : CallHistoryObserverApi {

    override fun observeCallHistory(): Observable<List<CallHistoryUpdate>> {
        return callHistoryObserver
    }

    private val currentCallHistoryUpdates = TreeMap<String, CallHistoryUpdate>()
    private val latestCallHistoryUpdates = BehaviorSubject.create<CallHistoryUpdate>()

    private val callStateChangeListenerId = linphoneContext.createCallStateChangeListener {
            callStateChange, _ ->

        with (callStateChange) {
            processCallStateChange()
        }
    }

    private val callHistoryObserver = ReplaySubject.create<List<CallHistoryUpdate>>().apply {
        processCallHistoryUpdates(this)
    }

    context (LinphoneCallStateChange)
    private fun processCallStateChange() {
        when (state) {
            IncomingReceived,
            OutgoingProgress -> postCallHistoryUpdate(::CallInvitationDetected)
            StreamsRunning -> ifPreviousHistoryUpdateIs(CallInvitationDetected::class) {
                postCallHistoryUpdate(::CallInvitationAccepted)
            }
            Error -> {
                ifPreviousHistoryUpdateIs(CallInvitationDetected::class) {
                    postCallFailedUpdate(::CallInvitationFailed)
                }

                ifPreviousHistoryUpdateIs(CallInvitationAccepted::class) {
                    postCallFailedUpdate(::CallSessionFailed)
                }
            }
            End -> when (status) {
                Aborted -> when (direction) {
                    Incoming -> ifPreviousHistoryUpdateIs(CallInvitationDetected::class) {
                        postConditionalCallHistoryUpdate(
                            ::CallInvitationDeclined,
                            ::CallInvitationCanceled
                        )
                    }
                    Outgoing -> ifPreviousHistoryUpdateIs(CallInvitationDetected::class) {
                        postConditionalCallHistoryUpdate(
                            ::CallInvitationCanceled,
                            ::CallInvitationDeclined
                        )
                    }
                }
                Missed -> ifPreviousHistoryUpdateIs(CallInvitationDetected::class) {
                    postCallHistoryUpdate(::CallInvitationMissed)
                }
                Declined,
                DeclinedElsewhere -> ifPreviousHistoryUpdateIs(CallInvitationDetected::class) {
                    postCallHistoryUpdate(::CallInvitationDeclined)
                }
                AcceptedElsewhere -> ifPreviousHistoryUpdateIs(CallInvitationDetected::class) {
                    postCallHistoryUpdate(::CallInviteAcceptedElsewhere)
                }
                Success -> when (direction) {
                    Incoming -> ifPreviousHistoryUpdateIs(CallInvitationAccepted::class) {
                        postConditionalCallHistoryUpdate(
                            ::CallSessionFinishedByCallee,
                            ::CallSessionFinishedByCaller
                        )
                    }
                    Outgoing -> ifPreviousHistoryUpdateIs(CallInvitationAccepted::class) {
                        postConditionalCallHistoryUpdate(
                            ::CallSessionFinishedByCaller,
                            ::CallSessionFinishedByCallee
                        )
                    }
                }
                else -> {}
            }
            else -> {}
        }
    }

    context (LinphoneCallStateChange)
    private fun <T : CallHistoryUpdate> ifPreviousHistoryUpdateIs(
        updateClass: KClass<T>,
        contextualFunction: () -> Unit
    ) {

        if (thereIsPreviousHistoryUpdate()) {
            getPreviousHistoryUpdate()?.let { previousHistoryUpdate ->

                if (previousHistoryUpdate::class == updateClass) {
                    contextualFunction()
                }
            }
        }
    }

    context (LinphoneCallStateChange)
    private fun thereIsPreviousHistoryUpdate() = currentCallHistoryUpdates.containsKey(callId)

    context (LinphoneCallStateChange)
    private fun getPreviousHistoryUpdate(): CallHistoryUpdate? {
        return currentCallHistoryUpdates[callId]
    }

    context (LinphoneCallStateChange)
    private fun <T : CallHistoryUpdate> postCallHistoryUpdate(
        updateCreator: (SipCallId, SipCallDirection) -> T
    ) {

        val update = updateCreator(createSipCallId(callId), createSipCallDirection(direction))
        currentCallHistoryUpdates[callId] = update
        latestCallHistoryUpdates.onNext(update)
    }

    context (LinphoneCallStateChange)
    private fun <T : CallHistoryUpdate> postConditionalCallHistoryUpdate(
        updateCreatorIfCallFinishedByLocalParty: (SipCallId, SipCallDirection) -> T,
        updateCreatorIfCallFinishedByRemoteParty: (SipCallId, SipCallDirection) -> T,
    ) {

        val updateIfFinishedByLocalParty = updateCreatorIfCallFinishedByLocalParty(
            createSipCallId(callId), createSipCallDirection(direction)
        )
        val updateIfFinishedByRemoteParty = updateCreatorIfCallFinishedByRemoteParty(
            createSipCallId(callId), createSipCallDirection(direction)
        )

        val conditionalUpdate = ConditionalCallHistoryUpdate(
            createSipCallId(callId), createSipCallDirection(direction),
            updateIfFinishedByLocalParty, updateIfFinishedByRemoteParty
        )
        currentCallHistoryUpdates[callId] = conditionalUpdate
        latestCallHistoryUpdates.onNext(conditionalUpdate)
    }

    context (LinphoneCallStateChange)
    private fun <T : CallFailedUpdate> postCallFailedUpdate(
        updateCreator: (SipCallId, SipCallDirection, SipCallErrorReason) -> T
    ) {

        val update = updateCreator(
            createSipCallId(callId),
            createSipCallDirection(direction),
            createSipCallErrorReason(errorReason)
        )
        currentCallHistoryUpdates[callId] = update
        latestCallHistoryUpdates.onNext(update)
    }

    private fun processCallHistoryUpdates(subject: ReplaySubject<List<CallHistoryUpdate>>) {
        with (linphoneContext) {
            doWhenLinphoneCoreStartsOrStops(subject) { isLinphoneCoreStarted ->

                if (isLinphoneCoreStarted) {
                    enableCoreListener(callStateChangeListenerId)

                    latestCallHistoryUpdates
                        .flatMap { update ->

                            if (update is ConditionalCallHistoryUpdate) {
                                wasCallFinishedByLocalParty(update.callId)
                                    .flatMapObservable { wasCallFinishedByLocalParty ->

                                        if (wasCallFinishedByLocalParty) {
                                            Observable.just(
                                                update.updateIfCallFinishedByLocalParty
                                            )

                                        } else {
                                            Observable.just(
                                                update.updateIfCallFinishedByRemoteParty
                                            )
                                        }
                                    }

                            } else {
                                Observable.just(update)
                            }
                        }.map { newUpdate ->

                            currentCallHistoryUpdates[newUpdate.callId.value] = newUpdate

                            val callHistoryUpdates = mutableListOf<CallHistoryUpdate>()

                            currentCallHistoryUpdates.values.forEach { update ->
                                callHistoryUpdates.add(update)
                            }

                            if (newUpdate is CallFailedUpdate || newUpdate is CallFinishedUpdate) {
                                currentCallHistoryUpdates.remove(newUpdate.callId.value)
                            }

                            callHistoryUpdates
                        }

                } else {
                    disableCoreListener(callStateChangeListenerId)

                    Observable.empty()
                }
            }
        }
    }

    private fun createSipCallId(callId: String?): SipCallId {
        return SipCallId(callId.orEmpty())
    }

    private fun createSipCallDirection(direction: Dir?): SipCallDirection {
        return when (direction) {
            Incoming -> INCOMING
            else -> OUTGOING
        }
    }

    private fun createSipCallErrorReason(errorReason: String?): SipCallErrorReason {
        return SipCallErrorReason(errorReason.orEmpty())
    }
}
