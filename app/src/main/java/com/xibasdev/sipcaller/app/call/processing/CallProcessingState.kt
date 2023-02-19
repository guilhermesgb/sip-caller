package com.xibasdev.sipcaller.app.call.processing

sealed interface CallProcessingState

object CallProcessingScheduled : CallProcessingState

object CallProcessingStarted : CallProcessingState

object CallProcessingStopped : CallProcessingState

data class CallProcessingFailed(val error: Throwable) : CallProcessingState
