package com.xibasdev.sipcaller.sip.linphone.history

import com.elvishew.xlog.Logger
import com.xibasdev.sipcaller.sip.SipCallErrorReason
import com.xibasdev.sipcaller.sip.calling.CallDirection
import com.xibasdev.sipcaller.sip.calling.CallDirection.INCOMING
import com.xibasdev.sipcaller.sip.calling.CallDirection.OUTGOING
import com.xibasdev.sipcaller.sip.calling.CallId
import com.xibasdev.sipcaller.sip.calling.streams.CallStreams
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
import com.xibasdev.sipcaller.sip.history.ConditionalCallSessionFinishedUpdate
import com.xibasdev.sipcaller.sip.identity.LocalIdentity
import com.xibasdev.sipcaller.sip.identity.RemoteIdentity
import com.xibasdev.sipcaller.sip.identity.UnreachableIdentity
import com.xibasdev.sipcaller.sip.linphone.context.LinphoneAccountAddress
import com.xibasdev.sipcaller.sip.linphone.context.LinphoneCallStateChange
import com.xibasdev.sipcaller.sip.linphone.context.LinphoneCallStream
import com.xibasdev.sipcaller.sip.linphone.context.LinphoneContextApi
import com.xibasdev.sipcaller.sip.linphone.identity.LinphoneIdentityResolver
import com.xibasdev.sipcaller.sip.linphone.utils.resolveMediaStream
import com.xibasdev.sipcaller.sip.protocol.DefinedPort
import com.xibasdev.sipcaller.sip.protocol.ProtocolInfo
import com.xibasdev.sipcaller.sip.protocol.ProtocolType.TCP
import com.xibasdev.sipcaller.sip.registering.account.AccountDisplayName
import com.xibasdev.sipcaller.sip.registering.account.AccountInfo
import com.xibasdev.sipcaller.sip.registering.account.AccountUsername
import com.xibasdev.sipcaller.sip.registering.account.EMPTY_DISPLAY_NAME
import com.xibasdev.sipcaller.sip.registering.account.UNKNOWN_USERNAME
import com.xibasdev.sipcaller.sip.registering.account.address.AccountDomain
import com.xibasdev.sipcaller.sip.registering.account.address.AccountDomainAddress
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.subjects.BehaviorSubject
import io.reactivex.rxjava3.subjects.ReplaySubject
import java.time.Clock
import java.time.OffsetDateTime
import java.util.TreeMap
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton
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

