package com.xibasdev.sipcaller.processing

import android.content.Context
import android.util.Log
import androidx.work.ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE
import androidx.work.ExistingWorkPolicy.REPLACE
import androidx.work.OneTimeWorkRequest
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import com.xibasdev.sipcaller.app.initializers.WorkManagerInitializerApi
import com.xibasdev.sipcaller.dto.processing.CallProcessing
import com.xibasdev.sipcaller.dto.processing.CallProcessingFailed
import com.xibasdev.sipcaller.dto.processing.CallProcessingStarted
import com.xibasdev.sipcaller.dto.processing.CallProcessingStopped
import com.xibasdev.sipcaller.dto.processing.CallProcessingSuspended
import com.xibasdev.sipcaller.processing.notifier.CallStateNotifierApi
import com.xibasdev.sipcaller.processing.util.InfiniteWorkFailed
import com.xibasdev.sipcaller.processing.util.InfiniteWorkMissing
import com.xibasdev.sipcaller.processing.util.InfiniteWorkOngoing
import com.xibasdev.sipcaller.processing.util.InfiniteWorkProgressUtils.getInfiniteWorkProgress
import com.xibasdev.sipcaller.processing.util.InfiniteWorkSuspended
import dagger.hilt.android.qualifiers.ApplicationContext
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.addTo
import io.reactivex.rxjava3.subjects.BehaviorSubject
import java.util.concurrent.TimeUnit.MILLISECONDS
import javax.inject.Inject
import javax.inject.Named

private const val TAG = "CallProcessor"
private const val CALL_PROCESSING_MONITORING_RATE_MS = 250L

