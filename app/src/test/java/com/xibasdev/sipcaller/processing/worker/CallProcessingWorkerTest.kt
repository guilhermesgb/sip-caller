package com.xibasdev.sipcaller.processing.worker

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.ListenableWorker.Result.Failure
import androidx.work.testing.TestListenableWorkerBuilder
import com.xibasdev.sipcaller.processing.ProcessingStateNotifier
import com.xibasdev.sipcaller.sip.FakeSipEngine
import com.xibasdev.sipcaller.test.Completable.prepareInBackgroundAndWaitUpToTimeout
import com.xibasdev.sipcaller.test.Single.prepareInBackground
import com.xibasdev.sipcaller.test.Single.prepareInBackgroundAndWaitUpToTimeout
import com.xibasdev.sipcaller.test.waitUpToTimeout
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.schedulers.Schedulers
import java.util.concurrent.TimeUnit.SECONDS
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CallProcessingWorkerTest {

    private lateinit var fakeSipEngine: FakeSipEngine
    private lateinit var callProcessingWorker: CallProcessingWorker

    @Before
    fun setUp() {
        fakeSipEngine = FakeSipEngine()

        val context: Context = ApplicationProvider.getApplicationContext()
        val callProcessingWorkerFactory = CallProcessingWorkerFactory(
            Schedulers.single(),
            fakeSipEngine,
            ProcessingStateNotifier(context)
        )

        callProcessingWorker = TestListenableWorkerBuilder<CallProcessingWorker>(context)
            .setWorkerFactory(callProcessingWorkerFactory)
            .build()
    }

    @Test
    fun `call processing worker is able to start and execute indefinitely`() {
        val single = callProcessingWorker.createWork()
            .prepareInBackgroundAndWaitUpToTimeout(timeoutDuration = 10, timeoutUnit = SECONDS)

        single.assertNotComplete()
        single.assertNoValues()
        single.assertNoErrors()
    }

    @Test
    fun `call processing worker is able to detect if SIP engine gets stuck`() {
        val single = callProcessingWorker.createWork()
            .prepareInBackground()

        Completable
            .fromCallable { fakeSipEngine.simulateSipEngineStuckInCoreIteration() }
            .delaySubscription(5, SECONDS)
            .prepareInBackgroundAndWaitUpToTimeout()

        single.waitUpToTimeout(timeoutDuration = 10, timeoutUnit = SECONDS)

        single.assertComplete()
        single.assertValue { result ->

            result is Failure
        }
    }
}
