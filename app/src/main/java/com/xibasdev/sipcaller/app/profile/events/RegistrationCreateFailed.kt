package com.xibasdev.sipcaller.app.profile.events

data class RegistrationCreateFailed(
    val error: Throwable
) : ProfileScreenEvent
