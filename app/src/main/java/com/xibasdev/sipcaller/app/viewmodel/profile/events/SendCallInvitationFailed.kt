package com.xibasdev.sipcaller.app.viewmodel.profile.events

data class SendCallInvitationFailed(
    val error: Throwable
) : com.xibasdev.sipcaller.app.viewmodel.profile.events.ViewModelEvent
