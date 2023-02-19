package com.xibasdev.sipcaller.app.call.processing.worker

import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
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
            .andThen(sipEngine.startEngine())
            .mapToWorkResult()
            .flatMap { result ->

                if (result is Failure) {
                    Single.just(result)

                } else {
                    Observable
                        .interval(250, MILLISECONDS, scheduler)
                        .flatMapCompletable {
                            sipEngine.processEngineSteps()
                        }
                        .mapToWorkResult()
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
