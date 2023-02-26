package com.xibasdev.sipcaller.sip.linphone.processing

import com.elvishew.xlog.Logger
import com.elvishew.xlog.XLog
import com.xibasdev.sipcaller.sip.linphone.LinphoneSipEngine
import com.xibasdev.sipcaller.sip.linphone.context.FakeLinphoneContext
import com.xibasdev.sipcaller.sip.linphone.history.LinphoneCallHistoryObserver
import com.xibasdev.sipcaller.sip.processing.ProcessingEngineApi
import com.xibasdev.sipcaller.sip.processing.ProcessingEngineProcessingFailed
import com.xibasdev.sipcaller.sip.processing.ProcessingEngineStartFailedAsync
import com.xibasdev.sipcaller.sip.processing.ProcessingEngineStartFailedSync
import com.xibasdev.sipcaller.test.Completable.prepareInForeground
import com.xibasdev.sipcaller.test.TEST_SCHEDULER
import com.xibasdev.sipcaller.test.XLogRule
import com.xibasdev.sipcaller.test.simulateWaitUpToTimeout
import io.reactivex.rxjava3.core.Observable
import java.util.concurrent.TimeUnit.MILLISECONDS
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class LinphoneProcessingEngineTest {

    @get:Rule
    val xLogRule = XLogRule()

    private lateinit var logger: Logger
    private lateinit var linphoneContext: FakeLinphoneContext
    private lateinit var processingEngine: ProcessingEngineApi

    @Before
    fun setUp() {
        logger = XLog.tag("LinphoneProcessingEngineTest").build()
        linphoneContext = FakeLinphoneContext()

        processingEngine = LinphoneSipEngine(
            LinphoneProcessingEngine(linphoneContext, logger),
            LinphoneCallHistoryObserver(linphoneContext, logger)
        )
    }

    @Test
    fun `engine can be started, it waits until Linphone starts`() {
        val completable = processingEngine.startEngine()
            .prepareInForeground()

        Observable.interval(250, MILLISECONDS, TEST_SCHEDULER)
            .flatMapCompletable {

                processingEngine.processEngineSteps()
            }
            .prepareInForeground()

        completable.simulateWaitUpToTimeout()

        completable.assertComplete()
    }

    @Test
    fun `engine can be started twice, second time is no-op`() {
        val completable = processingEngine.startEngine()
            .andThen(processingEngine.startEngine())
            .prepareInForeground()

        Observable.interval(250, MILLISECONDS, TEST_SCHEDULER)
            .flatMapCompletable {

                processingEngine.processEngineSteps()
            }
            .prepareInForeground()

        completable.simulateWaitUpToTimeout()

        completable.assertComplete()
    }

    @Test
    fun `engine propagates failure if Linphone synchronously fails to start`() {
        linphoneContext.failSynchronouslyOnLinphoneCoreStart()

        val completable = processingEngine.startEngine()
            .prepareInForeground()

        Observable.interval(250, MILLISECONDS, TEST_SCHEDULER)
            .flatMapCompletable {

                processingEngine.processEngineSteps()
            }
            .prepareInForeground()

        completable.simulateWaitUpToTimeout()

        completable.assertNotComplete()
        completable.assertError(ProcessingEngineStartFailedSync::class.java)
    }

    @Test
    fun `engine propagates failure if Linphone asynchronously fails to start`() {
        linphoneContext.failAsynchronouslyOnLinphoneCoreStart()

        val completable = processingEngine.startEngine()
            .prepareInForeground()

        Observable.interval(250, MILLISECONDS, TEST_SCHEDULER)
            .flatMapCompletable {

                processingEngine.processEngineSteps()
            }
            .prepareInForeground()

        completable.simulateWaitUpToTimeout()

        completable.assertNotComplete()
        completable.assertError(ProcessingEngineStartFailedAsync::class.java)
    }

    @Test
    fun `engine propagates failure if Linphone fails while iterating core`() {
        linphoneContext.failSynchronouslyOnLinphoneCoreIterate()

        processingEngine.startEngine()
            .prepareInForeground()

        val completable = Observable.interval(250, MILLISECONDS, TEST_SCHEDULER)
            .flatMapCompletable {

                processingEngine.processEngineSteps()
            }
            .prepareInForeground()

        completable.simulateWaitUpToTimeout()

        completable.assertNotComplete()
        completable.assertError(ProcessingEngineProcessingFailed::class.java)
    }
}
