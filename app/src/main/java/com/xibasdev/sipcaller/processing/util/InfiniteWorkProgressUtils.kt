package com.xibasdev.sipcaller.processing.util

import androidx.work.WorkInfo.State.BLOCKED
import androidx.work.WorkInfo.State.ENQUEUED
import androidx.work.WorkManager

object InfiniteWorkProgressUtils {

    context (WorkManager)
    fun getInfiniteWorkProgress(uniqueWorkName: String): InfiniteWorkProgress {
        val workInfoList = getWorkInfosForUniqueWork(uniqueWorkName).get()

        return if (workInfoList.isEmpty()) {
            InfiniteWorkMissing

        } else {
            val workState = workInfoList.first().state

            if (workState.isFinished) {
                InfiniteWorkFailed(workState)

            } else if (workState == ENQUEUED || workState == BLOCKED) {
                InfiniteWorkSuspended(workState)

            } else {
                InfiniteWorkOngoing
            }
        }
    }
}
