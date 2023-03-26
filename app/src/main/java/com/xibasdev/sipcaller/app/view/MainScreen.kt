package com.xibasdev.sipcaller.app.view

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.AbsoluteRoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rxjava3.subscribeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.xibasdev.sipcaller.app.view.history.HistoryColumn
import com.xibasdev.sipcaller.app.view.identity.CurrentIdentityView
import com.xibasdev.sipcaller.app.view.registration.RegistrationsColumn
import com.xibasdev.sipcaller.app.viewmodel.MainViewModel
import com.xibasdev.sipcaller.app.viewmodel.common.Indexed
import com.xibasdev.sipcaller.app.viewmodel.common.ScreenRendered
import com.xibasdev.sipcaller.app.viewmodel.common.ViewModelEvent
import com.xibasdev.sipcaller.sip.calling.CallId
import com.xibasdev.sipcaller.sip.calling.details.CallInvitationUpdate
import com.xibasdev.sipcaller.sip.calling.details.CallSessionUpdate
import com.xibasdev.sipcaller.sip.calling.details.CallUpdate
import com.xibasdev.sipcaller.sip.calling.details.NoCallUpdateAvailable
import com.xibasdev.sipcaller.sip.history.CallHistoryUpdate
import com.xibasdev.sipcaller.sip.identity.IdentityUpdate
import com.xibasdev.sipcaller.sip.identity.UnreachableIdentity
import com.xibasdev.sipcaller.sip.registering.AccountRegistrationUpdate
import java.time.OffsetDateTime

@Composable
fun MainScreen(
    viewModel: MainViewModel = hiltViewModel(),
    onNavigateToCallScreen: (CallId) -> Unit
) {

    val eventUpdates = viewModel.observeEvents()
        .subscribeAsState(initial = ScreenRendered)

    val registrationUpdates = viewModel.observeRegistrations()
        .subscribeAsState(initial = listOf())

    val callHistoryUpdates = viewModel.observeCallHistory(offset = OffsetDateTime.now())
        .subscribeAsState(initial = listOf())

    val identityUpdates = viewModel.observeIdentity()
        .subscribeAsState(initial = UnreachableIdentity)

    val callDetailsUpdates = viewModel.observeCallDetails(offset = OffsetDateTime.now())
        .subscribeAsState(initial = NoCallUpdateAvailable)

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.fillMaxSize()
    ) {
        RegistrationsAndHistoryColumns(
            registrationUpdatesProvider = { registrationUpdates.value },
            callHistoryUpdatesProvider = { callHistoryUpdates.value },
            onCreateRegistration = viewModel::createRegistration,
            onDestroyRegistration = viewModel::destroyRegistration,
            onSendCallInvitation = viewModel::sendCallInvitation
        )

        CurrentIdentityAndOngoingCallRow(
            eventUpdatesProvider = { eventUpdates.value },
            identityUpdatesProvider = { identityUpdates.value },
            callDetailsUpdatesProvider = { callDetailsUpdates.value },
            onNavigateToCallScreen = onNavigateToCallScreen,
            modifier = Modifier
                .align(alignment = Alignment.BottomCenter)
                .padding(bottom = 20.dp)
        )
    }
}

@Composable
private fun RegistrationsAndHistoryColumns(
    registrationUpdatesProvider: () -> List<Indexed<AccountRegistrationUpdate>>,
    callHistoryUpdatesProvider: () -> List<Indexed<CallHistoryUpdate>>,
    onCreateRegistration: (rawAccountRegistration: String) -> Unit,
    onDestroyRegistration: () -> Unit,
    onSendCallInvitation: (rawDestinationAccount: String) -> Unit,
) {

    Row(
        modifier = Modifier
            .fillMaxWidth(fraction = 0.97f)
            .fillMaxHeight()

    ) {
        RegistrationsColumn(
            registrationUpdatesProvider = registrationUpdatesProvider,
            onCreateRegistration = onCreateRegistration,
            onDestroyRegistration = onDestroyRegistration,
            modifier = Modifier
                .weight(0.5f, fill = true)
                .fillMaxHeight()
        )

        Spacer(modifier = Modifier.padding(8.dp))
        
        HistoryColumn(
            callHistoryUpdatesProvider = callHistoryUpdatesProvider,
            onSendCallInvitation = onSendCallInvitation,
            modifier = Modifier
                .weight(0.5f, fill = true)
                .fillMaxHeight()
        )
    }
}

@Composable
private fun CurrentIdentityAndOngoingCallRow(
    eventUpdatesProvider: () -> ViewModelEvent,
    identityUpdatesProvider: () -> IdentityUpdate,
    callDetailsUpdatesProvider: () -> CallUpdate,
    onNavigateToCallScreen: (CallId) -> Unit,
    modifier: Modifier
) {

    Column(
        modifier = modifier
            .fillMaxWidth(fraction = 0.97f)
    ) {

        CurrentIdentityView(
            eventUpdatesProvider = eventUpdatesProvider,
            identityUpdatesProvider = identityUpdatesProvider
        )

        OngoingCallShortcut(
            callDetailsUpdatesProvider = callDetailsUpdatesProvider,
            onNavigateToCallScreen = onNavigateToCallScreen,
            modifier = Modifier.weight(1.0f, fill = true)
        )
    }
}

@Composable
private fun OngoingCallShortcut(
    callDetailsUpdatesProvider: () -> CallUpdate,
    onNavigateToCallScreen: (CallId) -> Unit,
    modifier: Modifier
) {

    val callDetailsUpdate = callDetailsUpdatesProvider()

    val (showNavigateToCallScreenButton, callId) = when (callDetailsUpdate) {
        is CallInvitationUpdate -> !callDetailsUpdate.call.status.isTerminal to
                callDetailsUpdate.call.callId
        is CallSessionUpdate -> !callDetailsUpdate.call.status.isTerminal to
                callDetailsUpdate.call.callId
        NoCallUpdateAvailable -> false to null
    }

    if (showNavigateToCallScreenButton) {
        val text = if (callDetailsUpdate is CallInvitationUpdate) {
            "Open call invitation screen"
        } else {
            "Open call session screen"
        }

        Button(
            onClick = {
                callId?.let { onNavigateToCallScreen(callId) }
            },
            enabled = true,
            shape = AbsoluteRoundedCornerShape(size = 7.dp),
            modifier = modifier
                .wrapContentWidth()
        ) {
            Text(
                text = text,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp
            )
        }
    }
}
