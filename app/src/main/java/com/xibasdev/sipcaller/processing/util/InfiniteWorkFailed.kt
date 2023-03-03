package com.xibasdev.sipcaller.processing.util

import androidx.work.WorkInfo.State

data class InfiniteWorkFailed(val workState: State) : InfiniteWorkProgress
