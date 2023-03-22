package com.xibasdev.sipcaller.processing

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.OneTimeWorkRequest
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import androidx.work.testing.WorkManagerTestInitHelper
import com.xibasdev.sipcaller.app.FakeWorkManagerInitializer
import com.xibasdev.sipcaller.app.SipCallerAppModule
import com.xibasdev.sipcaller.app.initializers.WorkManagerInitializerApi
import com.xibasdev.sipcaller.processing.dto.CallProcessingFailed
import com.xibasdev.sipcaller.processing.dto.CallProcessingStarted
import com.xibasdev.sipcaller.processing.dto.CallProcessingStopped
import com.xibasdev.sipcaller.processing.dto.CallProcessingSuspended
import com.xibasdev.sipcaller.processing.di.CallProcessorDependenciesModule
import com.xibasdev.sipcaller.sip.FakeSipEngine
import com.xibasdev.sipcaller.sip.SipEngineApi
import com.xibasdev.sipcaller.test.Completable.andThenAfterDelay
import com.xibasdev.sipcaller.test.Completable.prepareInBackgroundAndWaitUpToTimeout
import com.xibasdev.sipcaller.test.Observable.prepareInBackground
import com.xibasdev.sipcaller.test.Observable.prepareInBackgroundAndWaitUpToTimeout
import com.xibasdev.sipcaller.test.waitUpToTimeout
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import dagger.hilt.components.SingletonComponent
import io.reactivex.rxjava3.core.Completable
import java.util.concurrent.TimeUnit.MILLISECONDS
import java.util.concurrent.TimeUnit.SECONDS
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@HiltAndroidTest
@UninstallModules(
    SipCallerAppModule::class,
    SipCallerAppModule.BindsModule::class,
    CallProcessorDependenciesModule::class
)
@RunWith(AndroidJUnit4::class)
class CallProcessorTest {

    @Module
    @InstallIn(SingletonComponent::class)
    interface FakeBindsModule {

        @Binds
        @Singleton
        fun bindWorkManagerInitializer(
            workManagerInitializer: FakeWorkManagerInitializer
        ): WorkManagerInitializerApi

        @Binds
        @Singleton
        fun bindSipEngine(sipEngine: FakeSipEngine): SipEngineApi
    }

    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var sipEngine: SipEngineApi

    @Inject
    @Named("CallProcessing")
    lateinit var startCallProcessingWorkRequest: OneTimeWorkRequest

    @Inject
    @Named("CallProcessingChecksUniqueWorkName")
    lateinit var callProcessingWorkName: String

    @Inject
    @Named("CallProcessingChecks")
    lateinit var processingChecksWorkRequest: PeriodicWorkRequest

    @Inject
    lateinit var callProcessor: CallProcessor

    @Before
    fun setUp() {
        hiltRule.inject()
    }

    @Test
    fun initialProcessingStateIsStopped() {
        val observable = callProcessor.observeProcessing()
            .prepareInBackgroundAndWaitUpToTimeout()

        println("RESULT: ${observable.values()}")
        observable.assertNotComplete()
        observable.assertValueCount(1)
        observable.assertValueAt(0, CallProcessingStopped)
    }

    @Test
    fun processorIsAbleToScheduleProcessingStartWhileNotConnectedToNetwork() {
        val completable = callProcessor.startProcessing()
            .prepareInBackgroundAndWaitUpToTimeout()

        completable.assertComplete()
    }

    @Test
    fun processorIsAbleToStartProcessingWhileConnectedToNetwork() {
        val completable = callProcessor.startProcessing()
            .andThen(simulateConnectedToNetwork())
            .prepareInBackgroundAndWaitUpToTimeout()

        completable.assertComplete()
    }

    @Test
    fun processingStateTransitionsFromStoppedToSuspendedWhileNotConnectedToNetwork() {
        val observable = callProcessor.observeProcessing()
            .prepareInBackground()

        callProcessor.startProcessing()
            .prepareInBackgroundAndWaitUpToTimeout()

        observable.waitUpToTimeout()

        println("RESULT: ${observable.values()}")
        observable.assertNotComplete()
        observable.assertValueCount(2)
        observable.assertValueAt(0, CallProcessingStopped)
        observable.assertValueAt(1, CallProcessingSuspended)
    }
    
