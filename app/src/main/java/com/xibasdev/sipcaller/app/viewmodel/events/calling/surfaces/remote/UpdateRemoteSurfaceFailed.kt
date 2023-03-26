package com.xibasdev.sipcaller.app.viewmodel.events.calling.surfaces.remote

import com.xibasdev.sipcaller.app.viewmodel.common.OperationFailed

data class UpdateRemoteSurfaceFailed(
    override val error: Throwable
) : UpdateRemoteSurfaceEvent, OperationFailed
