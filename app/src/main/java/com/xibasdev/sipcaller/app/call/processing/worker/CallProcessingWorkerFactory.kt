package com.xibasdev.sipcaller.app.call.processing.worker

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import com.xibasdev.sipcaller.app.LinphoneCore
import com.xibasdev.sipcaller.app.call.processing.notifier.CallStateNotifierApi
import io.reactivex.rxjava3.core.Scheduler
import javax.inject.Inject
import javax.inject.Named

class CallProcessingWorkerFactory @Inject constructor(
    @Named("LinphoneRxScheduler") private val scheduler: Scheduler,
    private val linphoneCore: LinphoneCore,
    private val callStateNotifier: CallStateNotifierApi
) : WorkerFactory() {

    override fun createWorker(
        context: Context,
        workerClassName: String,
        workerParameters: WorkerParameters
    ): ListenableWorker {

        return CallProcessingWorker(
            context, workerParameters, scheduler, linphoneCore, callStateNotifier
        )
    }
}
