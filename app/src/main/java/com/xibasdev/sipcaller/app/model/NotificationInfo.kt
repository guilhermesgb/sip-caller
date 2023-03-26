package com.xibasdev.sipcaller.processing.notifier

import android.app.Notification

/**
 * A pairing between a [Notification] and the notification identifier to be used for posting it.
 */
typealias NotificationInfo = Pair<Int, Notification>

fun NotificationInfo.getNotificationId(): Int = first

fun NotificationInfo.getNotification(): Notification = second
