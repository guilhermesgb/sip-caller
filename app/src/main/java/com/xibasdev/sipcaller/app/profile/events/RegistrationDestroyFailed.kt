package com.xibasdev.sipcaller.app.profile.events

data class RegistrationDestroyFailed(
    val error: Throwable
) : ProfileScreenEvent
