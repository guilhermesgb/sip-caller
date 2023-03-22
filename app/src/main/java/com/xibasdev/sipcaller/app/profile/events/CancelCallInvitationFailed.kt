package com.xibasdev.sipcaller.app.profile.events

data class CancelCallInvitationFailed(
    val error: Throwable
) : ProfileScreenEvent
