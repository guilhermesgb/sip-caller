package com.xibasdev.sipcaller.app.call.processing.worker

import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.ForegroundInfo
import androidx.work.ListenableWorker.Result.Failure
import androidx.work.WorkerParameters
import androidx.work.rxjava3.RxWorker
import com.xibasdev.sipcaller.app.call.processing.notifier.CallStateNotifierApi
import com.xibasdev.sipcaller.app.call.processing.notifier.getNotification
import com.xibasdev.sipcaller.app.call.processing.notifier.getNotificationId
import com.xibasdev.sipcaller.sip.SipEngineApi
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Scheduler
import io.reactivex.rxjava3.core.Single
import java.util.concurrent.TimeUnit.MILLISECONDS

const val CALL_PROCESSING_RATE_MS = 250L
private const val TAG = "CallProcessingWorker"

@HiltWorker
class CallProcessingWorker @AssistedInject constructor(
    context: Context,
    parameters: WorkerParameters,
    @Assisted private val scheduler: Scheduler,
    @Assisted private val sipEngine: SipEngineApi,
    @Assisted private val callStateNotifier: CallStateNotifierApi
) : RxWorker(context, parameters) {

    override fun createWork(): Single<Result> {
        return setForeground(createForegroundInfo())
            .doOnSubscribe {
                Log.d(TAG, "Starting call processing work in foreground...")
            }
            .andThen(sipEngine.startEngine())
            .mapToWorkResult()
            .flatMap { processingStartResult ->

                if (processingStartResult is Failure) {
                    Log.e(TAG, "Call processing failed to start!")

                    Single.just(processingStartResult)

                } else {
                    Log.d(TAG, "Call processing started successfully.")

                    Observable
                        .interval(CALL_PROCESSING_RATE_MS, MILLISECONDS, scheduler)
                        .flatMapCompletable {
                            Log.d(TAG, "Executing call processing steps.")

                            sipEngine.processEngineSteps()
                        }
                        .mapToWorkResult()
                        .doOnSuccess { processingStepsResult ->

                            if (processingStepsResult is Failure) {
                                Log.e(TAG, "Failed to execute call processing steps!")

                            } else {
                                Log.d(TAG, "Call processing steps executed successfully.")
                            }
                        }
                }
            }
    }

    private fun createForegroundInfo(): ForegroundInfo {
        val notificationInfo = callStateNotifier.getNotificationInfoForProcessingStarted()

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val foregroundServiceTypes = ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA or
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE

            ForegroundInfo(
                notificationInfo.getNotificationId(),
                notificationInfo.getNotification(),
                foregroundServiceTypes
            )

        } else {
            ForegroundInfo(
                notificationInfo.getNotificationId(),
                notificationInfo.getNotification()
            )
        }
    }

    private fun Completable.mapToWorkResult(): Single<Result> {
        return andThen(Single.just(Result.success()))
            .onErrorReturnItem(Result.failure())
    }
}
