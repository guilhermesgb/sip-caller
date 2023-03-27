package com.xibasdev.sipcaller.app.viewmodel.events.calling.surfaces.remote.update

import com.xibasdev.sipcaller.app.viewmodel.common.OperationFailed

data class UpdateRemoteSurfaceFailed(
    override val error: Throwable
) : UpdateRemoteSurfaceEvent, OperationFailed
