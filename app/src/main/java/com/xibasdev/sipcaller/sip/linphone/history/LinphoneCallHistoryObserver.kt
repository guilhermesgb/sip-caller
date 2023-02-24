package com.xibasdev.sipcaller.sip.linphone.history

import android.util.SparseArray
import androidx.core.util.valueIterator
import com.xibasdev.sipcaller.sip.SipCallDirection
import com.xibasdev.sipcaller.sip.SipCallDirection.INCOMING
import com.xibasdev.sipcaller.sip.SipCallDirection.OUTGOING
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
import com.xibasdev.sipcaller.sip.linphone.LinphoneContext
import com.xibasdev.sipcaller.sip.linphone.LinphoneCore
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.subjects.BehaviorSubject
import io.reactivex.rxjava3.subjects.ReplaySubject
import javax.inject.Inject
import kotlin.reflect.KClass
import org.linphone.core.Call
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
import org.linphone.core.Core
import org.linphone.core.CoreListener
import org.linphone.core.CoreListenerStub

private typealias CallInfo = Pair<SipCallId, SipCallDirection>

class LinphoneCallHistoryObserver @Inject constructor(
    private val linphoneCore: LinphoneCore,
    private val linphoneContext: LinphoneContext
) : CallHistoryObserverApi {

    override fun observeCallHistory(): Observable<List<CallHistoryUpdate>> {
        return callHistoryObserver
    }

    private val currentCallHistoryUpdates = SparseArray<CallHistoryUpdate>()
    private val latestCallHistoryUpdates = BehaviorSubject.create<CallHistoryUpdate>()

    private val callStateChangeListener: CoreListener = object : CoreListenerStub() {
        override fun onCallStateChanged(
            core: Core,
            call: Call,
            callState: Call.State,
            errorReason: String
        ) {

            with (CallInfo(
                createSipCallId(call.callLog.callId),
                createSipCallDirection(call.dir)
            )) {
                processCallStateChange(call, callState, errorReason)
            }
        }
    }

    private val callHistoryObserver = ReplaySubject.create<List<CallHistoryUpdate>>().apply {
        processCallHistoryUpdates(this)
    }

    context (CallInfo)
    private fun processCallStateChange(call: Call, callState: Call.State, errorReason: String) {
        when (callState) {
            IncomingReceived,
            OutgoingProgress -> postCallHistoryUpdate(::CallInvitationDetected)
            StreamsRunning -> ifPreviousHistoryIs(CallInvitationDetected::class) {
                postCallHistoryUpdate(::CallInvitationAccepted)
            }
            Error -> {
                ifPreviousHistoryIs(CallInvitationDetected::class) {
                    postCallFailedUpdate(::CallInvitationFailed, errorReason)
                }

                ifPreviousHistoryIs(CallInvitationAccepted::class) {
                    postCallFailedUpdate(::CallSessionFailed, errorReason)
                }
            }
            End -> when (call.callLog.status) {
                Aborted -> when (call.dir) {
                    Incoming -> ifPreviousHistoryIs(CallInvitationDetected::class) {
                        postConditionalCallHistoryUpdate(
                            ::CallInvitationDeclined,
                            ::CallInvitationCanceled
                        )
                    }
                    Outgoing -> ifPreviousHistoryIs(CallInvitationDetected::class) {
                        postConditionalCallHistoryUpdate(
                            ::CallInvitationCanceled,
                            ::CallInvitationDeclined
                        )
                    }
                    else -> {}
                }
                Missed -> ifPreviousHistoryIs(CallInvitationDetected::class) {
                    postCallHistoryUpdate(::CallInvitationMissed)
                }
                Declined,
                DeclinedElsewhere -> ifPreviousHistoryIs(CallInvitationDetected::class) {
                    postCallHistoryUpdate(::CallInvitationDeclined)
                }
                AcceptedElsewhere -> ifPreviousHistoryIs(CallInvitationDetected::class) {
                    postCallHistoryUpdate(::CallInviteAcceptedElsewhere)
                }
                Success -> when (call.dir) {
                    Incoming -> ifPreviousHistoryIs(CallInvitationAccepted::class) {
                        postConditionalCallHistoryUpdate(
                            ::CallSessionFinishedByCallee,
                            ::CallSessionFinishedByCaller
                        )
                    }
                    Outgoing -> ifPreviousHistoryIs(CallInvitationAccepted::class) {
                        postConditionalCallHistoryUpdate(
                            ::CallSessionFinishedByCaller,
                            ::CallSessionFinishedByCallee
                        )
                    }
                    else -> {}
                }
                else -> {}
            }
            else -> {}
        }
    }

    private fun createSipCallId(callId: String?): SipCallId {
        return SipCallId(callId.orEmpty())
    }

    private fun createSipCallDirection(direction: Call.Dir): SipCallDirection {
        return when(direction) {
            Incoming -> INCOMING
            else -> OUTGOING
        }
    }

    context (CallInfo)
    private fun <T : CallHistoryUpdate> ifPreviousHistoryIs(
        updateClass: KClass<T>,
        contextualFunction: () -> Unit
    ) {

        if (thereIsPreviousHistoryUpdate()) {
            val previousHistoryUpdate = getPreviousHistoryUpdate()

            if (previousHistoryUpdate::class == updateClass) {
                contextualFunction()
            }
        }
    }

    context (CallInfo)
    private fun thereIsPreviousHistoryUpdate() =
        currentCallHistoryUpdates.indexOfKey(first.hashCode()) >= 0

    context (CallInfo)
    private fun getPreviousHistoryUpdate(): CallHistoryUpdate {
        return currentCallHistoryUpdates.get(first.hashCode())
    }

    context (CallInfo)
    private fun <T : CallHistoryUpdate> postCallHistoryUpdate(
        updateCreator: (SipCallId, SipCallDirection) -> T
    ) {

        val update = updateCreator(first, second)
        currentCallHistoryUpdates.put(first.hashCode(), update)
        latestCallHistoryUpdates.onNext(update)
    }

    context (CallInfo)
    private fun <T : CallHistoryUpdate> postConditionalCallHistoryUpdate(
        updateCreatorIfCallFinishedByLocalParty: (SipCallId, SipCallDirection) -> T,
        updateCreatorIfCallFinishedByRemoteParty: (SipCallId, SipCallDirection) -> T,
    ) {

        val updateIfFinishedByLocalParty = updateCreatorIfCallFinishedByLocalParty(first, second)
        val updateIfFinishedByRemoteParty = updateCreatorIfCallFinishedByRemoteParty(first, second)

        val conditionalUpdate = ConditionalCallHistoryUpdate(
            first, second, updateIfFinishedByLocalParty, updateIfFinishedByRemoteParty
        )
        currentCallHistoryUpdates.put(first.hashCode(), conditionalUpdate)
        latestCallHistoryUpdates.onNext(conditionalUpdate)
    }

    context (CallInfo)
    private fun <T : CallFailedUpdate> postCallFailedUpdate(
        updateCreator: (SipCallId, SipCallDirection, String) -> T, errorReason: String
    ) {
        val update = updateCreator(first, second, errorReason)
        currentCallHistoryUpdates.put(first.hashCode(), update)
        latestCallHistoryUpdates.onNext(update)
    }

    private fun processCallHistoryUpdates(subject: ReplaySubject<List<CallHistoryUpdate>>) {
        with (linphoneContext) {
            updateWhileLinphoneStarted(subject) { isLinphoneStarted ->

                if (isLinphoneStarted) {
                    linphoneCore.addListener(callStateChangeListener)

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

                            currentCallHistoryUpdates.put(newUpdate.callId.hashCode(), newUpdate)

                            val callHistoryUpdates = mutableListOf<CallHistoryUpdate>()

                            currentCallHistoryUpdates.valueIterator().forEach { update ->
                                callHistoryUpdates.add(update)
                            }

                            if (newUpdate is CallFailedUpdate || newUpdate is CallFinishedUpdate) {
                                currentCallHistoryUpdates.remove(newUpdate.callId.hashCode())
                            }

                            callHistoryUpdates
                        }

                } else {
                    linphoneCore.removeListener(callStateChangeListener)

                    Observable.empty()
                }
            }
        }
    }
}
