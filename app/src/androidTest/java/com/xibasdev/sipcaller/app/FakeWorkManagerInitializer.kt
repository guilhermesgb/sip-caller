package com.xibasdev.sipcaller.app

import android.content.Context
import androidx.work.Configuration
import androidx.work.testing.WorkManagerTestInitHelper
import com.xibasdev.sipcaller.app.initializers.WorkManagerInitializerApi
import com.xibasdev.sipcaller.app.workers.DelegatingWorkerFactory
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class FakeWorkManagerInitializer @Inject constructor(
    @ApplicationContext private val context: Context,
    private val delegatingWorkerFactory: DelegatingWorkerFactory
) : WorkManagerInitializerApi {

    override fun initializeWorkManager() {
        val workManagerConfiguration = Configuration.Builder()
            .setWorkerFactory(delegatingWorkerFactory)
            .build()

        WorkManagerTestInitHelper.initializeTestWorkManager(context, workManagerConfiguration)
    }
}