    @Test
    fun processingStateTransitionsFromStoppedToStartedWhileConnectedToNetwork() {
        val observable = callProcessor.observeProcessing()
            .prepareInBackground()

        callProcessor.startProcessing()
            .andThen(simulateConnectedToNetwork())
            .prepareInBackgroundAndWaitUpToTimeout()
        
        observable.waitUpToTimeout()

        println("RESULT: ${observable.values()}")
        observable.assertNotComplete()
        observable.assertValueCount(3)
        observable.assertValueAt(0, CallProcessingStopped)
        observable.assertValueAt(1, CallProcessingSuspended)
        observable.assertValueAt(2, CallProcessingStarted)
    }

    @Test
    fun subsequentProcessingStartsAreNoOpWhileNotConnectedToNetwork() {
        val completable = callProcessor.startProcessing()
            .andThen(callProcessor.startProcessing())
            .prepareInBackgroundAndWaitUpToTimeout()

        completable.assertComplete()
    }

    @Test
    fun subsequentProcessingStartsAreNoOpWhileConnectedToNetwork() {
        val completable = callProcessor.startProcessing()
            .andThen(simulateConnectedToNetwork())
            .andThen(callProcessor.startProcessing())
            .prepareInBackgroundAndWaitUpToTimeout()

        completable.assertComplete()
    }
    
    @Test
    fun subsequentProcessingStartsDoNotImpactObservableProcessingStateWhileNotConnectedToNetwork() {
        val observable = callProcessor.observeProcessing()
            .prepareInBackground()

        callProcessor.startProcessing()
            .andThenAfterDelay(callProcessor.startProcessing())
            .prepareInBackgroundAndWaitUpToTimeout()

        observable.waitUpToTimeout()

        println("RESULT: ${observable.values()}")
        observable.assertNotComplete()
        observable.assertValueCount(2)
        observable.assertValueAt(0, CallProcessingStopped)
        observable.assertValueAt(1, CallProcessingSuspended)
    }

    @Test
    fun subsequentProcessingStartsDoNotImpactObservableProcessingStateWhileConnectedToNetwork() {
        val observable = callProcessor.observeProcessing()
            .prepareInBackground()

        callProcessor.startProcessing()
            .andThenAfterDelay(callProcessor.startProcessing())
            .andThen(simulateConnectedToNetwork())
            .prepareInBackgroundAndWaitUpToTimeout()
        
        observable.waitUpToTimeout()

        println("RESULT: ${observable.values()}")
        observable.assertNotComplete()
        observable.assertValueCount(3)
        observable.assertValueAt(0, CallProcessingStopped)
        observable.assertValueAt(1, CallProcessingSuspended)
        observable.assertValueAt(2, CallProcessingStarted)
    }
    
    @Test
    fun processorIsAbleToStopProcessingWhileSuspended() {
        val completable = callProcessor.startProcessing()
            .andThen(callProcessor.stopProcessing())
            .prepareInBackgroundAndWaitUpToTimeout()

        completable.assertComplete()
    }

    @Test
    fun processorIsAbleToStopProcessingWhileStarted() {
        val completable = callProcessor.startProcessing()
            .andThen(simulateConnectedToNetwork())
            .andThen(callProcessor.stopProcessing())
            .prepareInBackgroundAndWaitUpToTimeout()
        
        completable.assertComplete()
    }

    @Test
    fun processingStateTransitionsFromSuspendedToStopped() {
        val observable = callProcessor.observeProcessing()
            .prepareInBackground()

        callProcessor.startProcessing()
            .andThenAfterDelay(callProcessor.stopProcessing())
            .prepareInBackgroundAndWaitUpToTimeout()

        observable.waitUpToTimeout()

        println("RESULT: ${observable.values()}")
        observable.assertNotComplete()
        observable.assertValueCount(3)
        observable.assertValueAt(0, CallProcessingStopped)
        observable.assertValueAt(1, CallProcessingSuspended)
        observable.assertValueAt(2, CallProcessingStopped)
    }

    @Test
    fun processingStateTransitionsFromStartedToStopped() {
        val observable = callProcessor.observeProcessing()
            .prepareInBackground()

        callProcessor.startProcessing()
            .andThen(simulateConnectedToNetwork())
            .andThenAfterDelay(callProcessor.stopProcessing(), delayDuration = 500)
            .prepareInBackgroundAndWaitUpToTimeout()
        
        observable.waitUpToTimeout()

        println("RESULT: ${observable.values()}")
        observable.assertNotComplete()
        observable.assertValueCount(4)
        observable.assertValueAt(0, CallProcessingStopped)
        observable.assertValueAt(1, CallProcessingSuspended)
        observable.assertValueAt(2, CallProcessingStarted)
        observable.assertValueAt(3, CallProcessingStopped)
    }
    
