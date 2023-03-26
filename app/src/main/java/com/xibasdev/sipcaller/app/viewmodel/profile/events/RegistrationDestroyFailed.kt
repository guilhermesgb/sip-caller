package com.xibasdev.sipcaller.app.viewmodel.profile.events

data class RegistrationDestroyFailed(
    val error: Throwable
) : com.xibasdev.sipcaller.app.viewmodel.profile.events.ViewModelEvent
