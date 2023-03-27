package com.xibasdev.sipcaller.app.viewmodel.events.calling.surfaces.local.destroy

import com.xibasdev.sipcaller.app.viewmodel.common.OperationFailed

data class DestroyLocalSurfaceFailed(
    override val error: Throwable
) : DestroyLocalSurfaceEvent, OperationFailed
