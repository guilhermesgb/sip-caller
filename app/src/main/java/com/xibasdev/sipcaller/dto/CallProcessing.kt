package com.xibasdev.sipcaller.dto

sealed interface CallProcessing

object CallProcessingSuspended : CallProcessing

object CallProcessingStarted : CallProcessing

object CallProcessingStopped : CallProcessing

data class CallProcessingFailed(val error: Throwable) : CallProcessing
