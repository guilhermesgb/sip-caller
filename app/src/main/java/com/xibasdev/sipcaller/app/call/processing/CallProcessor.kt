package com.xibasdev.sipcaller.app.call.processing

import android.content.Context
import android.util.Log
import androidx.work.Configuration
import androidx.work.ExistingWorkPolicy.REPLACE
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkInfo.State.ENQUEUED
import androidx.work.WorkInfo.State.RUNNING
import androidx.work.WorkManager
import com.xibasdev.sipcaller.app.call.processing.notifier.CallStateNotifierApi
import com.xibasdev.sipcaller.app.call.processing.worker.CallProcessingWorkerFactory
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
private const val UNIQUE_WORK_NAME = "CallProcessing"

class CallProcessor @Inject constructor(
    @ApplicationContext private val context: Context,
    private val callStateNotifier: CallStateNotifierApi,
    @Named("CallProcessing") private val startCallProcessingWorkRequest: OneTimeWorkRequest,
    workerFactory: CallProcessingWorkerFactory
) : CallProcessorApi {

    init {
        WorkManager.initialize(context, Configuration.Builder()
            .setWorkerFactory(workerFactory).build())
    }

    private val disposables = CompositeDisposable()

    private val callProcessingStateUpdates = BehaviorSubject.create<CallProcessingState>().apply {
            onNext(CallProcessingStopped)
            monitorProcessingStateUpdates()
        }

    override fun startProcessing(): Completable {
        return Completable.create { emitter ->

            Log.d(TAG, "Starting calls processing...")

            val operation = WorkManager.getInstance(context)
                .enqueueUniqueWork(UNIQUE_WORK_NAME, REPLACE, startCallProcessingWorkRequest)

            try {
                operation.result.get()
                callProcessingStateUpdates.onNext(CallProcessingStarted)
                emitter.onComplete()

                Log.d(TAG, "Calls processing started.")

            } catch (error: Throwable) {

                callStateNotifier.notifyProcessingStartFailed(error)
                callProcessingStateUpdates.onNext(CallProcessingFailed(error))
                emitter.onError(error)

                Log.e(TAG, "Calls processing failed to start!", error)
            }
        }
    }

    override fun observeProcessingState(): Observable<CallProcessingState> {
        return callProcessingStateUpdates.distinctUntilChanged()
    }

    override fun stopProcessing(): Completable {
        return Completable.create { emitter ->

            Log.d(TAG, "Stopping calls processing...")

            val operation = WorkManager.getInstance(context)
                .cancelUniqueWork(UNIQUE_WORK_NAME)

            try {
                operation.result.get()
                callProcessingStateUpdates.onNext(CallProcessingStopped)
                emitter.onComplete()

                Log.d(TAG, "Calls processing stopped.")

            } catch (error: Throwable) {

                callStateNotifier.notifyProcessingStopFailed(error)
                callProcessingStateUpdates.onNext(CallProcessingFailed(error))
                emitter.onError(error)

                Log.e(TAG, "Calls processing failed to stop!", error)
            }
        }
    }

    private fun BehaviorSubject<CallProcessingState>.monitorProcessingStateUpdates() {
        switchMap { callProcessingState ->

            when (callProcessingState) {
                CallProcessingScheduled,
                CallProcessingStarted -> Observable
                    .interval(250, MILLISECONDS)
                    .doOnSubscribe {
                        Log.d(TAG, "Started monitoring " +
                                if (callProcessingState == CallProcessingScheduled) {
                                    "a pending"

                                } else {
                                    "an ongoing"
                                }
                                + " call processing..."
                        )
                    }
                    .map {
                        val workInfoList = WorkManager.getInstance(context)
                            .getWorkInfosForUniqueWork(UNIQUE_WORK_NAME)
                            .get()

                        if (workInfoList.isEmpty()) {
                            Log.e(TAG, "Processing fail - work not found!")

                            val error = IllegalStateException("Processing work not found.")
                            callStateNotifier.notifyProcessingFailed(error)
                            CallProcessingFailed(error)

                        } else when (val processingState = workInfoList.first().state) {
                            ENQUEUED -> {
                                Log.d(TAG, "Processing is still pending...")

                                CallProcessingScheduled
                            }
                            RUNNING -> {
                                Log.d(TAG, "Processing is still ongoing...")

                                CallProcessingStarted
                            }
                            else -> {
                                Log.e(TAG, "Processing fail - work state: $processingState!")

                                val error = Exception(
                                    "Processing not running; " +
                                            "system reported state: $processingState!"
                                )
                                callStateNotifier.notifyProcessingFailed(error)
                                CallProcessingFailed(error)
                            }
                        }
                    }
                CallProcessingStopped,
                is CallProcessingFailed -> Observable.just(callProcessingState)
            }
        }.subscribe().addTo(disposables)
    }
}
