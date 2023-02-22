package com.xibasdev.sipcaller.processing.worker

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.testing.TestListenableWorkerBuilder
import com.xibasdev.sipcaller.processing.notifier.CallStateNotifier
import com.xibasdev.sipcaller.processing.worker.CallProcessingWorker
import com.xibasdev.sipcaller.processing.worker.CallProcessingWorkerFactory
import com.xibasdev.sipcaller.sip.FakeSipEngine
import com.xibasdev.sipcaller.test.Single.prepareInBackgroundAndWaitUpToTimeout
import io.reactivex.rxjava3.schedulers.Schedulers
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CallProcessingWorkerTest {

    private lateinit var callProcessingWorker: CallProcessingWorker

    @Before
    fun setUp() {
        val context: Context = ApplicationProvider.getApplicationContext()
        val callProcessingWorkerFactory = CallProcessingWorkerFactory(
            Schedulers.newThread(),
            FakeSipEngine(),
            CallStateNotifier(context)
        )

        callProcessingWorker = TestListenableWorkerBuilder<CallProcessingWorker>(context)
            .setWorkerFactory(callProcessingWorkerFactory)
            .build()
    }

    @Test
    fun `call processing worker is able to start and execute indefinitely`() {
        val single = callProcessingWorker.createWork()
            .prepareInBackgroundAndWaitUpToTimeout()

        single.assertNotComplete()
        single.assertNoValues()
        single.assertNoErrors()
    }
}
