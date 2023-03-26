package com.xibasdev.sipcaller.app.view.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import com.xibasdev.sipcaller.app.viewmodel.common.Indexed
import com.xibasdev.sipcaller.sip.calling.CallDirection
import com.xibasdev.sipcaller.sip.calling.CallId
import com.xibasdev.sipcaller.sip.calling.CallStatus
import com.xibasdev.sipcaller.sip.calling.details.CallInvitationUpdate
import com.xibasdev.sipcaller.sip.calling.details.CallSessionUpdate
import com.xibasdev.sipcaller.sip.calling.details.CallUpdate
import com.xibasdev.sipcaller.sip.calling.details.NoCallUpdateAvailable
import com.xibasdev.sipcaller.sip.history.CallHistoryUpdate
import com.xibasdev.sipcaller.sip.identity.IdentityUpdate
import com.xibasdev.sipcaller.sip.registering.AccountRegistrationUpdate
import com.xibasdev.sipcaller.sip.registering.NoAccountRegistered
import com.xibasdev.sipcaller.sip.registering.RegisterAccountFailed
import com.xibasdev.sipcaller.sip.registering.RegisteredAccount
import com.xibasdev.sipcaller.sip.registering.RegisteringAccount
import com.xibasdev.sipcaller.sip.registering.RegistryOffline
import com.xibasdev.sipcaller.sip.registering.UnregisterAccountFailed
import com.xibasdev.sipcaller.sip.registering.UnregisteredAccount
import com.xibasdev.sipcaller.sip.registering.UnregisteringAccount

