package com.xibasdev.sipcaller.app.call.processing.notifier

import android.app.Notification

interface CallStateNotifierApi {

    fun createNotificationChannelIfApplicable()

    fun getNotificationInfoForProcessingStarted(): NotificationInfo

    fun notifyProcessingStartFailed(error: Throwable)

    fun notifyProcessingFailed(error: Throwable)

    fun notifyProcessingStopFailed(error: Throwable)
}

typealias NotificationInfo = Pair<Int, Notification>

fun NotificationInfo.getNotificationId(): Int = first

fun NotificationInfo.getNotification(): Notification = second
