package com.xibasdev.sipcaller.app.viewmodel.profile.events

data class CancelCallInvitationFailed(
    val error: Throwable
) : com.xibasdev.sipcaller.app.viewmodel.profile.events.ViewModelEvent
