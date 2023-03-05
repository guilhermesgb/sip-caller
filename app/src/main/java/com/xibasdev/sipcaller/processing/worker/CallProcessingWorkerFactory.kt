package com.xibasdev.sipcaller.processing.worker

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import com.xibasdev.sipcaller.app.workers.CustomWorkerFactory
import com.xibasdev.sipcaller.processing.notifier.CallStateNotifierApi
import com.xibasdev.sipcaller.sip.SipEngineApi
import io.reactivex.rxjava3.core.Scheduler
import javax.inject.Inject
import javax.inject.Named

class CallProcessingWorkerFactory @Inject constructor(
    @Named("LinphoneRxScheduler") private val scheduler: Scheduler,
    private val sipEngine: SipEngineApi,
    private val callStateNotifier: CallStateNotifierApi
) : CustomWorkerFactory() {

    override fun getWorkerClassName(): String {
        return CallProcessingWorker::class.java.name
    }

    override fun createWorker(
        context: Context,
        workerClassName: String,
        workerParameters: WorkerParameters
    ): ListenableWorker {

        return CallProcessingWorker(
            context, workerParameters, scheduler, sipEngine, callStateNotifier
        )
    }
}
