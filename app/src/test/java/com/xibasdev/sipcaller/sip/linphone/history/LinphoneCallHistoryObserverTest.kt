package com.xibasdev.sipcaller.sip.linphone.history

import com.elvishew.xlog.Logger
import com.elvishew.xlog.XLog
import com.xibasdev.sipcaller.sip.calling.CallDirection.INCOMING
import com.xibasdev.sipcaller.sip.calling.CallDirection.OUTGOING
import com.xibasdev.sipcaller.sip.calling.CallId
import com.xibasdev.sipcaller.sip.calling.streams.CallStreams
import com.xibasdev.sipcaller.sip.calling.streams.MediaStream
import com.xibasdev.sipcaller.sip.history.CallHistoryObserverApi
import com.xibasdev.sipcaller.sip.history.CallInvitationAccepted
import com.xibasdev.sipcaller.sip.history.CallInvitationCanceled
import com.xibasdev.sipcaller.sip.history.CallInvitationDeclined
import com.xibasdev.sipcaller.sip.history.CallInvitationDetected
import com.xibasdev.sipcaller.sip.history.CallInvitationMissed
import com.xibasdev.sipcaller.sip.history.CallInviteAcceptedElsewhere
import com.xibasdev.sipcaller.sip.linphone.LinphoneSipEngine
import com.xibasdev.sipcaller.sip.linphone.calling.details.LinphoneCallDetailsObserver
import com.xibasdev.sipcaller.sip.linphone.calling.features.LinphoneCallFeaturesManager
import com.xibasdev.sipcaller.sip.linphone.calling.state.LinphoneCallStateManager
import com.xibasdev.sipcaller.sip.linphone.context.FakeLinphoneContext
import com.xibasdev.sipcaller.sip.linphone.identity.LinphoneIdentityResolver
import com.xibasdev.sipcaller.sip.linphone.processing.LinphoneProcessingEngine
import com.xibasdev.sipcaller.sip.linphone.registering.LinphoneAccountRegistry
import com.xibasdev.sipcaller.sip.processing.ProcessingEngineApi
import com.xibasdev.sipcaller.test.Completable.prepareInForeground
import com.xibasdev.sipcaller.test.Completable.simulateAfterDelay
import com.xibasdev.sipcaller.test.History.createLocalAccountFromLocalIdentity
import com.xibasdev.sipcaller.test.History.createRemoteAccount
import com.xibasdev.sipcaller.test.Observable.prepareInForeground
import com.xibasdev.sipcaller.test.TEST_SCHEDULER
import com.xibasdev.sipcaller.test.XLogRule
import com.xibasdev.sipcaller.test.simulateWaitUpToTimeout
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Observable
import java.time.Clock
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId
import java.util.concurrent.TimeUnit.MILLISECONDS
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class LinphoneCallHistoryObserverTest {

    @get:Rule
    val xLogRule = XLogRule()

    private lateinit var logger: Logger
    private lateinit var clock: Clock
    private lateinit var linphoneContext: FakeLinphoneContext
    private lateinit var processingEngine: ProcessingEngineApi
    private lateinit var callHistoryObserver: CallHistoryObserverApi

    @Before
    fun setUp() {
        logger = XLog.tag("LinphoneCallHistoryObserverTest").build()
        clock = Clock.fixed(Instant.now(), ZoneId.systemDefault())

        linphoneContext = FakeLinphoneContext(TEST_SCHEDULER)

        val processingEngine = LinphoneProcessingEngine(
            linphoneContext, logger
        )
        val accountRegistry = LinphoneAccountRegistry(
            TEST_SCHEDULER, linphoneContext, logger
        )
        val identityResolver = LinphoneIdentityResolver(
            TEST_SCHEDULER, linphoneContext, accountRegistry, logger
        )
        val callHistoryObserver = LinphoneCallHistoryObserver(
            linphoneContext, identityResolver, logger, clock
        )
        val callDetailsObserver = LinphoneCallDetailsObserver(
            linphoneContext, callHistoryObserver, logger, clock
        )
        val callStateManager = LinphoneCallStateManager(
            TEST_SCHEDULER, linphoneContext, logger
        )
        val callFeaturesManager = LinphoneCallFeaturesManager(
            TEST_SCHEDULER, linphoneContext, callDetailsObserver
        )
        val sipEngine = LinphoneSipEngine(
            processingEngine, accountRegistry, identityResolver, callHistoryObserver,
            callDetailsObserver, callStateManager, callFeaturesManager
        )

        this.processingEngine = sipEngine
        this.callHistoryObserver = sipEngine
    }

    @Test
    fun `before engine is started, call history updates cannot be observed`() {
        val observable = callHistoryObserver
            .observeCallHistory(OffsetDateTime.now(clock).minusSeconds(5))
            .prepareInForeground()

        Observable.interval(250, MILLISECONDS, TEST_SCHEDULER)
            .flatMapCompletable {

                processingEngine.processEngineSteps()
            }
            .prepareInForeground()

        observable.simulateWaitUpToTimeout()

        observable.assertNotComplete()
        observable.assertValueCount(1)
        observable.assertValueAt(0, emptyList())
    }

    @Test
    fun `after engine is started, call history updates can be observed`() {
        val observable = callHistoryObserver
            .observeCallHistory(OffsetDateTime.now(clock).minusSeconds(5))
            .prepareInForeground()

        processingEngine.startEngine()
            .prepareInForeground()

        Observable.interval(250, MILLISECONDS, TEST_SCHEDULER)
            .flatMapCompletable {

                processingEngine.processEngineSteps()
            }
            .prepareInForeground()

        observable.simulateWaitUpToTimeout()

        observable.assertNotComplete()
        observable.assertValueCount(1)
        observable.assertValueAt(0, emptyList())
    }

    @Test
    fun `after engine is started twice, call history updates are not observed twice`() {
        val observable = callHistoryObserver
            .observeCallHistory(OffsetDateTime.now(clock).minusSeconds(5))
            .prepareInForeground()

        processingEngine.startEngine()
            .andThen(processingEngine.startEngine())
            .prepareInForeground()

        Observable.interval(250, MILLISECONDS, TEST_SCHEDULER)
            .flatMapCompletable {

                processingEngine.processEngineSteps()
            }
            .prepareInForeground()

        observable.simulateWaitUpToTimeout()

        observable.assertNotComplete()
        observable.assertValueCount(1)
        observable.assertValueAt(0, emptyList())
    }

    @Test
    fun `after engine is stopped, previous call history updates can still be observed`() {
        val observable = processingEngine.startEngine()
            .andThen(Completable.fromCallable {
                linphoneContext.simulateLinphoneCoreStop()
            })
            .andThen(callHistoryObserver
            .observeCallHistory(OffsetDateTime.now(clock).minusSeconds(5)))
            .prepareInForeground()

        Observable.interval(250, MILLISECONDS, TEST_SCHEDULER)
            .flatMapCompletable {

                processingEngine.processEngineSteps()
            }
            .prepareInForeground()

        observable.simulateWaitUpToTimeout()

        observable.assertNotComplete()
        observable.assertValueCount(1)
        observable.assertValueAt(0, emptyList())
    }

    @Test
    fun `after engine is restarted, previous call history remains observable the same`() {
        val observable = processingEngine.startEngine()
            .andThen(Completable.fromCallable {
                linphoneContext.simulateLinphoneCoreStop()
            })
            .andThen(processingEngine.startEngine())
            .andThen(callHistoryObserver
            .observeCallHistory(OffsetDateTime.now(clock).minusSeconds(5)))
            .prepareInForeground()

        Observable.interval(250, MILLISECONDS, TEST_SCHEDULER)
            .flatMapCompletable {

                processingEngine.processEngineSteps()
            }
            .prepareInForeground()

        observable.simulateWaitUpToTimeout()

        observable.assertNotComplete()
        observable.assertValueCount(1)
        observable.assertValueAt(0, emptyList())
    }

    @Test
    fun `after engine is started, incoming call that was received earlier is observed`() {
        linphoneContext.simulateIncomingCallInvitationArrived()

        val observable = callHistoryObserver
            .observeCallHistory(OffsetDateTime.now(clock).minusSeconds(5))
            .prepareInForeground()

        processingEngine.startEngine().simulateAfterDelay()
            .prepareInForeground()

        Observable.interval(250, MILLISECONDS, TEST_SCHEDULER)
            .flatMapCompletable {

                processingEngine.processEngineSteps()
            }
            .prepareInForeground()

        observable.simulateWaitUpToTimeout()

        observable.assertNotComplete()
        observable.assertValueCount(2)
        observable.assertValueAt(0, emptyList())
        observable.assertValueAt(1, listOf(
            CallInvitationDetected(
                callId = CallId("1"),
                callDirection = INCOMING,
                timestamp = OffsetDateTime.now(clock),
                localAccount = createLocalAccountFromLocalIdentity(),
                remoteAccount = createRemoteAccount(),
                streams = CallStreams(
                    audio = MediaStream(),
                    video = MediaStream()
                )
            )
        ))
    }

    @Test
    fun `after engine is started, outgoing call that was later sent is observed`() {
        val observable = callHistoryObserver
            .observeCallHistory(OffsetDateTime.now(clock).minusSeconds(5))
            .prepareInForeground()

        processingEngine.startEngine()
            .andThen(Completable.fromCallable {
                linphoneContext.simulateOutgoingCallInvitationSent()
            })
            .simulateAfterDelay()
            .prepareInForeground()

        Observable.interval(250, MILLISECONDS, TEST_SCHEDULER)
            .flatMapCompletable {

                processingEngine.processEngineSteps()
            }
            .prepareInForeground()

        observable.simulateWaitUpToTimeout()

        observable.assertNotComplete()
        observable.assertValueCount(2)
        observable.assertValueAt(0, emptyList())
        observable.assertValueAt(1, listOf(
            CallInvitationDetected(
                callId = CallId("1"),
                callDirection = OUTGOING,
                timestamp = OffsetDateTime.now(clock),
                localAccount = createLocalAccountFromLocalIdentity(),
                remoteAccount = createRemoteAccount(),
                streams = CallStreams(
                    audio = MediaStream(),
                    video = MediaStream()
                )
            )
        ))
    }

    @Test
    fun `after declining call invitation, proper update can be observed`() {
        val observable = callHistoryObserver
            .observeCallHistory(OffsetDateTime.now(clock).minusSeconds(5))
            .prepareInForeground()

        processingEngine.startEngine()
            .andThen(Completable.fromCallable {
                linphoneContext.simulateIncomingCallInvitationArrived()
            })
            .simulateAfterDelay()
            .andThen(Completable.fromCallable {
                linphoneContext.simulateDeclineIncomingCallInvitation("1")
            })
            .simulateAfterDelay()
            .prepareInForeground()

        Observable.interval(250, MILLISECONDS, TEST_SCHEDULER)
            .flatMapCompletable {

                processingEngine.processEngineSteps()
            }
            .prepareInForeground()

        observable.simulateWaitUpToTimeout()

        observable.assertNotComplete()
        observable.assertValueCount(3)
        observable.assertValueAt(0, emptyList())
        observable.assertValueAt(1, listOf(
            CallInvitationDetected(
                callId = CallId("1"),
                callDirection = INCOMING,
                timestamp = OffsetDateTime.now(clock),
                localAccount = createLocalAccountFromLocalIdentity(),
                remoteAccount = createRemoteAccount(),
                streams = CallStreams(
                    audio = MediaStream(),
                    video = MediaStream()
                )
            )
        ))
        observable.assertValueAt(2, listOf(
            CallInvitationDeclined(
                callId = CallId("1"),
                callDirection = INCOMING,
                timestamp = OffsetDateTime.now(clock),
                localAccount = createLocalAccountFromLocalIdentity(),
                remoteAccount = createRemoteAccount()
            )
        ))
    }

    @Test
    fun `after accepting call invitation, proper update can be observed`() {
        val observable = callHistoryObserver
            .observeCallHistory(OffsetDateTime.now(clock).minusSeconds(5))
            .prepareInForeground()

        processingEngine.startEngine()
            .andThen(Completable.fromCallable {
                linphoneContext.simulateIncomingCallInvitationArrived()
            })
            .simulateAfterDelay()
            .andThen(Completable.fromCallable {
                linphoneContext.simulateAcceptIncomingCallInvitation("1")
            })
            .simulateAfterDelay()
            .prepareInForeground()

        Observable.interval(250, MILLISECONDS, TEST_SCHEDULER)
            .flatMapCompletable {

                processingEngine.processEngineSteps()
            }
            .prepareInForeground()

        observable.simulateWaitUpToTimeout()

        observable.assertNotComplete()
        observable.assertValueCount(3)
        observable.assertValueAt(0, emptyList())
        observable.assertValueAt(1, listOf(
            CallInvitationDetected(
                callId = CallId("1"),
                callDirection = INCOMING,
                timestamp = OffsetDateTime.now(clock),
                localAccount = createLocalAccountFromLocalIdentity(),
                remoteAccount = createRemoteAccount(),
                streams = CallStreams(
                    audio = MediaStream(),
                    video = MediaStream()
                )
            )
        ))
        observable.assertValueAt(2, listOf(
            CallInvitationAccepted(
                callId = CallId("1"),
                callDirection = INCOMING,
                timestamp = OffsetDateTime.now(clock),
                localAccount = createLocalAccountFromLocalIdentity(),
                remoteAccount = createRemoteAccount(),
                streams = CallStreams(
                    audio = MediaStream(),
                    video = MediaStream()
                )
            )
        ))
    }

    @Test
    fun `when call invitation is missed, proper update can be observed`() {
        val observable = callHistoryObserver
            .observeCallHistory(OffsetDateTime.now(clock).minusSeconds(5))
            .prepareInForeground()

        processingEngine.startEngine()
            .andThen(Completable.fromCallable {
                linphoneContext.simulateIncomingCallInvitationArrived()
            })
            .simulateAfterDelay()
            .andThen(Completable.fromCallable {
                linphoneContext.simulateMissIncomingCallInvitation("1")
            })
            .simulateAfterDelay()
            .prepareInForeground()

        Observable.interval(250, MILLISECONDS, TEST_SCHEDULER)
            .flatMapCompletable {

                processingEngine.processEngineSteps()
            }
            .prepareInForeground()

        observable.simulateWaitUpToTimeout()

        observable.assertNotComplete()
        observable.assertValueCount(3)
        observable.assertValueAt(0, emptyList())
        observable.assertValueAt(1, listOf(
            CallInvitationDetected(
                callId = CallId("1"),
                callDirection = INCOMING,
                timestamp = OffsetDateTime.now(clock),
                localAccount = createLocalAccountFromLocalIdentity(),
                remoteAccount = createRemoteAccount(),
                streams = CallStreams(
                    audio = MediaStream(),
                    video = MediaStream()
                )
            )
        ))
        observable.assertValueAt(2, listOf(
            CallInvitationMissed(
                callId = CallId("1"),
                callDirection = INCOMING,
                timestamp = OffsetDateTime.now(clock),
                localAccount = createLocalAccountFromLocalIdentity(),
                remoteAccount = createRemoteAccount()
            )
        ))
    }

    @Test
    fun `when call invitation is accepted elsewhere, proper update can be observed`() {
        val observable = callHistoryObserver
            .observeCallHistory(OffsetDateTime.now(clock).minusSeconds(5))
            .prepareInForeground()

        processingEngine.startEngine()
            .andThen(Completable.fromCallable {
                linphoneContext.simulateIncomingCallInvitationArrived()
            })
            .simulateAfterDelay()
            .andThen(Completable.fromCallable {
                linphoneContext.simulateIncomingCallInvitationAcceptedElsewhere("1")
            })
            .simulateAfterDelay()
            .prepareInForeground()

        Observable.interval(250, MILLISECONDS, TEST_SCHEDULER)
            .flatMapCompletable {

                processingEngine.processEngineSteps()
            }
            .prepareInForeground()

        observable.simulateWaitUpToTimeout()

        observable.assertNotComplete()
        observable.assertValueCount(3)
        observable.assertValueAt(0, emptyList())
        observable.assertValueAt(1, listOf(
            CallInvitationDetected(
                callId = CallId("1"),
                callDirection = INCOMING,
                timestamp = OffsetDateTime.now(clock),
                localAccount = createLocalAccountFromLocalIdentity(),
                remoteAccount = createRemoteAccount(),
                streams = CallStreams(
                    audio = MediaStream(),
                    video = MediaStream()
                )
            )
        ))
        observable.assertValueAt(2, listOf(
            CallInviteAcceptedElsewhere(
                callId = CallId("1"),
                callDirection = INCOMING,
                timestamp = OffsetDateTime.now(clock),
                localAccount = createLocalAccountFromLocalIdentity(),
                remoteAccount = createRemoteAccount()
            )
        ))
    }

    @Test
    fun `when call invitation is canceled by caller, proper update can be observed`() {
        val observable = callHistoryObserver
            .observeCallHistory(OffsetDateTime.now(clock).minusSeconds(5))
            .prepareInForeground()

        processingEngine.startEngine()
            .andThen(Completable.fromCallable {
                linphoneContext.simulateIncomingCallInvitationArrived()
            })
            .simulateAfterDelay()
            .andThen(Completable.fromCallable {
                linphoneContext.simulateIncomingCallCanceledByCaller("1")
            })
            .simulateAfterDelay()
            .prepareInForeground()

        Observable.interval(250, MILLISECONDS, TEST_SCHEDULER)
            .flatMapCompletable {

                processingEngine.processEngineSteps()
            }
            .prepareInForeground()

        observable.simulateWaitUpToTimeout()

        observable.assertNotComplete()
        observable.assertValueCount(3)
        observable.assertValueAt(0, emptyList())
        observable.assertValueAt(1, listOf(
            CallInvitationDetected(
                callId = CallId("1"),
                callDirection = INCOMING,
                timestamp = OffsetDateTime.now(clock),
                localAccount = createLocalAccountFromLocalIdentity(),
                remoteAccount = createRemoteAccount(),
                streams = CallStreams(
                    audio = MediaStream(),
                    video = MediaStream()
                )
            )
        ))
        observable.assertValueAt(2, listOf(
            CallInvitationCanceled(
                callId = CallId("1"),
                callDirection = INCOMING,
                timestamp = OffsetDateTime.now(clock),
                localAccount = createLocalAccountFromLocalIdentity(),
                remoteAccount = createRemoteAccount()
            )
        ))
    }

    @Test
    fun `when call invitation is canceled by callee, proper update can be observed`() {
        val observable = callHistoryObserver
            .observeCallHistory(OffsetDateTime.now(clock).minusSeconds(5))
            .prepareInForeground()

        processingEngine.startEngine()
            .andThen(Completable.fromCallable {
                linphoneContext.simulateIncomingCallInvitationArrived()
            })
            .simulateAfterDelay()
            .andThen(Completable.fromCallable {
                linphoneContext.simulateIncomingCallCanceledByCallee("1")
            })
            .simulateAfterDelay()
            .prepareInForeground()

        Observable.interval(250, MILLISECONDS, TEST_SCHEDULER)
            .flatMapCompletable {

                processingEngine.processEngineSteps()
            }
            .prepareInForeground()

        observable.simulateWaitUpToTimeout()

        observable.assertNotComplete()
        observable.assertValueCount(3)
        observable.assertValueAt(0, emptyList())
        observable.assertValueAt(1, listOf(
            CallInvitationDetected(
                callId = CallId("1"),
                callDirection = INCOMING,
                timestamp = OffsetDateTime.now(clock),
                localAccount = createLocalAccountFromLocalIdentity(),
                remoteAccount = createRemoteAccount(),
                streams = CallStreams(
                    audio = MediaStream(),
                    video = MediaStream()
                )
            )
        ))
        observable.assertValueAt(2, listOf(
            CallInvitationDeclined(
                callId = CallId("1"),
                callDirection = INCOMING,
                timestamp = OffsetDateTime.now(clock),
                localAccount = createLocalAccountFromLocalIdentity(),
                remoteAccount = createRemoteAccount()
            )
        ))
    }
}
