package com.xibasdev.sipcaller.app.model.common

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import com.xibasdev.sipcaller.app.model.worker.CallProcessingChecksWorkerFactory
import com.xibasdev.sipcaller.app.model.worker.CallProcessingWorkerFactory
import javax.inject.Inject

class DelegatingWorkerFactory @Inject constructor(
    private val callProcessingWorkerFactory: CallProcessingWorkerFactory,
    private val callProcessingChecksWorkerFactory: CallProcessingChecksWorkerFactory
) : WorkerFactory() {

    override fun createWorker(
        appContext: Context,
        workerClassName: String,
        workerParameters: WorkerParameters
    ): ListenableWorker? {

        return when (workerClassName) {
            callProcessingWorkerFactory.getWorkerClassName() -> {
                callProcessingWorkerFactory.createWorker(
                    appContext, workerClassName, workerParameters
                )
            }
            callProcessingChecksWorkerFactory.getWorkerClassName() -> {
                callProcessingChecksWorkerFactory.createWorker(
                    appContext, workerClassName, workerParameters
                )
            }
            else -> {
                null
            }
        }
    }
}
