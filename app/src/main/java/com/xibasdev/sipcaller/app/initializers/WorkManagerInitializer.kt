package com.xibasdev.sipcaller.app.initializers

import android.content.Context
import androidx.work.Configuration
import androidx.work.WorkManager
import com.xibasdev.sipcaller.processing.worker.CallProcessingWorkerFactory
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class WorkManagerInitializer @Inject constructor(
    @ApplicationContext private val context: Context,
    private val callProcessingWorkerFactory: CallProcessingWorkerFactory
) : WorkManagerInitializerApi {

    override fun initializeWorkManager() {
        if (!WorkManager.isInitialized()) {
            WorkManager.initialize(
                context,
                Configuration.Builder()
                    .setWorkerFactory(callProcessingWorkerFactory)
                    .build()
            )
        }
    }
}
