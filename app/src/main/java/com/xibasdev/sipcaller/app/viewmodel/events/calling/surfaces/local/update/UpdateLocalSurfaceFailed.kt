package com.xibasdev.sipcaller.app.viewmodel.events.calling.surfaces.local.update

import com.xibasdev.sipcaller.app.viewmodel.common.OperationFailed

data class UpdateLocalSurfaceFailed(
    override val error: Throwable
) : UpdateLocalSurfaceEvent, OperationFailed
