package com.xibasdev.sipcaller.processing.notifier

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.xibasdev.sipcaller.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

private const val NOTIFICATION_CHANNEL_ID = "CallStateNotifier"
private const val NOTIFICATION_ID_CALL_PROCESSING = 1

class CallStateNotifier @Inject constructor(
    @ApplicationContext private val context: Context
) : CallStateNotifierApi {

    private val notificationManager = context
        .getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    override fun getNotificationInfoForProcessingStarted(): NotificationInfo {
        createNotificationChannelIfApplicable()

        return Pair(
            NOTIFICATION_ID_CALL_PROCESSING,
            NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(androidx.core.R.drawable.notification_bg)
                .setContentText("SIP Caller is standby.")
                .setOngoing(true)
                .build()
        )
    }

    override fun notifyProcessingStartFailed(error: Throwable) {
        createNotificationChannelIfApplicable()

        notificationManager.notify(
            NOTIFICATION_ID_CALL_PROCESSING,
            NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(androidx.core.R.drawable.notification_bg)
                .setContentText("SIP Caller failed to start. Reason: ${error.message}")
                .build()
        )
    }

    override fun notifyProcessingFailed(error: Throwable) {
        createNotificationChannelIfApplicable()

        notificationManager.notify(
            NOTIFICATION_ID_CALL_PROCESSING,
            NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(androidx.core.R.drawable.notification_bg)
                .setContentText("SIP Caller processing failed. Reason: ${error.message}")
                .build()
        )
    }

    override fun notifyProcessingStopFailed(error: Throwable) {
        createNotificationChannelIfApplicable()

        notificationManager.notify(
            NOTIFICATION_ID_CALL_PROCESSING,
            NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(androidx.core.R.drawable.notification_bg)
                .setContentText("SIP Caller failed to stop. Reason: ${error.message}")
                .build()
        )
    }

    override fun notifyProcessingSuspended() {
        createNotificationChannelIfApplicable()

        notificationManager.notify(
            NOTIFICATION_ID_CALL_PROCESSING,
            NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(androidx.core.R.drawable.notification_bg)
                .setContentText("SIP Caller processing suspended.")
                .build()
        )
    }

    private fun createNotificationChannelIfApplicable() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (isNotificationChannelNotCreatedYet()) {
                val channel = NotificationChannel(
                    NOTIFICATION_CHANNEL_ID,
                    context.getString(R.string.app_name),
                    NotificationManager.IMPORTANCE_HIGH
                )

                notificationManager.createNotificationChannel(channel)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun isNotificationChannelNotCreatedYet() =
        notificationManager.notificationChannels.none { it.id == NOTIFICATION_CHANNEL_ID }
}
