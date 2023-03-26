package com.xibasdev.sipcaller.app.viewmodel.events.account.unregistering

import com.xibasdev.sipcaller.app.viewmodel.common.OperationFailed

data class RegistrationDestroyFailed(
    override val error: Throwable
) : DestroyRegistrationEvent, OperationFailed
