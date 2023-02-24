package com.xibasdev.sipcaller.sip.processing

import io.reactivex.rxjava3.core.Completable

interface ProcessingEngineApi {

    /**
     * Starts the SIP Engine's underlying background processing.
     */
    fun startEngine(): Completable

    /**
     * Controls the SIP Engine's underlying background processing, instructing it to continue with
     *   its underlying processing work in the background.
     *
     * This has to be called periodically for the background processing to produce results, but can
     *   be not called for periods of time for as long as it is desired that the SIP engine remains
     *   with its background processing "paused". When this method starts being called once again,
     *   the SIP Engine's background processing is seamlessly "resumed".
     */
    fun processEngineSteps(): Completable
}
