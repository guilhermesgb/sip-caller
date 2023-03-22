package com.xibasdev.sipcaller.processing

import android.content.Context
import android.util.Log
import androidx.work.ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE
import androidx.work.ExistingWorkPolicy.REPLACE
import androidx.work.OneTimeWorkRequest
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import com.xibasdev.sipcaller.app.initializers.WorkManagerInitializerApi
import com.xibasdev.sipcaller.processing.dto.CallProcessing
import com.xibasdev.sipcaller.processing.dto.CallProcessingFailed
import com.xibasdev.sipcaller.processing.dto.CallProcessingStarted
import com.xibasdev.sipcaller.processing.dto.CallProcessingStopped
import com.xibasdev.sipcaller.processing.dto.CallProcessingSuspended
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

/**
 * Public API that serves as a contract for components interested in handling background call
 *   processing: starting or stopping it as well as reacting to processing state changes.
 *
 * To be used by app components scoped to some lifecycle context, such as the
 *   [com.xibasdev.sipcaller.app.SipCallerAppLifecycleObserver] that is scoped to the whole app process.
 */
class CallProcessor @Inject constructor(
    @ApplicationContext private val context: Context,
    private val processingStateNotifier: ProcessingStateNotifier,
    @Named("CallProcessingUpdates") private val processingUpdates: BehaviorSubject<CallProcessing>,
    @Named("CallProcessingUniqueWorkName") private val callProcessingWorkName: String,
    @Named("CallProcessing") private val startCallProcessingWorkRequest: OneTimeWorkRequest,
    @Named("CallProcessingChecksUniqueWorkName") private val processingChecksWorkName: String,
    @Named("CallProcessingChecks") private val processingChecksWorkRequest: PeriodicWorkRequest,
    workManagerInitializer: WorkManagerInitializerApi
) {

    private val workManager by lazy {
        WorkManager.getInstance(context)
    }

    private val disposables = CompositeDisposable()

    init {
        workManagerInitializer.initializeWorkManager()

        processingUpdates.monitorProcessingStateUpdates()
    }

    /**
     * Start processing calls in the background. Completes if processing started successfully, or if
     *   processing was scheduled by the system to be started sometime in the future, returning an
     *   error signal otherwise (e.g. if underlying worker is not allowed to start).
     *
     * When processing is started successfully, the app will be able to place outgoing calls to
     *   remote parties as well as receive incoming calls from remote parties.
     *
     * While processing is just scheduled, the app will not yet be able to place outgoing calls to
     *   remote parties as well as receive incoming calls from remote parties. Processing may start
     *   in the scheduled state instead of being actively running right away, under the system's own
     *   discretion. Processing will revert back to the scheduled state while the device has no
     *   active network connection available.
     *
     * Using the [observeProcessing] method you may observe a future transition from the
     *   [com.xibasdev.sipcaller.processing.dto.CallProcessingSuspended] state into the
     *   [com.xibasdev.sipcaller.processing.dto.CallProcessingStarted] state when processing does
     *   indeed start executing in the background, as well as a transition from
     *   [com.xibasdev.sipcaller.processing.dto.CallProcessingStarted] back to
     *   [com.xibasdev.sipcaller.processing.dto.CallProcessingSuspended] if the call processing is
     *   once again suspended by the system e.g. while there's no active network connection
     *   available.
     *
     * Processing may be stopped with [stopProcessing].
     */
    fun startProcessing(): Completable {
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
                            processingStateNotifier.notifyProcessingStartFailed(error)
                            processingUpdates.onNext(CallProcessingFailed(error))
                            emitter.onError(error)

                            Log.e(TAG, "Calls processing failed to start: " +
                                    "work not found!", error)
                        }
                        is InfiniteWorkFailed -> {
                            val error = IllegalStateException("Work not started: finished.")
                            processingStateNotifier.notifyProcessingStartFailed(error)
                            processingUpdates.onNext(CallProcessingFailed(error))
                            emitter.onError(error)

                            Log.e(TAG, "Calls processing failed: " +
                                    "in state ${progress.workState}!")
                        }
                        is InfiniteWorkSuspended -> {
                            processingUpdates.onNext(CallProcessingSuspended)
                            emitter.onComplete()

                            Log.d(TAG, "Calls processing start scheduled.")
                        }
                        InfiniteWorkOngoing -> {
                            processingUpdates.onNext(CallProcessingStarted)
                            emitter.onComplete()

                            Log.d(TAG, "Calls processing started.")
                        }
                    }
                } catch (error: Throwable) {
                    processingStateNotifier.notifyProcessingStartFailed(error)
                    processingUpdates.onNext(CallProcessingFailed(error))
                    emitter.onError(error)

                    Log.e(TAG, "Calls processing failed to start!", error)
                }
            }
        }
    }

    /**
     * Observe calls processing state over time.
     *
     * Emits [com.xibasdev.sipcaller.processing.dto.CallProcessingStarted] when processing is
     *   started successfully and [com.xibasdev.sipcaller.processing.dto.CallProcessingStopped] when
     *   processing is successfully stopped.
     *
     * It also emits [com.xibasdev.sipcaller.processing.dto.CallProcessingFailed] if a failure to
     *   start/stop processing occurs.
     *
     * While call processing is ongoing, it also emits
     *   [com.xibasdev.sipcaller.processing.dto.CallProcessingFailed] if a failure is detected that
     *   suddenly halts call processing.
     *
     * [com.xibasdev.sipcaller.processing.dto.CallProcessingSuspended] is emitted when the call
     *   processing is scheduled but not currently executing. Call background processing is paused
     *   and sent back to the scheduled state by the system while the device is currently with no
     *   active network connection enabled.
     *
     * It is possible for the system to not immediately start call processing from the
     *   [startProcessing] method e.g. if the system is under heavy load and/or the call processing
     *   work quota is depleted (in which case some time has to pass until it refreshes again) -
     *   under such conditions, [com.xibasdev.sipcaller.processing.dto.CallProcessingSuspended] is
     *   also emitted signaling this.
     */
    fun observeProcessing(): Observable<CallProcessing> {
        return processingUpdates.distinctUntilChanged()
    }

    /**
     * Stop processing calls in the background. Completes if processing stopped successfully,
     *   returning an error signal otherwise (e.g. there was no processing started to be stopped).
     *
     * After processing is stopped, the app will no longer be able to place outgoing calls to remote
     *   parties as well as receive incoming calls from remote parties.
     *
     * Processing may be started again with [startProcessing].
     */
    fun stopProcessing(): Completable {
        return Completable.create { emitter ->

            Log.d(TAG, "Stopping calls processing...")

            val stopCallProcessing = workManager
                .cancelUniqueWork(callProcessingWorkName)

            val stopCallProcessingChecks = workManager
                .cancelUniqueWork(processingChecksWorkName)

            try {
                stopCallProcessing.result.get()
                stopCallProcessingChecks.result.get()

                processingUpdates.onNext(CallProcessingStopped)
                emitter.onComplete()

                Log.d(TAG, "Calls processing stopped.")

            } catch (error: Throwable) {

                processingStateNotifier.notifyProcessingStopFailed(error)
                processingUpdates.onNext(CallProcessingFailed(error))
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

                                processingStateNotifier.notifyProcessingSuspended()
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
        return with (workManager) {
            when (val processingProgress = getInfiniteWorkProgress(callProcessingWorkName)) {
                InfiniteWorkMissing -> {
                    val currentProcessingStatus = value

                    if (currentProcessingStatus != CallProcessingSuspended
                        && currentProcessingStatus != CallProcessingStarted
                    ) {

                        return@with currentProcessingStatus ?: CallProcessingStopped
                    }

                    Log.e(TAG, "Processing fail - work for call processing not found!")

                    val error = IllegalStateException("Processing work not found.")
                    processingStateNotifier.notifyProcessingFailed(error)
                    CallProcessingFailed(error)
                }
                is InfiniteWorkFailed -> {
                    val currentProcessingStatus = value

                    if (currentProcessingStatus != CallProcessingSuspended
                            && currentProcessingStatus != CallProcessingStarted
                    ) {

                        return@with currentProcessingStatus ?: CallProcessingStopped
                    }

                    Log.e(TAG, "Processing fail - call processing state:" +
                            " ${processingProgress.workState}!")

                    val error = IllegalStateException(
                        "Processing not running; system reported state:" +
                                " ${processingProgress.workState}!"
                    )
                    processingStateNotifier.notifyProcessingFailed(error)
                    CallProcessingFailed(error).also {
                        onNext(it)
                    }
                }
                is InfiniteWorkSuspended -> {
                    val currentProcessingStatus = value

                    if (currentProcessingStatus == CallProcessingSuspended) {
                        Log.d(TAG, "Processing is still suspended...")
                        return@with currentProcessingStatus
                    }

                    Log.d(TAG, "Processing is now suspended...")

                    CallProcessingSuspended.also {
                        onNext(it)
                    }
                }
                InfiniteWorkOngoing -> {
                    val currentProcessingStatus = value

                    if (currentProcessingStatus == CallProcessingStarted) {
                        Log.d(TAG, "Processing is still ongoing...")

                        return@with checkProcessingChecksProgress(currentProcessingStatus)
                    }

                    Log.d(TAG, "Processing is now ongoing...")

                    CallProcessingStarted.also {
                        onNext(it)
                    }
                }
            }
        }
    }

    context (WorkManager)
    private fun BehaviorSubject<CallProcessing>.checkProcessingChecksProgress(
        currentProcessingStatus: CallProcessing
    ): CallProcessing {
        return when (val progress = getInfiniteWorkProgress(processingChecksWorkName)) {
            InfiniteWorkMissing -> {
                Log.e(TAG, "Processing fail - work for call processing checks not found!")

                val error = IllegalStateException("Processing checks work not found.")
                processingStateNotifier.notifyProcessingFailed(error)
                return CallProcessingFailed(error).also {
                    onNext(it)
                }
            }
            is InfiniteWorkFailed -> {
                Log.e(TAG, "Processing fail - process checks state: ${progress.workState}!")

                val error = IllegalStateException(
                    "Processing checks not running; system reported state: ${progress.workState}!"
                )
                processingStateNotifier.notifyProcessingFailed(error)
                return CallProcessingFailed(error).also {
                    onNext(it)
                }
            }
            else -> currentProcessingStatus
        }
    }

    /**
     * Invalidates this processor instance, cleaning up its underlying monitoring processing.
     */
    fun clear() {
        disposables.dispose()
    }
}
