package com.xibasdev.sipcaller.sip.processing

sealed class ProcessingEngineFailure(
    message: String? = null,
    cause: Throwable? = null
) : Throwable(message, cause)
