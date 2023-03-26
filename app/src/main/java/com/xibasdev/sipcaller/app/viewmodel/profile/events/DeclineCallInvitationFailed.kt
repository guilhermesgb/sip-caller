package com.xibasdev.sipcaller.app.viewmodel.profile.events

data class DeclineCallInvitationFailed(
    val error: Throwable
) : com.xibasdev.sipcaller.app.viewmodel.profile.events.ViewModelEvent
