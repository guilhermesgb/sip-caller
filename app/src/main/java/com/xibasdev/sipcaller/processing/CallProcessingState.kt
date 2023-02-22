package com.xibasdev.sipcaller.processing

sealed interface CallProcessingState

object CallProcessingSuspended : CallProcessingState

object CallProcessingStarted : CallProcessingState

object CallProcessingStopped : CallProcessingState

data class CallProcessingFailed(val error: Throwable) : CallProcessingState
