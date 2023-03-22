package com.xibasdev.sipcaller.processing

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.xibasdev.sipcaller.R
import com.xibasdev.sipcaller.processing.notifier.NotificationInfo
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/**
 * Public API that serves as a contract between internal call processing components that need to
 *   represent certain processing events as system notifications either due to system requirements
 *   or to better inform app users of certain important states such as when failures happen.
 */
class ProcessingStateNotifier @Inject constructor(@ApplicationContext private val context: Context) {

    private val notificationManager = context
        .getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    /**
     * To be used when call processing is started.
     *
     * Return a [NotificationInfo] to be associated with the foreground service encompassing
     *   the ongoing call processing that was just started.
     */
    fun getNotificationInfoForProcessingStarted(): NotificationInfo {
        createNotificationChannelIfApplicable()

        return Pair(
            NOTIFICATION_ID_CALL_PROCESSING,
            NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(androidx.core.R.drawable.notification_bg)
                .setContentText("SIP Caller is standby.")
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_MIN)
                .build()
        )
    }

    /**
     * To be used when call processing fails to start.
     *
     * Posts a [Notification] to the system informing users that such failure occurred.
     */
    fun notifyProcessingStartFailed(error: Throwable) {
        createNotificationChannelIfApplicable()

        notificationManager.notify(
            NOTIFICATION_ID_CALL_PROCESSING,
            NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(androidx.core.R.drawable.notification_bg)
                .setContentText("SIP Caller failed to start. Reason: ${error.message}")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .build()
        )
    }

    /**
     * To be used when some failure occurs that halts a previously-ongoing call processing.
     *
     * Posts a [Notification] to the system informing users that such failure occurred.
     */
    fun notifyProcessingFailed(error: Throwable) {
        createNotificationChannelIfApplicable()

        notificationManager.notify(
            NOTIFICATION_ID_CALL_PROCESSING,
            NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(androidx.core.R.drawable.notification_bg)
                .setContentText("SIP Caller processing failed. Reason: ${error.message}")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .build()
        )
    }


    /**
     * To be used when call processing fails to stop.
     *
     * Posts a [Notification] to the system informing users that such failure occurred.
     */
    fun notifyProcessingStopFailed(error: Throwable) {
        createNotificationChannelIfApplicable()

        notificationManager.notify(
            NOTIFICATION_ID_CALL_PROCESSING,
            NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(androidx.core.R.drawable.notification_bg)
                .setContentText("SIP Caller failed to stop. Reason: ${error.message}")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .build()
        )
    }

    /**
     * To be used when call processing is suspended.
     *
     * Posts a [Notification] to the system informing users of such condition.
     */
    fun notifyProcessingSuspended() {
        createNotificationChannelIfApplicable()

        notificationManager.notify(
            NOTIFICATION_ID_CALL_PROCESSING,
            NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(androidx.core.R.drawable.notification_bg)
                .setContentText("SIP Caller processing suspended.")
                .setPriority(NotificationCompat.PRIORITY_MIN)
                .build()
        )
    }

    private fun createNotificationChannelIfApplicable() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (isNotificationChannelNotCreatedYet()) {
                val channel = NotificationChannel(
                    NOTIFICATION_CHANNEL_ID,
                    context.getString(R.string.app_name),
                    NotificationManager.IMPORTANCE_MIN
                )

                notificationManager.createNotificationChannel(channel)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun isNotificationChannelNotCreatedYet() =
        notificationManager.notificationChannels.none { it.id == NOTIFICATION_CHANNEL_ID }

    companion object {
        private const val NOTIFICATION_CHANNEL_ID = "CallStateNotifier"
        private const val NOTIFICATION_ID_CALL_PROCESSING = 1
    }
}