@Singleton
class LinphoneCallHistoryObserver @Inject constructor(
    private val linphoneContext: LinphoneContextApi,
    private val identityResolver: LinphoneIdentityResolver,
    @Named("SipEngineLogger") private val logger: Logger,
    @Named("LinphoneSipEngineClock") private val clock: Clock
) : CallHistoryObserverApi {

    private val callStateChangeListenerId = linphoneContext.createCallStateChangeListener {
            callStateChange, _ ->

        with (callStateChange) {
            processCallStateChange()
        }
    }

    private val currentCallHistoryUpdates = TreeMap<String, CallHistoryUpdate>()
    private val latestCallHistoryUpdates = BehaviorSubject
        .create<(AccountInfo) -> CallHistoryUpdate>()

    private val callHistory = ReplaySubject.create<List<CallHistoryUpdate>>().apply {
        processCallHistoryUpdates(this)
    }

    override fun observeCallHistory(offset: OffsetDateTime): Observable<List<CallHistoryUpdate>> {
        return callHistory
            .flatMap { updates ->

                val filteredUpdates = updates.flatMap { update ->

                    if (update.timestamp.isAfter(offset)) {
                        listOf(update)

                    } else {
                        emptyList()
                    }
                }

                if (filteredUpdates.isNotEmpty()) {
                    Observable.just(filteredUpdates)

                } else {
                    Observable.empty()
                }
            }
            .startWithItem(callHistory.value ?: emptyList())
            .distinctUntilChanged { previous, next ->

                if (previous.size != next.size) {
                    return@distinctUntilChanged false
                }

                val previousSorted = previous.sortedBy { it.timestamp }
                val nextSorted = next.sortedBy { it.timestamp }

                previousSorted.foldIndexed(true) {
                        index, unchanged, previousUpdate ->

                    val nextUpdate = nextSorted[index]

                    unchanged && previousUpdate.callId == nextUpdate.callId &&
                            ((previousUpdate.javaClass.name == nextUpdate.javaClass.name))
                }
            }
    }

    context (LinphoneCallStateChange)
    private fun processCallStateChange() {
        when (state) {
            IncomingReceived,
            OutgoingProgress -> postCallHistoryUpdate(::CallInvitationDetected)
            StreamsRunning -> ifPreviousHistoryUpdateIs(CallInvitationDetected::class.java) {
                postCallHistoryUpdate(::CallInvitationAccepted)
            }
            Error -> {
                ifPreviousHistoryUpdateIs(CallInvitationDetected::class.java) {
                    postCallFailedUpdate(::CallInvitationFailed)
                }

                ifPreviousHistoryUpdateIs(CallInvitationAccepted::class.java) {
                    postCallFailedUpdate(::CallSessionFailed)
                }
            }
            End -> when (status) {
                Aborted -> when (direction) {
                    Incoming -> ifPreviousHistoryUpdateIs(CallInvitationDetected::class.java) {
                        postConditionalCallHistoryUpdate(
                            ::CallInvitationDeclined,
                            ::CallInvitationCanceled
                        )
                    }
                    Outgoing -> ifPreviousHistoryUpdateIs(CallInvitationDetected::class.java) {
                        postConditionalCallHistoryUpdate(
                            ::CallInvitationCanceled,
                            ::CallInvitationDeclined
                        )
                    }
                }
                Missed -> ifPreviousHistoryUpdateIs(CallInvitationDetected::class.java) {
                    postCallHistoryUpdate(::CallInvitationMissed)
                }
                Declined,
                DeclinedElsewhere -> ifPreviousHistoryUpdateIs(CallInvitationDetected::class.java) {
                    postCallHistoryUpdate(::CallInvitationDeclined)
                }
                AcceptedElsewhere -> ifPreviousHistoryUpdateIs(CallInvitationDetected::class.java) {
                    postCallHistoryUpdate(::CallInviteAcceptedElsewhere)
                }
                Success -> when (direction) {
                    Incoming -> ifPreviousHistoryUpdateIs(CallInvitationAccepted::class.java) {
                        postConditionalCallHistoryUpdate(
                            ::CallSessionFinishedByCallee,
                            ::CallSessionFinishedByCaller
                        )
                    }
                    Outgoing -> ifPreviousHistoryUpdateIs(CallInvitationAccepted::class.java) {
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
        updateClass: Class<T>,
        contextualFunction: () -> Unit
    ) {

        if (thereIsPreviousHistoryUpdate()) {
            getPreviousHistoryUpdate()?.let { previousHistoryUpdate ->

                if (previousHistoryUpdate::class.java == updateClass) {
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
        updateCreator: (
            callId: CallId,
            direction: CallDirection,
            timestamp: OffsetDateTime,
            localAccount: AccountInfo,
            remoteAccount: AccountInfo,
            streams: CallStreams
        ) -> T
    ) {

        latestCallHistoryUpdates.onNext { localAccount ->

            val update = updateCreator(
                createCallId(callId),
                createCallDirection(direction),
                OffsetDateTime.now(clock),
                localAccount,
                createRemoteAccount(remoteAccountAddress),
                createCallStreams(audioStream, videoStream)
            )
            currentCallHistoryUpdates[callId] = update
            update
        }
    }

    context (LinphoneCallStateChange)
    private fun <T : CallHistoryUpdate> postCallHistoryUpdate(
        updateCreator: (
            callId: CallId,
            direction: CallDirection,
            timestamp: OffsetDateTime,
            localAccount: AccountInfo,
            remoteAccount: AccountInfo
        ) -> T
    ) {

        latestCallHistoryUpdates.onNext { localAccount ->

            val update = updateCreator(
                createCallId(callId),
                createCallDirection(direction),
                OffsetDateTime.now(clock),
                localAccount,
                createRemoteAccount(remoteAccountAddress)
            )
            currentCallHistoryUpdates[callId] = update

            update
        }
    }

    context (LinphoneCallStateChange)
    private fun <T : CallHistoryUpdate> postConditionalCallHistoryUpdate(
        updateCreatorIfCallFinishedLocally: (
            callId: CallId,
            direction: CallDirection,
            timestamp: OffsetDateTime,
            localAccount: AccountInfo,
            remoteAccount: AccountInfo
        ) -> T,
        updateCreatorIfCallFinishedRemotely: (
            callId: CallId,
            direction: CallDirection,
            timestamp: OffsetDateTime,
            localAccount: AccountInfo,
            remoteAccount: AccountInfo
        ) -> T,
    ) {

        latestCallHistoryUpdates.onNext { localAccount ->

            val callId = createCallId(callId)
            val direction = createCallDirection(direction)
            val timestamp = OffsetDateTime.now(clock)

            val remoteAccount = createRemoteAccount(remoteAccountAddress)

            val updateIfFinishedByLocalParty = updateCreatorIfCallFinishedLocally(
                callId, direction, timestamp, localAccount, remoteAccount
            )
            val updateIfFinishedByRemoteParty = updateCreatorIfCallFinishedRemotely(
                callId, direction, timestamp, localAccount, remoteAccount
            )

            val conditionalUpdate = ConditionalCallSessionFinishedUpdate(
                callId, direction, timestamp, localAccount, remoteAccount,
                updateIfFinishedByLocalParty, updateIfFinishedByRemoteParty
            )
            currentCallHistoryUpdates[callId.value] = conditionalUpdate

            conditionalUpdate
        }
    }

    context (LinphoneCallStateChange)
    private fun <T : CallFailedUpdate> postCallFailedUpdate(
        updateCreator: (
            callId: CallId,
            direction: CallDirection,
            errorReason: SipCallErrorReason,
            timestamp: OffsetDateTime,
            localAccount: AccountInfo,
            remoteAccount: AccountInfo
        ) -> T
    ) {

        latestCallHistoryUpdates.onNext { localAccount ->

            val update = updateCreator(
                createCallId(callId),
                createCallDirection(direction),
                createErrorReason(errorReason),
                OffsetDateTime.now(clock),
                localAccount,
                createRemoteAccount(remoteAccountAddress)
            )
            currentCallHistoryUpdates[callId] = update

            update
        }
    }

    private fun processCallHistoryUpdates(subject: ReplaySubject<List<CallHistoryUpdate>>) {
        with (linphoneContext) {
            doWhenLinphoneCoreStartsOrStops(subject) { isLinphoneCoreStarted ->

                logger.d("Call history observer detected Linphone core " +
                        (if (isLinphoneCoreStarted) "start!" else "stop!"))

                if (isLinphoneCoreStarted) {
                    enableCoreListener(callStateChangeListenerId)

                    if (subject.values.isEmpty()) {
                        subject.onNext(emptyList())
                    }

                    Observable
                        .combineLatest(
                            identityResolver.observeIdentity()
                                .filter { identityUpdate ->
                                    identityUpdate !is UnreachableIdentity
                                },
                            latestCallHistoryUpdates
                        ) { identity, update ->

                            val localAccount = when (identity) {
                                is LocalIdentity -> AccountInfo(
                                    displayName = EMPTY_DISPLAY_NAME,
                                    username = UNKNOWN_USERNAME,
                                    address = identity.address
                                )
                                is RemoteIdentity -> identity.account
                                UnreachableIdentity -> {
                                    // This should never happen, guaranteed by the filter operation
                                    //   applied to the 'identityResolver.observeIdentity()' source.
                                    throw IllegalStateException(
                                        "Unreachable identity update not filtered!"
                                    )
                                }
                            }

                            update(localAccount)
                        }
                        .flatMap { update ->

                            if (update is ConditionalCallSessionFinishedUpdate) {
                                logger.d("Process conditional call history update: $update.")

                                wasCallFinishedByLocalParty(update.callId)
                                    .flatMapObservable { wasCallFinishedByLocalParty ->

                                        if (wasCallFinishedByLocalParty) {
                                            logger.d("Call was finished by local party.")

                                            Observable.just(
                                                update.updateIfCallFinishedByLocalParty
                                            )

                                        } else {
                                            logger.d("Call was finished by remote party.")

                                            Observable.just(
                                                update.updateIfCallFinishedByRemoteParty
                                            )
                                        }
                                    }

                            } else {
                                logger.d("Process call history update: $update.")

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

    private fun createCallId(callId: String?): CallId {
        return CallId(callId.orEmpty())
    }

    private fun createCallDirection(direction: Dir?): CallDirection {
        return when (direction) {
            Incoming -> INCOMING
            else -> OUTGOING
        }
    }

    private fun createCallStreams(
        audioStream: LinphoneCallStream,
        videoStream: LinphoneCallStream
    ): CallStreams {

        return CallStreams(
            audio = resolveMediaStream(audioStream),
            video = resolveMediaStream(videoStream)
        )
    }

    private fun createErrorReason(errorReason: String?): SipCallErrorReason {
        return SipCallErrorReason(errorReason.orEmpty())
    }

    private fun createRemoteAccount(remoteAccountAddress: LinphoneAccountAddress): AccountInfo {
        return AccountInfo(
            displayName = if (remoteAccountAddress.displayName.isNotEmpty()) {
                AccountDisplayName(remoteAccountAddress.displayName)

            } else {
                EMPTY_DISPLAY_NAME
            },
            username = if (remoteAccountAddress.username.isNotEmpty()) {
                AccountUsername(remoteAccountAddress.username)

            } else {
                UNKNOWN_USERNAME
            },
            address = AccountDomainAddress(
                domain = AccountDomain(remoteAccountAddress.domain),
                protocol = ProtocolInfo(
                    type = TCP, // TODO resolve current protocol port in use
                    port = DefinedPort(remoteAccountAddress.port)
                )
            )
        )
    }
}
