package com.xibasdev.sipcaller.app.viewmodel.events.calling.surfaces.remote.destroy

import com.xibasdev.sipcaller.app.viewmodel.common.OperationFailed

data class DestroyRemoteSurfaceFailed(
    override val error: Throwable
) : DestroyRemoteSurfaceEvent, OperationFailed
