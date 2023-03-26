package com.xibasdev.sipcaller.app.model.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.ExistingWorkPolicy.REPLACE
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.rxjava3.RxWorker
import com.xibasdev.sipcaller.app.model.dto.processing.CallProcessing
import com.xibasdev.sipcaller.app.model.dto.processing.CallProcessingFailed
import com.xibasdev.sipcaller.app.model.dto.processing.CallProcessingStarted
import com.xibasdev.sipcaller.app.model.dto.processing.CallProcessingSuspended
import com.xibasdev.sipcaller.app.model.dto.worker.InfiniteWorkFailed
import com.xibasdev.sipcaller.app.model.dto.worker.InfiniteWorkMissing
import com.xibasdev.sipcaller.app.model.dto.worker.InfiniteWorkOngoing
import com.xibasdev.sipcaller.app.model.dto.worker.InfiniteWorkProgressUtils.getInfiniteWorkProgress
import com.xibasdev.sipcaller.app.model.dto.worker.InfiniteWorkSuspended
import dagger.assisted.AssistedInject
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.subjects.BehaviorSubject
import javax.inject.Named

private const val TAG = "CallProcessingChecksWorker"

@HiltWorker
class CallProcessingChecksWorker @AssistedInject constructor(
    private val context: Context,
    parameters: WorkerParameters,
    @Named("CallProcessingUpdates") private val processingUpdates: BehaviorSubject<CallProcessing>,
    @Named("CallProcessingUniqueWorkName") private val callProcessingWorkName: String,
    @Named("CallProcessing") private val startCallProcessingWorkRequest: OneTimeWorkRequest
) : RxWorker(context, parameters) {

    private val workManager by lazy {
        WorkManager.getInstance(context)
    }

    override fun createWork(): Single<Result> {
        return Single.fromCallable {
            val ongoingOrRestarted = with (workManager) {
                when (val progress = getInfiniteWorkProgress(callProcessingWorkName)) {
                    InfiniteWorkMissing -> {
                        Log.d(TAG, "Check detected work for call processing not found!")

                        restartCallProcessing()
                    }
                    is InfiniteWorkFailed -> {
                        Log.d(
                            TAG, "Check detected call processing state: " +
                                "${progress.workState}!")

                        restartCallProcessing()
                    }
                    else -> {
                        Log.d(TAG, "Check detected call processing is still ongoing.")
                        true
                    }
                }
            }

            if (ongoingOrRestarted) {
                Result.success()

            } else {
                Result.failure()
            }
        }
    }

    context (WorkManager)
    private fun restartCallProcessing(): Boolean {
        workManager.enqueueUniqueWork(
            callProcessingWorkName, REPLACE, startCallProcessingWorkRequest
        )

        return when (val progress = getInfiniteWorkProgress(callProcessingWorkName)) {
            is InfiniteWorkFailed -> {
                Log.e(
                    TAG, "Check failed to restart processing: " +
                        "in state ${progress.workState}!")

                val error = IllegalStateException("Work not restarted: " +
                        "in state ${progress.workState}.")
                processingUpdates.onNext(CallProcessingFailed(error))

                false
            }
            InfiniteWorkMissing -> {
                Log.e(TAG, "Check failed to restart call processing: not found!")

                val error = IllegalStateException("Work not restarted: not found.")
                processingUpdates.onNext(CallProcessingFailed(error))

                false
            }
            is InfiniteWorkSuspended -> {
                Log.d(TAG, "Check scheduled call processing restart successfully.")
                processingUpdates.onNext(CallProcessingSuspended)

                true
            }
            InfiniteWorkOngoing -> {
                Log.d(TAG, "Check restarted call processing successfully.")
                processingUpdates.onNext(CallProcessingStarted)

                true
            }
        }
    }
}
