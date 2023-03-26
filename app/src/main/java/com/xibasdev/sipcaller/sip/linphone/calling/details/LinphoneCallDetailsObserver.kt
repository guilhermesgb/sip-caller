package com.xibasdev.sipcaller.sip.linphone.calling.details

import com.elvishew.xlog.Logger
import com.xibasdev.sipcaller.sip.calling.Call
import com.xibasdev.sipcaller.sip.calling.CallDirection.INCOMING
import com.xibasdev.sipcaller.sip.calling.CallDirection.OUTGOING
import com.xibasdev.sipcaller.sip.calling.CallId
import com.xibasdev.sipcaller.sip.calling.details.CallInvitationUpdate
import com.xibasdev.sipcaller.sip.calling.details.CallSessionUpdate
import com.xibasdev.sipcaller.sip.calling.CallStage
import com.xibasdev.sipcaller.sip.calling.CallStage.INVITATION
import com.xibasdev.sipcaller.sip.calling.CallStage.SESSION
import com.xibasdev.sipcaller.sip.calling.CallStatus
import com.xibasdev.sipcaller.sip.calling.CallStatus.ABORTED_DUE_TO_ERROR
import com.xibasdev.sipcaller.sip.calling.CallStatus.ACCEPTED
import com.xibasdev.sipcaller.sip.calling.CallStatus.ACCEPTED_ELSEWHERE
import com.xibasdev.sipcaller.sip.calling.CallStatus.CANCELED
import com.xibasdev.sipcaller.sip.calling.CallStatus.DECLINED
import com.xibasdev.sipcaller.sip.calling.CallStatus.FINISHED_BY_LOCAL_PARTY
import com.xibasdev.sipcaller.sip.calling.CallStatus.FINISHED_BY_REMOTE_PARTY
import com.xibasdev.sipcaller.sip.calling.CallStatus.FINISHED_DUE_TO_ERROR
import com.xibasdev.sipcaller.sip.calling.CallStatus.MISSED
import com.xibasdev.sipcaller.sip.calling.CallStatus.RINGING
import com.xibasdev.sipcaller.sip.calling.details.CallUpdate
import com.xibasdev.sipcaller.sip.calling.details.NoCallUpdateAvailable
import com.xibasdev.sipcaller.sip.calling.details.CallDetailsObserverApi
import com.xibasdev.sipcaller.sip.calling.parties.CallParties
import com.xibasdev.sipcaller.sip.calling.streams.CallStreams
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
import com.xibasdev.sipcaller.sip.history.ConditionalCallSessionFinishedUpdate
import com.xibasdev.sipcaller.sip.linphone.context.CallStatsUpdated
import com.xibasdev.sipcaller.sip.linphone.context.LinphoneCallStatsChange
import com.xibasdev.sipcaller.sip.linphone.context.LinphoneContextApi
import com.xibasdev.sipcaller.sip.linphone.history.LinphoneCallHistoryObserver
import com.xibasdev.sipcaller.sip.linphone.utils.resolveMediaStream
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.subjects.BehaviorSubject
import io.reactivex.rxjava3.subjects.ReplaySubject
import java.time.Clock
import java.time.OffsetDateTime
import java.util.TreeMap
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class LinphoneCallDetailsObserver @Inject constructor(
    private val linphoneContext: LinphoneContextApi,
    private val callHistoryObserver: LinphoneCallHistoryObserver,
    @Named("SipEngineLogger") private val logger: Logger,
    @Named("LinphoneSipEngineClock") private val clock: Clock
) : CallDetailsObserverApi {

    private data class InternalCallStreamsUpdate(
        val callId: CallId,
        val streams: CallStreams
    )

    private val callStatsChangeListenerId = linphoneContext.createCallStatsChangeListener {
            callStatsChange, _ ->

        with (callStatsChange) {
            processCallStatsChange()
        }
    }

    private val currentCallStreamsUpdates = TreeMap<String, InternalCallStreamsUpdate>()
    private val latestCallStreamsUpdates = BehaviorSubject.create<List<InternalCallStreamsUpdate>>()

    private val latestObservedCallUpdates = ReplaySubject.create<CallUpdate>().apply {
        processObservedCallUpdates(this)
    }

    override fun observeCallDetails(callId: CallId): Observable<CallUpdate> {
        return latestObservedCallUpdates.filter { update ->

            when (update) {
                is CallInvitationUpdate -> update.call.callId == callId
                is CallSessionUpdate -> update.call.callId == callId
                NoCallUpdateAvailable -> false
            }
        }
    }

    context (LinphoneCallStatsChange)
    private fun processCallStatsChange() {
        if (!currentCallStreamsUpdates.containsKey(callId)) {
            currentCallStreamsUpdates[callId] = InternalCallStreamsUpdate(
                callId = CallId(callId),
                streams = CallStreams()
            )
        }

        if (audioStats is CallStatsUpdated) {
            currentCallStreamsUpdates[callId]?.let { callStreams ->

                val updatedCallStreams = callStreams.copy(
                    streams = callStreams.streams.copy(
                        audio = resolveMediaStream(audioStats.stream)
                    )
                )
                currentCallStreamsUpdates[callId] = updatedCallStreams
                latestCallStreamsUpdates.onNext(currentCallStreamsUpdates.values.toList())
            }
        } else if (videoStats is CallStatsUpdated) {
            currentCallStreamsUpdates[callId]?.let { callStreams ->

                val updatedCallStreams = callStreams.copy(
                    streams = callStreams.streams.copy(
                        video = resolveMediaStream(videoStats.stream)
                    )
                )
                currentCallStreamsUpdates[callId] = updatedCallStreams
                latestCallStreamsUpdates.onNext(currentCallStreamsUpdates.values.toList())
            }
        }
    }

    private fun processObservedCallUpdates(subject: ReplaySubject<CallUpdate>) {
        with (linphoneContext) {
            doWhenLinphoneCoreStartsOrStops(subject) { isLinphoneCoreStarted ->

                logger.d("Calling manager detected Linphone core " +
                        (if (isLinphoneCoreStarted) "start!" else "stop!"))

                if (isLinphoneCoreStarted) {
                    enableCoreListener(callStatsChangeListenerId)

                    callHistoryObserver.observeCallHistory(OffsetDateTime.now(clock))
                        .flatMap { callHistoryUpdate ->

                            Observable.fromIterable(callHistoryUpdate)
                        }
                        .switchMap { callHistoryUpdate ->

                            val callId = callHistoryUpdate.callId
                            val direction = callHistoryUpdate.callDirection
                            val parties = CallParties(
                                local = callHistoryUpdate.localAccount,
                                remote = callHistoryUpdate.remoteAccount
                            )
                            val stage = callHistoryUpdate.resolveCallStage()
                            val status = callHistoryUpdate.resolveCallStatus()

                            when (callHistoryUpdate) {
                                is CallInvitationDetected -> processCallInvitationProgress(
                                    Call(
                                        callId = callId,
                                        direction = direction,
                                        parties = parties,
                                        stage = stage,
                                        status = status,
                                        streams = callHistoryUpdate.streams
                                    )
                                )
                                is CallInvitationFailed,
                                is CallInvitationCanceled,
                                is CallInvitationDeclined,
                                is CallInvitationMissed,
                                is CallInviteAcceptedElsewhere -> Observable.just(
                                    CallInvitationUpdate(
                                        call = Call(
                                            callId = callId,
                                            direction = direction,
                                            parties = parties,
                                            stage = stage,
                                            status = status
                                        )
                                    )
                                )
                                is CallInvitationAccepted -> processCallSessionProgress(
                                    Call(
                                        callId = callId,
                                        direction = direction,
                                        parties = parties,
                                        stage = stage,
                                        status = status,
                                        streams = callHistoryUpdate.streams
                                    )
                                )
                                is CallSessionFailed,
                                is CallSessionFinishedByCallee,
                                is CallSessionFinishedByCaller -> Observable.just(
                                    CallSessionUpdate(
                                        call = Call(
                                            callId = callId,
                                            direction = direction,
                                            parties = parties,
                                            stage = stage,
                                            status = status
                                        )
                                    )
                                )
                                else -> Observable.never()
                            }
                        }
                        .scan { previous, next ->

                            val nextCall = when (next) {
                                is CallInvitationUpdate -> next.call
                                is CallSessionUpdate -> next.call
                                NoCallUpdateAvailable -> return@scan next
                            }

                            val previousCall = when (previous) {
                                is CallInvitationUpdate -> previous.call
                                is CallSessionUpdate -> previous.call
                                NoCallUpdateAvailable -> return@scan next
                            }

                            if (nextCall.status.isTerminal) {
                                currentCallStreamsUpdates.remove(nextCall.callId.value)

                                if (next is CallInvitationUpdate) {
                                    return@scan next.copy(
                                        call = nextCall.copy(
                                            durationMs = previousCall.durationMs
                                        )
                                    )

                                } else if (next is CallSessionUpdate) {
                                    return@scan next.copy(
                                        call = nextCall.copy(
                                            durationMs = previousCall.durationMs
                                        )
                                    )
                                }
                            }

                            next
                        }

                } else {
                    disableCoreListener(callStatsChangeListenerId)

                    Observable.empty()
                }
            }
        }
    }

    private fun CallHistoryUpdate.resolveCallStage(): CallStage {
        return when (this) {
            is CallInvitationDetected,
            is CallInvitationFailed,
            is CallInvitationCanceled,
            is CallInvitationDeclined,
            is CallInvitationMissed,
            is CallInviteAcceptedElsewhere -> INVITATION
            is CallInvitationAccepted,
            is CallSessionFailed,
            is CallSessionFinishedByCallee,
            is CallSessionFinishedByCaller,
            is ConditionalCallSessionFinishedUpdate -> SESSION
        }
    }

    private fun CallHistoryUpdate.resolveCallStatus(): CallStatus {
        return when (this) {
            is CallInvitationDetected -> RINGING
            is CallInvitationFailed -> ABORTED_DUE_TO_ERROR
            is CallInvitationCanceled -> CANCELED
            is CallInvitationDeclined -> DECLINED
            is CallInvitationMissed -> MISSED
            is CallInviteAcceptedElsewhere -> ACCEPTED_ELSEWHERE
            is CallInvitationAccepted -> ACCEPTED
            is CallSessionFailed -> FINISHED_DUE_TO_ERROR
            is CallSessionFinishedByCallee -> when (callDirection) {
                OUTGOING -> FINISHED_BY_REMOTE_PARTY
                INCOMING -> FINISHED_BY_LOCAL_PARTY
            }
            is CallSessionFinishedByCaller -> when (callDirection) {
                OUTGOING -> FINISHED_BY_LOCAL_PARTY
                INCOMING -> FINISHED_BY_REMOTE_PARTY
            }
            else -> {
                // This should never happen, callHistoryObserver.observeCallHistory() guarantees it.
                // FIXME Do not leak this implementation detail, rework the data classes structure.
                throw IllegalStateException("Conditional history update emitted by observer!")
            }
        }
    }

    private fun processCallInvitationProgress(call: Call): Observable<CallUpdate> {
        return processCallStatsUpdates(call)
            .takeUntilCallOver(call.callId)
    }

    private fun processCallSessionProgress(call: Call): Observable<CallUpdate> {
        return Observable
            .combineLatest(
                Observable
                    .interval(1, TimeUnit.SECONDS)
                    .map { durationS ->

                        call.copy(
                            durationMs = durationS * 1000
                        )
                    },
                processCallStatsUpdates(call)
            ) { durationUpdate, statsUpdate ->

                statsUpdate.copy(
                    durationMs = durationUpdate.durationMs
                )
            }
            .takeUntilCallOver(call.callId)
    }

    private fun processCallStatsUpdates(call: Call): Observable<Call> {
        return latestCallStreamsUpdates
            .startWith(Observable.just(
                listOf(
                    InternalCallStreamsUpdate(
                        callId = call.callId,
                        streams = CallStreams()
                    )
                )
            ))
            .flatMap { callStreamsUpdates ->

                Observable.fromIterable(callStreamsUpdates)
            }
            .filter { callStreamsUpdate ->

                callStreamsUpdate.callId == call.callId
            }
            .map { callStreamsUpdate ->

                call.copy(
                    streams = callStreamsUpdate.streams
                )
            }
    }

    private fun Observable<Call>.takeUntilCallOver(callId: CallId): Observable<CallUpdate> {
        return takeUntil(
            callHistoryObserver.observeCallHistory(OffsetDateTime.now(clock))
                .flatMap { futureCallHistoryUpdate ->

                    Observable.fromIterable(futureCallHistoryUpdate)
                }
                .filter { futureCallHistoryUpdate ->

                    futureCallHistoryUpdate.resolveCallStatus().isTerminal &&
                            futureCallHistoryUpdate.callId == callId
                }
        ).map { call ->

            when (call.stage) {
                INVITATION -> CallInvitationUpdate(call = call)
                SESSION -> CallSessionUpdate(call = call)
            }
        }
    }
}
