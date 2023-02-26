package com.xibasdev.sipcaller.sip.processing

data class ProcessingEngineProcessingFailed(
    override val message: String? = null,
    override val cause: Throwable? = null
) : ProcessingEngineFailure(message, cause)
