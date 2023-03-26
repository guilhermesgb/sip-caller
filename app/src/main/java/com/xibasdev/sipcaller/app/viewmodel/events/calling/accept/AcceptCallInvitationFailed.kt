package com.xibasdev.sipcaller.app.viewmodel.events.calling.accept

import com.xibasdev.sipcaller.app.viewmodel.common.OperationFailed

data class AcceptCallInvitationFailed(
    override val error: Throwable
) : AcceptCallInvitationEvent, OperationFailed
