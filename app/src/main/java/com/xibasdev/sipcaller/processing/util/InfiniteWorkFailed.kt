package com.xibasdev.sipcaller.processing.util

import androidx.work.WorkInfo.State
import androidx.work.WorkInfo.State.FAILED

data class InfiniteWorkFailed(
    val workState: State = FAILED,
    val cause: Throwable? = null
) : InfiniteWorkProgress
