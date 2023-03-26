package com.xibasdev.sipcaller.app.viewmodel.events.account.registering

import com.xibasdev.sipcaller.app.viewmodel.common.OperationFailed

data class RegistrationCreateFailed(
    override val error: Throwable
) : CreateRegistrationEvent, OperationFailed
