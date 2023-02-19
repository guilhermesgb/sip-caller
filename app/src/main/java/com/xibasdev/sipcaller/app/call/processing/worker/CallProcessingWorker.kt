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
import com.xibasdev.sipcaller.app.LinphoneCore
import com.xibasdev.sipcaller.app.call.processing.notifier.CallStateNotifierApi
import com.xibasdev.sipcaller.app.call.processing.notifier.getNotification
import com.xibasdev.sipcaller.app.call.processing.notifier.getNotificationId
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Scheduler
import io.reactivex.rxjava3.core.Single
import java.util.concurrent.TimeUnit.MILLISECONDS
import org.linphone.core.Core
import org.linphone.core.CoreListener
import org.linphone.core.CoreListenerStub
import org.linphone.core.GlobalState
import org.linphone.core.GlobalState.Configuring
import org.linphone.core.GlobalState.Off
import org.linphone.core.GlobalState.On
import org.linphone.core.GlobalState.Ready
import org.linphone.core.GlobalState.Shutdown
import org.linphone.core.GlobalState.Startup

private const val TAG = "CallProcessingWorker"

@HiltWorker
class CallProcessingWorker @AssistedInject constructor(
    context: Context,
    parameters: WorkerParameters,
    @Assisted private val scheduler: Scheduler,
    @Assisted private val linphoneCore: LinphoneCore,
    @Assisted private val callStateNotifier: CallStateNotifierApi
) : RxWorker(context, parameters) {

    override fun createWork(): Single<Result> {
        return setForeground(createForegroundInfo())
            .andThen(
                Single.create { emitter ->

                    Log.d(TAG, "Starting Linphone...")

                    val globalStateChangeListener: CoreListener = object : CoreListenerStub() {
                        override fun onGlobalStateChanged(
                            core: Core,
                            state: GlobalState?,
                            message: String
                        ) {

                            when (state) {
                                Startup,
                                Configuring -> Log.d(TAG, "Linphone startup in progress...")
                                On -> {
                                    Log.d(TAG, "Linphone startup complete!")

                                    linphoneCore.removeListener(this)
                                    emitter.onSuccess(Result.success())
                                }
                                Ready,
                                Off,
                                Shutdown,
                                null -> {
                                    Log.e(TAG, "Failed to startup Linphone core;" +
                                            " transitioned to state: $state!")

                                    linphoneCore.removeListener(this)
                                    emitter.onSuccess(Result.failure())
                                }
                            }
                        }
                    }

                    linphoneCore.addListener(globalStateChangeListener)

                    val startupCode = linphoneCore.start()

                    if (startupCode != 0) {
                        Log.e(TAG, "Failed to startup Linphone core;" +
                                " startup code: $startupCode!")

                        linphoneCore.removeListener(globalStateChangeListener)
                        emitter.onSuccess(Result.failure())
                    }
                }
            )
            .flatMap { result ->

                if (result is Failure) {
                    Single.just(result)

                } else {
                    Observable.interval(250, MILLISECONDS, scheduler)
                        .map {

                            Log.d(TAG, "Iterating Linphone core...")
                            linphoneCore.iterate()
                        }
                        .ignoreElements()
                        .andThen(Single.just(result))
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
}