    @Test
    fun subsequentProcessingStopsAreNoOpWhileNotConnectedToNetwork() {
        val completable = callProcessor.startProcessing()
            .andThen(callProcessor.stopProcessing())
            .andThen(callProcessor.stopProcessing())
            .prepareInBackgroundAndWaitUpToTimeout()

        completable.assertComplete()
    }

    @Test
    fun subsequentProcessingStopsAreNoOpWhileConnectedToNetwork() {
        val completable = callProcessor.startProcessing()
            .andThen(simulateConnectedToNetwork())
            .andThen(callProcessor.stopProcessing())
            .andThen(callProcessor.stopProcessing())
            .prepareInBackgroundAndWaitUpToTimeout()
        
        completable.assertComplete()
    }
    
    @Test
    fun subsequentProcessingStopsDoNotImpactObservableProcessingStateWhileNotConnectedToNetwork() {
        val observable = callProcessor.observeProcessing()
            .prepareInBackground()

        callProcessor.startProcessing()
            .andThenAfterDelay(callProcessor.stopProcessing())
            .andThenAfterDelay(callProcessor.stopProcessing())
            .prepareInBackgroundAndWaitUpToTimeout()

        observable.waitUpToTimeout()

        println("RESULT: ${observable.values()}")
        observable.assertNotComplete()
        observable.assertValueCount(3)
        observable.assertValueAt(0, CallProcessingStopped)
        observable.assertValueAt(1, CallProcessingSuspended)
        observable.assertValueAt(2, CallProcessingStopped)
    }

    @Test
    fun subsequentProcessingStopsDoNotImpactObservableProcessingStateWhileConnectedToNetwork() {
        val observable = callProcessor.observeProcessing()
            .prepareInBackground()

        callProcessor.startProcessing()
            .andThen(simulateConnectedToNetwork())
            .andThenAfterDelay(callProcessor.stopProcessing(), delayDuration = 500)
            .andThenAfterDelay(callProcessor.stopProcessing())
            .prepareInBackgroundAndWaitUpToTimeout()

        observable.waitUpToTimeout()

        println("RESULT: ${observable.values()}")
        observable.assertNotComplete()
        observable.assertValueCount(4)
        observable.assertValueAt(0, CallProcessingStopped)
        observable.assertValueAt(1, CallProcessingSuspended)
        observable.assertValueAt(2, CallProcessingStarted)
        observable.assertValueAt(3, CallProcessingStopped)
    }

    @Test
    fun processorIsAbleToRestartProcessingNotConnectedToNetwork() {
        val completable = callProcessor.startProcessing()
            .andThen(callProcessor.stopProcessing())
            .andThen(callProcessor.startProcessing())
            .prepareInBackgroundAndWaitUpToTimeout()

        completable.assertComplete()
    }

    @Test
    fun processorIsAbleToRestartProcessingWhileConnectedToNetwork() {
        val completable = callProcessor.startProcessing()
            .andThen(simulateConnectedToNetwork())
            .andThen(callProcessor.stopProcessing())
            .andThen(callProcessor.startProcessing())
            .prepareInBackgroundAndWaitUpToTimeout()

        completable.assertComplete()
    }

    @Test
    fun processingStateTransitionsFromStoppedToSuspendedOnceAgain() {
        val observable = callProcessor.observeProcessing()
            .prepareInBackground()

        callProcessor.startProcessing()
            .andThenAfterDelay(callProcessor.stopProcessing())
            .andThenAfterDelay(callProcessor.startProcessing())
            .prepareInBackgroundAndWaitUpToTimeout()

        observable.waitUpToTimeout()

        println("RESULT: ${observable.values()}")
        observable.assertNotComplete()
        observable.assertValueCount(4)
        observable.assertValueAt(0, CallProcessingStopped)
        observable.assertValueAt(1, CallProcessingSuspended)
        observable.assertValueAt(2, CallProcessingStopped)
        observable.assertValueAt(3, CallProcessingSuspended)
    }

    @Test
    fun processingStateTransitionsFromStoppedToStartedOnceAgain() {
        val observable = callProcessor.observeProcessing()
            .prepareInBackground()

        callProcessor.startProcessing()
            .andThenAfterDelay(callProcessor.stopProcessing(), delayDuration = 500)
            .andThenAfterDelay(callProcessor.startProcessing())
            .andThen(simulateConnectedToNetwork())
            .prepareInBackgroundAndWaitUpToTimeout()

        observable.waitUpToTimeout()

        println("RESULT: ${observable.values()}")
        observable.assertNotComplete()
        observable.assertValueCount(5)
        observable.assertValueAt(0, CallProcessingStopped)
        observable.assertValueAt(1, CallProcessingSuspended)
        observable.assertValueAt(2, CallProcessingStopped)
        observable.assertValueAt(3, CallProcessingSuspended)
        observable.assertValueAt(4, CallProcessingStarted)
    }

