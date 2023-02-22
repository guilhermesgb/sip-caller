package com.xibasdev.sipcaller.processing.notifier

import android.app.Notification

/**
 * Public API that serves as a contract between internal call processing components that need to
 *   represent certain processing events as system notifications either due to system requirements
 *   or to better inform app users of certain important states such as when failures happen.
 */
interface CallStateNotifierApi {

    /**
     * To be used when call processing is started.
     *
     * Return a [NotificationInfo] to be associated with the foreground service encompassing
     *   the ongoing call processing that was just started.
     */
    fun getNotificationInfoForProcessingStarted(): NotificationInfo

    /**
     * To be used when call processing fails to start.
     *
     * Posts a [Notification] to the system informing users that such failure occurred.
     */
    fun notifyProcessingStartFailed(error: Throwable)

    /**
     * To be used when some failure occurs that halts a previously-ongoing call processing.
     *
     * Posts a [Notification] to the system informing users that such failure occurred.
     */
    fun notifyProcessingFailed(error: Throwable)

    /**
     * To be used when call processing fails to stop.
     *
     * Posts a [Notification] to the system informing users that such failure occurred.
     */
    fun notifyProcessingStopFailed(error: Throwable)

    /**
     * To be used when call processing is suspended.
     *
     * Posts a [Notification] to the system informing users of such condition.
     */
    fun notifyProcessingSuspended()
}

/**
 * A pairing between a [Notification] and the notification identifier to be used for posting it.
 */
typealias NotificationInfo = Pair<Int, Notification>

fun NotificationInfo.getNotificationId(): Int = first

fun NotificationInfo.getNotification(): Notification = second
