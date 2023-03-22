package com.xibasdev.sipcaller.processing.worker

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkerParameters
import com.xibasdev.sipcaller.app.workers.CustomWorkerFactory
import com.xibasdev.sipcaller.processing.dto.CallProcessing
import io.reactivex.rxjava3.subjects.BehaviorSubject
import javax.inject.Inject
import javax.inject.Named

class CallProcessingChecksWorkerFactory @Inject constructor(
    @Named("CallProcessingUpdates") private val processingUpdates: BehaviorSubject<CallProcessing>,
    @Named("CallProcessingUniqueWorkName") private val callProcessingWorkName: String,
    @Named("CallProcessing") private val startCallProcessingWorkRequest: OneTimeWorkRequest
) : CustomWorkerFactory() {

    override fun getWorkerClassName(): String {
        return CallProcessingChecksWorker::class.java.name
    }

    override fun createWorker(
        context: Context,
        workerClassName: String,
        workerParameters: WorkerParameters
    ): ListenableWorker {

        return CallProcessingChecksWorker(
            context, workerParameters, processingUpdates,
            callProcessingWorkName, startCallProcessingWorkRequest
        )
    }
}
