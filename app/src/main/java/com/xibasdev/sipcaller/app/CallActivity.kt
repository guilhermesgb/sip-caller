package com.xibasdev.sipcaller.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.xibasdev.sipcaller.app.viewmodel.call.CallViewModel
import com.xibasdev.sipcaller.app.view.call.CallScreen
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class CallActivity : ComponentActivity() {

    private val viewModel: CallViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel.onUpdateLifecycle(lifecycle)

        setContent {
            CallScreen(
                onUpdateLocalSurfaceView = viewModel::onUpdateLocalSurfaceView,
                onUpdateRemoteSurfaceView = viewModel::onUpdateRemoteSurfaceView
            )
        }
    }
}
