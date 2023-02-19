package com.xibasdev.sipcaller.app.call.processing

import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Observable

/**
 * Public API that serves as a contract for components interested in handling background call
 *   processing: starting or stopping it as well as reacting to processing state changes.
 *
 * To be used by app components scoped to some lifecycle context, such as the
 *   [com.xibasdev.sipcaller.app.AppLifecycleObserver] that is scoped to the whole app process.
 */
interface CallProcessorApi {

    /**
     * Start processing calls in the background. Completes if processing started successfully, or if
     *   processing was scheduled by the system to be started sometime in the future, returning an
     *   error signal otherwise (e.g. if underlying worker is not allowed to start).
     *
     * When processing is started successfully, the app will be able to place outgoing calls to
     *   remote parties as well as receive incoming calls from remote parties.
     *
     * While processing is just scheduled, the app will not yet be able to place outgoing calls to
     *   remote parties as well as receive incoming calls from remote parties.
     *
     * Using the [observeProcessingState] method you may observe a future transition from the
     *   [CallProcessingScheduled] state into the [CallProcessingStarted] state when processing does
     *   indeed start executing in the background.
     *
     * Processing may be stopped with [stopProcessing].
     */
    fun startProcessing(): Completable

    /**
     * Observe calls processing state over time. Emits [CallProcessingStarted] when processing is
     *   started successfully and [CallProcessingStopped] when processing is successfully stopped.
     *
     * It also emits [CallProcessingFailed] if a failure to start/stop processing occurs.
     *
     * While call processing is ongoing, it also emits [CallProcessingFailed] if a failure is
     *   detected that suddenly halts call processing.
     *
     * It also emits [CallProcessingScheduled] if the system does not honor the call processing
     *   startup request immediately, meaning it may start it only sometime in the future.
     */
    fun observeProcessingState(): Observable<CallProcessingState>

    /**
     * Stop processing calls in the background. Completes if processing stopped successfully,
     *   returning an error signal otherwise (e.g. there was no processing started to be stopped).
     *
     * After processing is stopped, the app will no longer be able to place outgoing calls to remote
     *   parties as well as receive incoming calls from remote parties.
     *
     * Processing may be started again with [startProcessing].
     */
    fun stopProcessing(): Completable
}