@Composable
fun ProfileScreen(
    registrationUpdatesProvider: () -> List<Indexed<AccountRegistrationUpdate>>,
    identityUpdatesProvider: () -> IdentityUpdate,
    callHistoryUpdatesProvider: () -> List<Indexed<CallHistoryUpdate>>,
    callDetailsUpdatesProvider: () -> CallUpdate,
    onCreateRegistration: (rawAccountRegistration: String) -> Unit,
    onDestroyRegistration: () -> Unit,
    onSendCallInvitation: (rawDestinationAccount: String) -> Unit,
    onCancelCallInvitation: (callId: CallId) -> Unit,
    onAcceptCallInvitation: (callId: CallId) -> Unit,
    onDeclineCallInvitation: (callId: CallId) -> Unit,
    onTerminateCallSession: (callId: CallId) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.SpaceEvenly,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxSize()
    ) {
        RawAccountRegistrationInputArea(
            registrationUpdatesProvider,
            onCreateRegistration,
            onDestroyRegistration,
            Modifier.align(Alignment.CenterHorizontally)
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly,
            modifier = Modifier
                .fillMaxWidth()
                .weight(weight = 0.3f, fill = true)
        ) {

            RegistrationView(
                registrationUpdatesProvider = registrationUpdatesProvider,
                modifier = Modifier.weight(weight = 0.5f, fill = true)
            )

            IdentityView(
                identityUpdatesProvider = identityUpdatesProvider,
                modifier = Modifier.weight(weight = 0.5f, fill = true)
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly,
            modifier = Modifier
                .fillMaxWidth()
                .weight(weight = 0.3f, fill = true)
        ) {

            HistoryView(
                callHistoryUpdatesProvider = callHistoryUpdatesProvider,
                modifier = Modifier.weight(weight = 0.5f, fill = true)
            )

            DetailsView(
                callDetailsUpdatesProvider = callDetailsUpdatesProvider,
                modifier = Modifier.weight(weight = 0.5f, fill = true)
            )
        }

        Column(
            verticalArrangement = Arrangement.SpaceEvenly,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .weight(weight = 0.4f, fill = true)
        ) {

            CallControlsView(
                callDetailsUpdatesProvider = callDetailsUpdatesProvider,
                onSendCallInvitation = onSendCallInvitation,
                onCancelCallInvitation = onCancelCallInvitation,
                onAcceptCallInvitation = onAcceptCallInvitation,
                onDeclineCallInvitation = onDeclineCallInvitation,
                onTerminateCallSession = onTerminateCallSession,
                Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun RawAccountRegistrationInputArea(
    registrationUpdatesProvider: () -> List<Indexed<AccountRegistrationUpdate>>,
    onCreateRegistration: (rawAccountRegistration: String) -> Unit,
    onDestroyRegistration: () -> Unit,
    modifier: Modifier
) {
    val textInput = remember { mutableStateOf("") }

    TextField(
        value = textInput.value,
        onValueChange = { updatedText ->

            textInput.value = updatedText
        },
        label = {
            Text(
                text = "<Display Name> username:password@domain:port",
                modifier = Modifier.wrapContentSize()
            )
        },
        singleLine = true,
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight()
    )

    val registrationUpdates = registrationUpdatesProvider()

    if (registrationUpdates.isNotEmpty()) {
        val (createBtnVisible, destroyBtnVisible) = when (registrationUpdates.first().value) {
            RegistryOffline,
            is RegisteringAccount,
            is UnregisteringAccount,
            is UnregisteredAccount -> false to false
            NoAccountRegistered,
            is RegisterAccountFailed -> textInput.value.isNotEmpty() to false
            is RegisteredAccount -> false to true
            is UnregisterAccountFailed -> false to true
        }

        Button(
            onClick = { onCreateRegistration(textInput.value) },
            enabled = createBtnVisible,
            modifier = modifier
        ) {

            Text(text = "Register account")
        }

        Button(
            onClick = { onDestroyRegistration() },
            enabled = destroyBtnVisible,
            modifier = modifier
        ) {

            Text(text = "Unregister account")
        }
    }
}

@Composable
private fun RegistrationView(
    registrationUpdatesProvider: () -> List<Indexed<AccountRegistrationUpdate>>,
    modifier: Modifier
) {
    val lazyColumnState = rememberLazyListState()

    LazyColumn(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        state = lazyColumnState,
        modifier = modifier.fillMaxHeight()
    ) {
        val registration = registrationUpdatesProvider()

        items(
            count = registration.size,
            key = { itemIndex ->

                registration[itemIndex].index
            }
        ) { itemIndex ->

            val rawRegistrationEntry = registration[itemIndex].value.toString()

            Text(
                text = rawRegistrationEntry.substring(
                    0 until 150.coerceAtMost(rawRegistrationEntry.length)
                ),
                fontSize = 8.sp,
                modifier = Modifier.wrapContentSize()
            )
        }
    }
}

@Composable
private fun IdentityView(
    identityUpdatesProvider: () -> IdentityUpdate,
    modifier: Modifier
) {
    val identity = identityUpdatesProvider()

    Text(
        text = identity.toString(),
        textAlign = TextAlign.Center,
        modifier = modifier.wrapContentSize()
    )
}

@Composable
private fun HistoryView(
    callHistoryUpdatesProvider: () -> List<Indexed<CallHistoryUpdate>>,
    modifier: Modifier
) {

    val lazyColumnState = rememberLazyListState()

    LazyColumn(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        state = lazyColumnState,
        modifier = modifier.fillMaxHeight()
    ) {
        val callHistory = callHistoryUpdatesProvider()

        items(
            count = callHistory.size,
            key = { itemIndex ->

                callHistory[itemIndex].index
            }
        ) { itemIndex ->

            val rawCallHistoryEntry = callHistory[itemIndex].value.toString()

            Text(
                text = rawCallHistoryEntry.substring(
                    0 until 150.coerceAtMost(rawCallHistoryEntry.length)
                ),
                fontSize = 8.sp,
                modifier = Modifier.wrapContentSize()
            )
        }
    }
}

@Composable
private fun DetailsView(
    callDetailsUpdatesProvider: () -> CallUpdate,
    modifier: Modifier
) {

    val callDetails = callDetailsUpdatesProvider()

    Text(
        text = callDetails.toString(),
        fontSize = 8.sp,
        textAlign = TextAlign.Center,
        modifier = modifier.wrapContentSize()
    )
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun CallControlsView(
    callDetailsUpdatesProvider: () -> CallUpdate,
    onSendCallInvitation: (rawDestinationAccount: String) -> Unit,
    onCancelCallInvitation: (callId: CallId) -> Unit,
    onAcceptCallInvitation: (callId: CallId) -> Unit,
    onDeclineCallInvitation: (callId: CallId) -> Unit,
    onTerminateCallSession: (callId: CallId) -> Unit,
    modifier: Modifier
) {

    val textInput = remember { mutableStateOf("") }

    TextField(
        value = textInput.value,
        onValueChange = { updatedText ->

            textInput.value = updatedText
        },
        label = {
            Text(
                text = "<Display Name> username@domain:port",
                modifier = Modifier.wrapContentSize()
            )
        },
        singleLine = true,
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
    )

    when (val callDetails = callDetailsUpdatesProvider()) {
        NoCallUpdateAvailable -> Button(
            onClick = { onSendCallInvitation(textInput.value) },
            modifier = modifier
        ) {

            Text(text = "Send call invitation")
        }
        is CallInvitationUpdate -> when (callDetails.call.status) {
            CallStatus.RINGING -> when (callDetails.call.direction) {
                CallDirection.OUTGOING -> Button(
                    onClick = { onCancelCallInvitation(callDetails.call.callId) },
                    modifier = modifier
                ) {

                    Text(text = "Cancel call invitation")
                }
                CallDirection.INCOMING -> {
                    Button(
                        onClick = { onAcceptCallInvitation(callDetails.call.callId) },
                        modifier = modifier
                    ) {

                        Text(text = "Accept call invitation")
                    }

                    Button(
                        onClick = { onDeclineCallInvitation(callDetails.call.callId) },
                        modifier = modifier
                    ) {

                        Text(text = "Decline call invitation")
                    }
                }
            }
            CallStatus.CANCELED -> {
                Button(
                    onClick = {},
                    enabled = false,
                    modifier = modifier
                ) {

                    Text(text = "Canceled call invitation")
                }

                Button(
                    onClick = { onSendCallInvitation(textInput.value) },
                    modifier = modifier
                ) {

                    Text(text = "Send call invitation")
                }
            }
            CallStatus.MISSED -> {
                Button(
                    onClick = {},
                    enabled = false,
                    modifier = modifier
                ) {

                    Text(text = "Missed call invitation")
                }

                Button(
                    onClick = { onSendCallInvitation(textInput.value) },
                    modifier = modifier
                ) {

                    Text(text = "Send call invitation")
                }
            }
            CallStatus.ACCEPTED_ELSEWHERE -> {
                Button(
                    onClick = {},
                    enabled = false,
                    modifier = modifier
                ) {

                    Text(text = "Call invitation accepted elsewhere")
                }

                Button(
                    onClick = { onSendCallInvitation(textInput.value) },
                    modifier = modifier
                ) {

                    Text(text = "Send call invitation")
                }
            }
            CallStatus.DECLINED -> {
                Button(
                    onClick = {},
                    enabled = false,
                    modifier = modifier
                ) {

                    Text(text = "Declined call invitation")
                }

                Button(
                    onClick = { onSendCallInvitation(textInput.value) },
                    modifier = modifier
                ) {

                    Text(text = "Send call invitation")
                }
            }
            CallStatus.ABORTED_DUE_TO_ERROR -> {
                Button(
                    onClick = {},
                    enabled = false,
                    modifier = modifier
                ) {

                    Text(text = "Call invitation aborted due to failure")
                }

                Button(
                    onClick = { onSendCallInvitation(textInput.value) },
                    modifier = modifier
                ) {

                    Text(text = "Send call invitation")
                }
            }
            else -> {}
        }
        is CallSessionUpdate -> when (callDetails.call.status) {
            CallStatus.ACCEPTED -> {
                Button(
                    onClick = {},
                    enabled = false,
                    modifier = modifier
                ) {

                    Text(text = "Call invitation accepted")
                }

                Button(
                    onClick = { onTerminateCallSession(callDetails.call.callId) },
                    modifier = modifier
                ) {

                    Text(text = "Terminate call session")
                }
            }
            CallStatus.FINISHED_BY_LOCAL_PARTY -> {
                Button(
                    onClick = {},
                    enabled = false,
                    modifier = modifier
                ) {

                    Text(text = "Call session terminated by you")
                }

                Button(
                    onClick = { onSendCallInvitation(textInput.value) },
                    modifier = modifier
                ) {

                    Text(text = "Send call invitation")
                }
            }
            CallStatus.FINISHED_BY_REMOTE_PARTY -> {
                Button(
                    onClick = {},
                    enabled = false,
                    modifier = modifier
                ) {

                    Text(text = "Call session terminated by the other side")
                }

                Button(
                    onClick = { onSendCallInvitation(textInput.value) },
                    modifier = modifier
                ) {

                    Text(text = "Send call invitation")
                }
            }
            CallStatus.FINISHED_DUE_TO_ERROR -> {
                Button(
                    onClick = {},
                    enabled = false,
                    modifier = modifier
                ) {

                    Text(text = "Call session terminated due to failure")
                }

                Button(
                    onClick = { onSendCallInvitation(textInput.value) },
                    modifier = modifier
                ) {

                    Text(text = "Send call invitation")
                }
            }
            else -> {}
        }
    }
}
