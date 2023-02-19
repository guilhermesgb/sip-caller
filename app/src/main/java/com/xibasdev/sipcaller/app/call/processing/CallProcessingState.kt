package com.xibasdev.sipcaller.app.call.processing

sealed interface CallProcessingState

object CallProcessingSuspended : CallProcessingState

object CallProcessingStarted : CallProcessingState

object CallProcessingStopped : CallProcessingState

data class CallProcessingFailed(val error: Throwable) : CallProcessingState
