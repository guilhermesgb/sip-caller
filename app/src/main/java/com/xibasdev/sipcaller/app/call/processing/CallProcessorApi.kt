package com.xibasdev.sipcaller.app.call.processing.api

import io.reactivex.rxjava3.core.Completable

interface CallProcessorApi {

    /**
     * Start processing calls in the background. Completes if processing started successfully,
     *  returning an error signal otherwise.
     */
    fun startProcessing(): Completable
}
