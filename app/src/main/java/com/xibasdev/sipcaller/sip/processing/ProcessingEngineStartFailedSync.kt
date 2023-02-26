package com.xibasdev.sipcaller.sip.processing

data class ProcessingEngineStartFailedSync(
    override val message: String? = null,
    override val cause: Throwable? = null
) : ProcessingEngineFailure(message, cause)
