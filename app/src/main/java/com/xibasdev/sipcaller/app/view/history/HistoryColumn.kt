package com.xibasdev.sipcaller.app.view.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.AbsoluteRoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xibasdev.sipcaller.app.view.common.ItemsColumn
import com.xibasdev.sipcaller.app.view.common.toRawString
import com.xibasdev.sipcaller.app.viewmodel.common.Indexed
import com.xibasdev.sipcaller.sip.calling.details.CallInvitationUpdate
import com.xibasdev.sipcaller.sip.calling.details.CallSessionUpdate
import com.xibasdev.sipcaller.sip.calling.details.CallUpdate
import com.xibasdev.sipcaller.sip.calling.details.NoCallUpdateAvailable
import com.xibasdev.sipcaller.sip.history.CallHistoryUpdate

@Composable
fun HistoryColumn(
    callHistoryUpdatesProvider: () -> List<Indexed<CallHistoryUpdate>>,
    callDetailsUpdatesProvider: () -> CallUpdate,
    onSendCallInvitation: (rawDestinationAccount: String) -> Unit,
    modifier: Modifier
) {

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top,
        modifier = modifier
    ) {

        SendCallInvitationView(
            callHistoryUpdatesProvider = callHistoryUpdatesProvider,
            callDetailsUpdatesProvider = callDetailsUpdatesProvider,
            onSendCallInvitation = onSendCallInvitation
        )

        HistoryLog(
            callHistoryUpdatesProvider = callHistoryUpdatesProvider,
            modifier = Modifier.fillMaxWidth()
                .weight(1.0f, fill = true)
        )
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun SendCallInvitationView(
    callHistoryUpdatesProvider: () -> List<Indexed<CallHistoryUpdate>>,
    callDetailsUpdatesProvider: () -> CallUpdate,
    onSendCallInvitation: (rawDestinationAccount: String) -> Unit
) {

    val textInput = remember { mutableStateOf("") }

    val callHistoryUpdates = callHistoryUpdatesProvider()

    if (callHistoryUpdates.isNotEmpty()) {
        textInput.value = callHistoryUpdates.first().value.remoteAccount
            .toRawString(includeDisplayName = false)
    }

    val inputFormEnabled = when (val callDetailsUpdate = callDetailsUpdatesProvider()) {
        is CallInvitationUpdate -> callDetailsUpdate.call.status.isTerminal
        is CallSessionUpdate -> callDetailsUpdate.call.status.isTerminal
        NoCallUpdateAvailable -> true
    }

    TextField(
        value = textInput.value,
        enabled = inputFormEnabled,
        onValueChange = { updatedText ->

            textInput.value = updatedText
        },
        label = {
            Text(
                text = "Destination account: <Display Name> username@domain:port",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp,
                modifier = Modifier.wrapContentSize()
            )
        },
        singleLine = true,
        modifier = Modifier
            .fillMaxWidth()
    )

    Button(
        onClick = { onSendCallInvitation(textInput.value) },
        enabled = inputFormEnabled,
        shape = AbsoluteRoundedCornerShape(size = 7.dp),
        modifier = Modifier
            .fillMaxWidth(fraction = 0.8f)
            .wrapContentHeight()
    ) {

        Text(
            text = "Send call invitation",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.8.sp
        )
    }
}

@Composable
private fun HistoryLog(
    callHistoryUpdatesProvider: () -> List<Indexed<CallHistoryUpdate>>,
    modifier: Modifier
) {

    ItemsColumn(
        columnName = "Call history log (recent)",
        sizeProvider = { callHistoryUpdatesProvider().size },
        itemKeyProvider = { itemIndex -> callHistoryUpdatesProvider()[itemIndex].index },
        modifier = modifier
    ) { itemIndex, visibleProvider, _, itemModifier ->

        HistoryItemCard(
            itemIndex = itemIndex,
            visibleProvider = visibleProvider,
            callHistoryUpdateProvider = { callHistoryUpdatesProvider()[itemIndex].value },
            modifier = itemModifier
        )
    }
}
