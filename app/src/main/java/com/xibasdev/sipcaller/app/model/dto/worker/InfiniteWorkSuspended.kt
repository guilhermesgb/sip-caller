package com.xibasdev.sipcaller.app.model.dto.worker

import androidx.work.WorkInfo.State

data class InfiniteWorkSuspended(val workState: State) : InfiniteWorkProgress
