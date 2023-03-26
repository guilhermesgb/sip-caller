package com.xibasdev.sipcaller.app.model.dto.worker

import androidx.work.WorkInfo.State
import androidx.work.WorkInfo.State.FAILED

data class InfiniteWorkFailed(
    val workState: State = FAILED,
    val cause: Throwable? = null
) : InfiniteWorkProgress
