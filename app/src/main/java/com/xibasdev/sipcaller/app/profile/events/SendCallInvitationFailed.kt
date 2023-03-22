package com.xibasdev.sipcaller.app.profile.events

data class SendCallInvitationFailed(
    val error: Throwable
) : ProfileScreenEvent
