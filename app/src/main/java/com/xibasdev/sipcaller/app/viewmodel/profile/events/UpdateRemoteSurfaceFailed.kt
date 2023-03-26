package com.xibasdev.sipcaller.app.viewmodel.profile.events

data class UpdateRemoteSurfaceFailed(
    val error: Throwable
) : ViewModelEvent
