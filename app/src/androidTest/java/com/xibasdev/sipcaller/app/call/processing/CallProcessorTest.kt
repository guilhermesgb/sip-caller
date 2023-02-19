package com.xibasdev.sipcaller.app.call.processing

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import com.xibasdev.sipcaller.app.FakeWorkManagerInitializer
import com.xibasdev.sipcaller.app.WorkManagerInitializerApi
import com.xibasdev.sipcaller.app.call.processing.worker.CallProcessingWorker
import com.xibasdev.sipcaller.sip.FakeSipEngine
import com.xibasdev.sipcaller.sip.SipEngineApi
import com.xibasdev.sipcaller.test.Completable.andThenAfterDelay
import com.xibasdev.sipcaller.test.Completable.prepareInBackgroundAndWaitUpToTimeout
import com.xibasdev.sipcaller.test.Observable.prepareInBackground
import com.xibasdev.sipcaller.test.Observable.prepareInBackgroundAndWaitUpToTimeout
import com.xibasdev.sipcaller.test.waitUpToTimeout
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@HiltAndroidTest
@UninstallModules(CallProcessorDependenciesModule::class, CallProcessingWorkerModule::class)
@RunWith(AndroidJUnit4::class)
class CallProcessorTest {

    @Module
    @InstallIn(SingletonComponent::class)
    interface FakeCallProcessorDependenciesModule {

        @Binds
        @Singleton
        fun bindWorkManagerInitializer(
            workManagerInitializer: FakeWorkManagerInitializer
        ): WorkManagerInitializerApi

        @Binds
        @Singleton
        fun bindSipEngine(sipEngine: FakeSipEngine): SipEngineApi
    }

    @Module
    @InstallIn(SingletonComponent::class)
    class TestCallProcessingWorkerModule {
        @Provides
        @Named("CallProcessing")
        fun provideStartCallProcessingWorkRequest(): OneTimeWorkRequest {
            return OneTimeWorkRequestBuilder<CallProcessingWorker>()
                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .build()
        }
    }

    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var callProcessor: CallProcessor

    @Before
    fun setUp() {
        hiltRule.inject()
    }

    @Test
    fun initialProcessingStateIsStopped() {
        val observable = callProcessor.observeProcessingState()
            .prepareInBackgroundAndWaitUpToTimeout()

        observable.assertNotComplete()
        observable.assertValueCount(1)
        observable.assertValueAt(0, CallProcessingStopped)
    }

    @Test
    fun processorIsAbleToStartProcessing() {
        val completable = callProcessor.startProcessing()
            .prepareInBackgroundAndWaitUpToTimeout()

        completable.assertComplete()
    }

    @Test
    fun processingStateTransitionsFromStoppedToStarted() {
        val observable = callProcessor.observeProcessingState()
            .prepareInBackground()

        callProcessor.startProcessing()
            .prepareInBackgroundAndWaitUpToTimeout()

        observable.waitUpToTimeout()

        observable.assertNotComplete()
        observable.assertValueCount(2)
        observable.assertValueAt(0, CallProcessingStopped)
        observable.assertValueAt(1, CallProcessingStarted)
    }

    @Test
    fun subsequentProcessingStartsAreNoOp() {
        val completable = callProcessor.startProcessing()
            .andThen(callProcessor.startProcessing())
            .prepareInBackgroundAndWaitUpToTimeout()

        completable.assertComplete()
    }

    @Test
    fun subsequentProcessingStartsDoNotImpactObservableProcessingState() {
        val observable = callProcessor.observeProcessingState()
            .prepareInBackground()

        callProcessor.startProcessing()
            .andThenAfterDelay(callProcessor.startProcessing())
            .prepareInBackgroundAndWaitUpToTimeout()

        observable.waitUpToTimeout()

        observable.assertNotComplete()
        observable.assertValueCount(2)
        observable.assertValueAt(0, CallProcessingStopped)
        observable.assertValueAt(1, CallProcessingStarted)
    }

    @Test
    fun processorIsAbleToStopProcessing() {
        val completable = callProcessor.startProcessing()
            .andThen(callProcessor.stopProcessing())
            .prepareInBackgroundAndWaitUpToTimeout()

        completable.assertComplete()
    }

    @Test
    fun processingStateTransitionsFromStartedToStopped() {
        val observable = callProcessor.observeProcessingState()
            .prepareInBackground()

        callProcessor.startProcessing()
            .andThenAfterDelay(callProcessor.stopProcessing())
            .prepareInBackgroundAndWaitUpToTimeout()

        observable.waitUpToTimeout()

        observable.assertNotComplete()
        observable.assertValueCount(3)
        observable.assertValueAt(0, CallProcessingStopped)
        observable.assertValueAt(1, CallProcessingStarted)
        observable.assertValueAt(2, CallProcessingStopped)
    }

    @Test
    fun subsequentProcessingStopsAreNoOp() {
        val completable = callProcessor.startProcessing()
            .andThen(callProcessor.stopProcessing())
            .andThen(callProcessor.stopProcessing())
            .prepareInBackgroundAndWaitUpToTimeout()

        completable.assertComplete()
    }

    @Test
    fun subsequentProcessingStopsDoNotImpactObservableProcessingState() {
        val observable = callProcessor.observeProcessingState()
            .prepareInBackground()

        callProcessor.startProcessing()
            .andThenAfterDelay(callProcessor.stopProcessing())
            .andThenAfterDelay(callProcessor.stopProcessing())
            .prepareInBackgroundAndWaitUpToTimeout()

        observable.waitUpToTimeout()

        observable.assertNotComplete()
        observable.assertValueCount(3)
        observable.assertValueAt(0, CallProcessingStopped)
        observable.assertValueAt(1, CallProcessingStarted)
        observable.assertValueAt(2, CallProcessingStopped)
    }

    @Test
    fun processorIsAbleToRestartProcessing() {
        val completable = callProcessor.startProcessing()
            .andThen(callProcessor.stopProcessing())
            .andThen(callProcessor.startProcessing())
            .prepareInBackgroundAndWaitUpToTimeout()

        completable.assertComplete()
    }

    @Test
    fun processingStateTransitionsFromStoppedToStartedOnceAgain() {
        val observable = callProcessor.observeProcessingState()
            .prepareInBackground()

        callProcessor.startProcessing()
            .andThenAfterDelay(callProcessor.stopProcessing())
            .andThenAfterDelay(callProcessor.startProcessing())
            .prepareInBackgroundAndWaitUpToTimeout()

        observable.waitUpToTimeout()

        observable.assertNotComplete()
        observable.assertValueCount(4)
        observable.assertValueAt(0, CallProcessingStopped)
        observable.assertValueAt(1, CallProcessingStarted)
        observable.assertValueAt(2, CallProcessingStopped)
        observable.assertValueAt(3, CallProcessingStarted)
    }

    @After
    fun tearDown() {
        callProcessor.clear()
    }
}