class CallProcessor @Inject constructor(
    @ApplicationContext private val context: Context,
    private val callStateNotifier: CallStateNotifierApi,
    @Named("CallProcessingUniqueWorkName") private val callProcessingWorkName: String,
    @Named("CallProcessing") private val startCallProcessingWorkRequest: OneTimeWorkRequest,
    @Named("CallProcessingChecksUniqueWorkName") private val processingChecksWorkName: String,
    @Named("CallProcessingChecks") private val processingChecksWorkRequest: PeriodicWorkRequest,
    workManagerInitializer: WorkManagerInitializerApi
) : CallProcessorApi {

    init {
        workManagerInitializer.initializeWorkManager()
    }

    private val workManager by lazy {
        WorkManager.getInstance(context)
    }

    private val disposables = CompositeDisposable()

    private val callProcessingUpdates = BehaviorSubject
        .createDefault<CallProcessing>(CallProcessingStopped).apply {
            monitorProcessingStateUpdates()
        }

    override fun startProcessing(): Completable {
        return Completable.create { emitter ->

            with (workManager) {
                Log.d(TAG, "Starting calls processing...")

                enqueueUniqueWork(callProcessingWorkName, REPLACE, startCallProcessingWorkRequest)

                enqueueUniquePeriodicWork(
                    processingChecksWorkName, CANCEL_AND_REENQUEUE, processingChecksWorkRequest
                )

                try {
                    when (val progress = getInfiniteWorkProgress(callProcessingWorkName)) {
                        InfiniteWorkMissing -> {
                            val error = IllegalStateException("Work not started: not found.")
                            callStateNotifier.notifyProcessingStartFailed(error)
                            callProcessingUpdates.onNext(CallProcessingFailed(error))
                            emitter.onError(error)

                            Log.e(TAG, "Calls processing failed to start: work not found!", error)
                        }
                        is InfiniteWorkFailed -> {
                            val error = IllegalStateException("Work not started: finished.")
                            callStateNotifier.notifyProcessingStartFailed(error)
                            callProcessingUpdates.onNext(CallProcessingFailed(error))
                            emitter.onError(error)

                            Log.e(TAG, "Calls processing failed: in state ${progress.workState}!")
                        }
                        is InfiniteWorkSuspended -> {
                            callProcessingUpdates.onNext(CallProcessingSuspended)
                            emitter.onComplete()

                            Log.d(TAG, "Calls processing start scheduled.")
                        }
                        InfiniteWorkOngoing -> {
                            callProcessingUpdates.onNext(CallProcessingStarted)
                            emitter.onComplete()

                            Log.d(TAG, "Calls processing started.")
                        }
                    }
                } catch (error: Throwable) {
                    callStateNotifier.notifyProcessingStartFailed(error)
                    callProcessingUpdates.onNext(CallProcessingFailed(error))
                    emitter.onError(error)

                    Log.e(TAG, "Calls processing failed to start!", error)
                }
            }
        }
    }

    override fun observeProcessing(): Observable<CallProcessing> {
        return callProcessingUpdates.distinctUntilChanged()
    }

    override fun stopProcessing(): Completable {
        return Completable.create { emitter ->

            Log.d(TAG, "Stopping calls processing...")

            val stopCallProcessing = workManager
                .cancelUniqueWork(callProcessingWorkName)

            val stopCallProcessingChecks = workManager
                .cancelUniqueWork(processingChecksWorkName)

            try {
                stopCallProcessing.result.get()
                stopCallProcessingChecks.result.get()

                callProcessingUpdates.onNext(CallProcessingStopped)
                emitter.onComplete()

                Log.d(TAG, "Calls processing stopped.")

            } catch (error: Throwable) {

                callStateNotifier.notifyProcessingStopFailed(error)
                callProcessingUpdates.onNext(CallProcessingFailed(error))
                emitter.onError(error)

                Log.e(TAG, "Calls processing failed to stop!", error)
            }
        }
    }

    private fun BehaviorSubject<CallProcessing>.monitorProcessingStateUpdates() {
        distinctUntilChanged()
            .switchMap { callProcessingState ->

                when (callProcessingState) {
                    CallProcessingSuspended,
                    CallProcessingStarted -> Observable
                        .interval(CALL_PROCESSING_MONITORING_RATE_MS, MILLISECONDS)
                        .doOnSubscribe {
                            if (callProcessingState == CallProcessingSuspended) {
                                Log.d(TAG, "Monitoring suspended call processing...")

                                callStateNotifier.notifyProcessingSuspended()
                            } else {
                                Log.d(TAG, "Monitoring ongoing call processing...")
                            }
                        }
                        .map { checkCurrentWorkProgress() }
                        .onErrorReturn { error ->

                            CallProcessingFailed(
                                IllegalStateException("Processing monitoring error!", error)
                            )
                        }
                    CallProcessingStopped,
                    is CallProcessingFailed -> Observable.just(callProcessingState)
                }
            }
            .subscribe()
            .addTo(disposables)
    }

    private fun BehaviorSubject<CallProcessing>.checkCurrentWorkProgress(): CallProcessing {
        with (workManager) {
            when (val progress = getInfiniteWorkProgress(processingChecksWorkName)) {
                InfiniteWorkMissing -> {
                    Log.e(TAG, "Processing fail - work for call processing checks not found!")

                    val error = IllegalStateException("Processing checks work not found.")
                    callStateNotifier.notifyProcessingFailed(error)
                    return CallProcessingFailed(error).also {
                        onNext(it)
                    }
                }
                is InfiniteWorkFailed -> {
                    Log.e(TAG, "Processing fail - process checks state: ${progress.workState}!")

                    val error = IllegalStateException(
                        "Processing checks not running; system reported state: ${progress.workState}!"
                    )
                    callStateNotifier.notifyProcessingFailed(error)
                    return CallProcessingFailed(error).also {
                        onNext(it)
                    }
                }
                else -> {}
            }

            return when (val progress = getInfiniteWorkProgress(callProcessingWorkName)) {
                InfiniteWorkMissing -> {
                    Log.e(TAG, "Processing fail - work for call processing not found!")

                    val error = IllegalStateException("Processing work not found.")
                    callStateNotifier.notifyProcessingFailed(error)
                    CallProcessingFailed(error)
                }
                is InfiniteWorkFailed -> {
                    Log.e(TAG, "Processing fail - call processing state: ${progress.workState}!")

                    val error = IllegalStateException(
                        "Processing not running; system reported state: ${progress.workState}!"
                    )
                    callStateNotifier.notifyProcessingFailed(error)
                    CallProcessingFailed(error).also {
                        onNext(it)
                    }
                }
                is InfiniteWorkSuspended -> {
                    Log.d(TAG, "Processing is still suspended...")

                    CallProcessingSuspended.also {
                        onNext(it)
                    }
                }
                InfiniteWorkOngoing -> {
                    Log.d(TAG, "Processing is still ongoing...")

                    CallProcessingStarted.also {
                        onNext(it)
                    }
                }
            }
        }
    }

    override fun clear() {
        disposables.dispose()
    }
}
