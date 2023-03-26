package com.xibasdev.sipcaller.app.viewmodel.profile.events

data class UpdateLocalSurfaceFailed(
    val error: Throwable
) : com.xibasdev.sipcaller.app.viewmodel.profile.events.ViewModelEvent
