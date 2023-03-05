package com.xibasdev.sipcaller.processing.util

import androidx.work.WorkInfo.State.BLOCKED
import androidx.work.WorkInfo.State.ENQUEUED
import androidx.work.WorkManager

object InfiniteWorkProgressUtils {

    context (WorkManager)
    fun getInfiniteWorkProgress(uniqueWorkName: String): InfiniteWorkProgress {
        return try {
            val workInfoList = getWorkInfosForUniqueWork(uniqueWorkName).get()

            if (workInfoList.isEmpty()) {
                InfiniteWorkMissing

            } else {
                val workInfoListEntry = workInfoList.first()
                val workState = workInfoListEntry.state

                if (workState.isFinished) {
                    InfiniteWorkFailed(workState = workState)

                } else if (workState == ENQUEUED || workState == BLOCKED) {
                    InfiniteWorkSuspended(workState = workState)

                } else {
                    InfiniteWorkOngoing
                }
            }
        } catch (error: Throwable) {
            InfiniteWorkFailed(cause = error)
        }
    }
}
