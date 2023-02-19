package com.xibasdev.sipcaller.app.call.processing

import android.content.Context
import androidx.work.WorkManager
import androidx.work.WorkRequest
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Named

class CallProcessing @Inject constructor(
    @ApplicationContext context: Context, @Named("CallProcessing") workRequest: WorkRequest
) : CallProcessingApi {

    init {
        WorkManager.getInstance(context).enqueue(workRequest)
    }

    override fun processCalls() {
        TODO("Not yet implemented")
    }
}
