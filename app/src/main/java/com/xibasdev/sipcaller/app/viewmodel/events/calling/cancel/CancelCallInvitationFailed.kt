package com.xibasdev.sipcaller.app.viewmodel.events.calling.cancel

import com.xibasdev.sipcaller.app.viewmodel.common.OperationFailed

data class CancelCallInvitationFailed(
    override val error: Throwable
) : CancelCallInvitationEvent, OperationFailed
