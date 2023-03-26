package com.xibasdev.sipcaller.app.viewmodel.events.calling.terminate

import com.xibasdev.sipcaller.app.viewmodel.common.OperationFailed

data class TerminateCallSessionFailed(
    override val error: Throwable
) : TerminateCallSessionEvent, OperationFailed
