package com.xibasdev.sipcaller.app.viewmodel.profile.events

data class RegistrationCreateFailed(
    val error: Throwable
) : com.xibasdev.sipcaller.app.viewmodel.profile.events.ViewModelEvent
