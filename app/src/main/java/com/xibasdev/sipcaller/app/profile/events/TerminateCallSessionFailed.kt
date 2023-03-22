package com.xibasdev.sipcaller.app.profile.events

data class TerminateCallSessionFailed(
    val error: Throwable
) : ProfileScreenEvent
