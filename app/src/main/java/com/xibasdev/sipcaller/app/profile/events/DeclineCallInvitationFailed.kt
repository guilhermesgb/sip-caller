package com.xibasdev.sipcaller.app.profile.events

data class DeclineCallInvitationFailed(
    val error: Throwable
) : ProfileScreenEvent