    @Test
    fun processingStateTransitionsFromSuspendedToFailed() {
        val observable = callProcessor.observeProcessing()
            .prepareInBackground()
        
        callProcessor.startProcessing()
            .andThenAfterDelay(simulateCallProcessingWorkerFailure(),
                delayDuration = 1, delayUnit = SECONDS)
            .prepareInBackgroundAndWaitUpToTimeout()
        
        observable.waitUpToTimeout()

        println("RESULT: ${observable.values()}")
        observable.assertNotComplete()
        observable.assertValueCount(3)
        observable.assertValueAt(0, CallProcessingStopped)
        observable.assertValueAt(1, CallProcessingSuspended)
        observable.assertValueAt(2) { callProcessingState ->

            callProcessingState is CallProcessingFailed
        }
    }

    @Test
    fun processingStateTransitionsFromStartedToFailed() {
        val observable = callProcessor.observeProcessing()
            .prepareInBackground()

        callProcessor.startProcessing()
            .andThen(simulateConnectedToNetwork())
            .andThenAfterDelay(simulateCallProcessingWorkerFailure(),
                delayDuration = 1, delayUnit = SECONDS)
            .prepareInBackgroundAndWaitUpToTimeout()

        observable.waitUpToTimeout()

        println("RESULT: ${observable.values()}")
        observable.assertNotComplete()
        observable.assertValueCount(4)
        observable.assertValueAt(0, CallProcessingStopped)
        observable.assertValueAt(1, CallProcessingSuspended)
        observable.assertValueAt(2, CallProcessingStarted)
        observable.assertValueAt(3) { callProcessingState ->

            callProcessingState is CallProcessingFailed
        }
    }

    @Test
    fun processingIsEventuallyRestartedByCheckAfterFailure() {
        val observable = callProcessor.observeProcessing()
            .prepareInBackground()

        callProcessor.startProcessing()
            .andThenAfterDelay(simulateCallProcessingWorkerFailure(),
                delayDuration = 1, delayUnit = SECONDS)
            .andThenAfterDelay(simulateEnoughTimePassedForCheckToKickIn(),
                delayDuration = 1, delayUnit = SECONDS)
            .andThenAfterDelay(simulateConnectedToNetwork(),
                delayDuration = 250, delayUnit = MILLISECONDS)
            .prepareInBackgroundAndWaitUpToTimeout()

        observable.waitUpToTimeout(timeoutDuration = 16)

        println("RESULT: ${observable.values()}")
        observable.assertNotComplete()
        observable.assertValueCount(5)
        observable.assertValueAt(0, CallProcessingStopped)
        observable.assertValueAt(1, CallProcessingSuspended)
        observable.assertValueAt(2) { callProcessingState ->

            callProcessingState is CallProcessingFailed
        }
        observable.assertValueAt(3, CallProcessingSuspended)
        observable.assertValueAt(4, CallProcessingStarted)
    }

    @After
    fun tearDown() {
        callProcessor.stopProcessing()
            .andThen(simulateCallProcessingWorkerFailure())
            .prepareInBackgroundAndWaitUpToTimeout()

        callProcessor.clear()
    }

    private fun simulateConnectedToNetwork(): Completable {
        return Completable.fromCallable {
            WorkManagerTestInitHelper.getTestDriver(
                ApplicationProvider.getApplicationContext()
            )?.let {
                it.setAllConstraintsMet(startCallProcessingWorkRequest.id)
                it.setAllConstraintsMet(processingChecksWorkRequest.id)
            }
        }
    }

    private fun simulateCallProcessingWorkerFailure(): Completable {
        return Completable.fromCallable {
            (sipEngine as FakeSipEngine).simulateFailureWhileProcessingEngineSteps()

            WorkManager.getInstance(ApplicationProvider.getApplicationContext())
                .cancelWorkById(startCallProcessingWorkRequest.id)
        }
    }

    private fun simulateEnoughTimePassedForCheckToKickIn(): Completable {
        return Completable.fromCallable {
            WorkManagerTestInitHelper.getTestDriver(
                ApplicationProvider.getApplicationContext()
            )?.let {
                (sipEngine as FakeSipEngine).revertFailureSimulationWhileProcessingEngineSteps()

                it.setAllConstraintsMet(processingChecksWorkRequest.id)
                it.setPeriodDelayMet(processingChecksWorkRequest.id)
            }
        }
    }
}
