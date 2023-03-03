package com.xibasdev.sipcaller.sip.linphone.history

import com.elvishew.xlog.Logger
import com.elvishew.xlog.XLog
import com.xibasdev.sipcaller.sip.SipCallDirection.INCOMING
import com.xibasdev.sipcaller.sip.SipCallDirection.OUTGOING
import com.xibasdev.sipcaller.sip.SipCallId
import com.xibasdev.sipcaller.sip.history.CallHistoryObserverApi
import com.xibasdev.sipcaller.sip.history.CallInvitationAccepted
import com.xibasdev.sipcaller.sip.history.CallInvitationCanceled
import com.xibasdev.sipcaller.sip.history.CallInvitationDeclined
import com.xibasdev.sipcaller.sip.history.CallInvitationDetected
import com.xibasdev.sipcaller.sip.history.CallInvitationMissed
import com.xibasdev.sipcaller.sip.history.CallInviteAcceptedElsewhere
import com.xibasdev.sipcaller.sip.linphone.LinphoneSipEngine
import com.xibasdev.sipcaller.sip.linphone.context.FakeLinphoneContext
import com.xibasdev.sipcaller.sip.linphone.processing.LinphoneProcessingEngine
import com.xibasdev.sipcaller.sip.processing.ProcessingEngineApi
import com.xibasdev.sipcaller.test.Completable.prepareInForeground
import com.xibasdev.sipcaller.test.Completable.simulateAfterDelay
import com.xibasdev.sipcaller.test.Observable.prepareInForeground
import com.xibasdev.sipcaller.test.TEST_SCHEDULER
import com.xibasdev.sipcaller.test.XLogRule
import com.xibasdev.sipcaller.test.simulateWaitUpToTimeout
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Observable
import java.util.concurrent.TimeUnit.MILLISECONDS
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class LinphoneCallHistoryObserverTest {

    @get:Rule
    val xLogRule = XLogRule()

    private lateinit var logger: Logger
    private lateinit var linphoneContext: FakeLinphoneContext
    private lateinit var processingEngine: ProcessingEngineApi
    private lateinit var callHistoryObserver: CallHistoryObserverApi

    @Before
    fun setUp() {
        logger = XLog.tag("LinphoneProcessingEngineTest").build()
        linphoneContext = FakeLinphoneContext()

        val sipEngine = LinphoneSipEngine(
            LinphoneProcessingEngine(linphoneContext, logger),
            LinphoneCallHistoryObserver(linphoneContext, logger)
        )
        processingEngine = sipEngine
        callHistoryObserver = sipEngine
    }

    @Test
    fun `before engine is started, call history updates cannot be observed`() {
        val observable = callHistoryObserver.observeCallHistory()
            .prepareInForeground()

        Observable.interval(250, MILLISECONDS, TEST_SCHEDULER)
            .flatMapCompletable {

                processingEngine.processEngineSteps()
            }
            .prepareInForeground()

        observable.simulateWaitUpToTimeout()

        observable.assertNotComplete()
        observable.assertNoValues()
        observable.assertNoErrors()
    }

    @Test
    fun `after engine is started, call history updates can be observed`() {
        val observable = callHistoryObserver.observeCallHistory()
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
        val observable = callHistoryObserver.observeCallHistory()
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
            .andThen(callHistoryObserver.observeCallHistory())
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
            .andThen(callHistoryObserver.observeCallHistory())
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

        val observable = callHistoryObserver.observeCallHistory()
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
            CallInvitationDetected(callId = SipCallId("1"), callDirection = INCOMING)
        ))
    }

    @Test
    fun `after engine is started, outgoing call that was later sent is observed`() {
        val observable = callHistoryObserver.observeCallHistory()
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
            CallInvitationDetected(callId = SipCallId("1"), callDirection = OUTGOING)
        ))
    }

    @Test
    fun `after declining call invitation, proper update can be observed`() {
        val observable = callHistoryObserver.observeCallHistory()
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
            CallInvitationDetected(callId = SipCallId("1"), callDirection = INCOMING)
        ))
        observable.assertValueAt(2, listOf(
            CallInvitationDeclined(callId = SipCallId("1"), callDirection = INCOMING)
        ))
    }

    @Test
    fun `after accepting call invitation, proper update can be observed`() {
        val observable = callHistoryObserver.observeCallHistory()
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
            CallInvitationDetected(callId = SipCallId("1"), callDirection = INCOMING)
        ))
        observable.assertValueAt(2, listOf(
            CallInvitationAccepted(callId = SipCallId("1"), callDirection = INCOMING)
        ))
    }

    @Test
    fun `when call invitation is missed, proper update can be observed`() {
        val observable = callHistoryObserver.observeCallHistory()
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
            CallInvitationDetected(callId = SipCallId("1"), callDirection = INCOMING)
        ))
        observable.assertValueAt(2, listOf(
            CallInvitationMissed(callId = SipCallId("1"), callDirection = INCOMING)
        ))
    }

    @Test
    fun `when call invitation is accepted elsewhere, proper update can be observed`() {
        val observable = callHistoryObserver.observeCallHistory()
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
            CallInvitationDetected(callId = SipCallId("1"), callDirection = INCOMING)
        ))
        observable.assertValueAt(2, listOf(
            CallInviteAcceptedElsewhere(callId = SipCallId("1"), callDirection = INCOMING)
        ))
    }

    @Test
    fun `when call invitation is canceled by caller, proper update can be observed`() {
        val observable = callHistoryObserver.observeCallHistory()
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
            CallInvitationDetected(callId = SipCallId("1"), callDirection = INCOMING)
        ))
        observable.assertValueAt(2, listOf(
            CallInvitationCanceled(callId = SipCallId("1"), callDirection = INCOMING)
        ))
    }

    @Test
    fun `when call invitation is canceled by callee, proper update can be observed`() {
        val observable = callHistoryObserver.observeCallHistory()
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
            CallInvitationDetected(callId = SipCallId("1"), callDirection = INCOMING)
        ))
        observable.assertValueAt(2, listOf(
            CallInvitationDeclined(callId = SipCallId("1"), callDirection = INCOMING)
        ))
    }
}
