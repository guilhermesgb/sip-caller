package com.xibasdev.sipcaller.app.viewmodel.profile.events

data class AcceptCallInvitationFailed(
    val error: Throwable
) : com.xibasdev.sipcaller.app.viewmodel.profile.events.ViewModelEvent
