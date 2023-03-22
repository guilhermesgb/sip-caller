package com.xibasdev.sipcaller.sip.calling.features

/**
 * A snapshot of the current state of an arbitrary call feature. It encodes information on whether
 * said feature is currently enabled or not, and whether the feature is currently in use or not.
 */
data class CallFeature(
    val enabled: Boolean = true,
    val active: Boolean = false
)
