package com.xibasdev.sipcaller.app.profile.events

data class AcceptCallInvitationFailed(
    val error: Throwable
) : ProfileScreenEvent
