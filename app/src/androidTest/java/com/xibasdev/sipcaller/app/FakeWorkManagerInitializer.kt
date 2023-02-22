package com.xibasdev.sipcaller.app

import android.content.Context
import androidx.work.Configuration
import androidx.work.testing.WorkManagerTestInitHelper
import com.xibasdev.sipcaller.processing.worker.CallProcessingWorkerFactory
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class FakeWorkManagerInitializer @Inject constructor(
    @ApplicationContext private val context: Context,
    private val callProcessingWorkerFactory: CallProcessingWorkerFactory
) : WorkManagerInitializerApi {

    override fun initializeWorkManager() {
        val workManagerConfiguration = Configuration.Builder()
            .setWorkerFactory(callProcessingWorkerFactory)
            .build()

        WorkManagerTestInitHelper.initializeTestWorkManager(context, workManagerConfiguration)
    }
}
