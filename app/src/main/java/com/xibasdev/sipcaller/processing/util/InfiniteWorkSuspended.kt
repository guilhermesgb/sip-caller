package com.xibasdev.sipcaller.processing.util

import androidx.work.WorkInfo.State

data class InfiniteWorkSuspended(val workState: State) : InfiniteWorkProgress
