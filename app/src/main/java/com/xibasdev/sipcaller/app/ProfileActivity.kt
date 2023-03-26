package com.xibasdev.sipcaller.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.rxjava3.subscribeAsState
import com.xibasdev.sipcaller.app.viewmodel.profile.ProfileViewModel
import com.xibasdev.sipcaller.app.view.profile.ProfileScreen
import com.xibasdev.sipcaller.sip.calling.details.NoCallUpdateAvailable
import com.xibasdev.sipcaller.sip.identity.UnreachableIdentity
import dagger.hilt.android.AndroidEntryPoint
import java.time.OffsetDateTime

@AndroidEntryPoint
class ProfileActivity : ComponentActivity() {

    private val viewModel: ProfileViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val registrationUpdates = viewModel.observeRegistrations()
                .subscribeAsState(initial = listOf())

            val identityUpdates = viewModel.observeIdentity()
                .subscribeAsState(initial = UnreachableIdentity)

            val offset = OffsetDateTime.now()

            val historyUpdates = viewModel.observeCallHistory(offset)
                .subscribeAsState(initial = listOf())

            val detailsUpdates = viewModel.observeCallDetails(offset)
                .subscribeAsState(initial = NoCallUpdateAvailable)

            ProfileScreen(
                registrationUpdatesProvider = { registrationUpdates.value },
                identityUpdatesProvider = { identityUpdates.value },
                callHistoryUpdatesProvider = { historyUpdates.value },
                callDetailsUpdatesProvider = { detailsUpdates.value },
                onCreateRegistration = viewModel::createRegistration,
                onDestroyRegistration = viewModel::destroyRegistration,
                onSendCallInvitation = viewModel::sendCallInvitation,
                onCancelCallInvitation = viewModel::cancelCallInvitation,
                onAcceptCallInvitation = viewModel::acceptCallInvitation,
                onDeclineCallInvitation = viewModel::declineCallInvitation,
                onTerminateCallSession = viewModel::terminateCallSession
            )
        }
    }
}
