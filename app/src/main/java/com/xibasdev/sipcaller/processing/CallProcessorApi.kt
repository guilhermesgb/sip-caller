package com.xibasdev.sipcaller.processing

import com.xibasdev.sipcaller.dto.CallProcessing
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
     *   remote parties as well as receive incoming calls from remote parties. Processing may start
     *   in the scheduled state instead of being actively running right away, under the system's own
     *   discretion. Processing will revert back to the scheduled state while the device has no
     *   active network connection available.
     *
     * Using the [observeProcessing] method you may observe a future transition from the
     *   [com.xibasdev.sipcaller.dto.CallProcessingSuspended] state into the
     *   [com.xibasdev.sipcaller.dto.CallProcessingStarted] state when processing does indeed start
     *   executing in the background, as well as a transition from
     *   [com.xibasdev.sipcaller.dto.CallProcessingStarted] back to
     *   [com.xibasdev.sipcaller.dto.CallProcessingSuspended] if the call processing is once again
     *   suspended by the system e.g. while there's no active network connection available.
     *
     * Processing may be stopped with [stopProcessing].
     */
    fun startProcessing(): Completable

    /**
     * Observe calls processing state over time.
     *
     * Emits [com.xibasdev.sipcaller.dto.CallProcessingStarted] when processing is started
     *   successfully and [com.xibasdev.sipcaller.dto.CallProcessingStopped] when processing is
     *   successfully stopped.
     *
     * It also emits [com.xibasdev.sipcaller.dto.CallProcessingFailed] if a failure to start/stop
     *   processing occurs.
     *
     * While call processing is ongoing, it also emits
     *   [com.xibasdev.sipcaller.dto.CallProcessingFailed] if a failure is detected that suddenly
     *   halts call processing.
     *
     * [com.xibasdev.sipcaller.dto.CallProcessingSuspended] is emitted when the call processing is
     *   scheduled but not currently executing. Call background processing is paused and sent back
     *   to the scheduled state by the system while the device is currently with no active network
     *   connection enabled.
     *
     * It is possible for the system to not immediately start call processing from the
     *   [startProcessing] method e.g. if the system is under heavy load and/or the call processing
     *   work quota is depleted (in which case some time has to pass until it refreshes again) -
     *   under such conditions, [com.xibasdev.sipcaller.dto.CallProcessingSuspended] is also emitted
     *   signaling this.
     */
    fun observeProcessing(): Observable<CallProcessing>

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

    /**
     * Invalidates this processor instance, cleaning up its underlying monitoring processing.
     */
    fun clear()
}
