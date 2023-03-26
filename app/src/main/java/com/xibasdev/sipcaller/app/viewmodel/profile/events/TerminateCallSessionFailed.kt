package com.xibasdev.sipcaller.app.viewmodel.profile.events

data class TerminateCallSessionFailed(
    val error: Throwable
) : com.xibasdev.sipcaller.app.viewmodel.profile.events.ViewModelEvent
