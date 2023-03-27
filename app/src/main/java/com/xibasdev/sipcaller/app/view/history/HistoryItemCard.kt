package com.xibasdev.sipcaller.app.view.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.xibasdev.sipcaller.app.view.common.ItemCard
import com.xibasdev.sipcaller.app.view.common.toRawString
import com.xibasdev.sipcaller.sip.calling.CallDirection
import com.xibasdev.sipcaller.sip.history.CallHistoryUpdate
import com.xibasdev.sipcaller.sip.history.CallInvitationAccepted
import com.xibasdev.sipcaller.sip.history.CallInvitationCanceled
import com.xibasdev.sipcaller.sip.history.CallInvitationDeclined
import com.xibasdev.sipcaller.sip.history.CallInvitationDetected
import com.xibasdev.sipcaller.sip.history.CallInvitationFailed
import com.xibasdev.sipcaller.sip.history.CallInvitationMissed
import com.xibasdev.sipcaller.sip.history.CallInviteAcceptedElsewhere
import com.xibasdev.sipcaller.sip.history.CallSessionFailed
import com.xibasdev.sipcaller.sip.history.CallSessionFinishedByCallee
import com.xibasdev.sipcaller.sip.history.CallSessionFinishedByCaller

@Composable
fun HistoryItemCard(
    itemIndex: Int,
    visibleProvider: () -> Boolean,
    callHistoryUpdateProvider: () -> CallHistoryUpdate,
    modifier: Modifier
) {

    ItemCard(
        itemIndex = itemIndex,
        visibleProvider = visibleProvider,
        modifier = modifier
    ) { contentHeight, actionButtonsHeight ->

        val callHistoryUpdate = callHistoryUpdateProvider()

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly,
            modifier = Modifier
                .fillMaxWidth()
                .height(contentHeight)
        ) {

            Text(
                text = getDescription(callHistoryUpdate),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp,
                color = Color.White,
                modifier = Modifier.wrapContentSize()
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly,
            modifier = Modifier
                .fillMaxWidth()
                .height(actionButtonsHeight)
        ) {

            Text(
                text = callHistoryUpdate.remoteAccount.toRawString(),
                fontSize = 12.sp,
                fontWeight = FontWeight.Normal,
                letterSpacing = 0.8.sp,
                color = Color.White,
                modifier = Modifier.wrapContentSize()
            )
        }
    }
}

private fun getDescription(callHistoryUpdate: CallHistoryUpdate): String {
    return when (callHistoryUpdate) {
        is CallInvitationDetected -> when (callHistoryUpdate.callDirection) {
            CallDirection.OUTGOING -> "Outgoing call invitation sent"
            CallDirection.INCOMING -> "Incoming call invitation received"
        }
        is CallInvitationFailed -> "Failed to send outgoing call invitation"
        is CallInvitationCanceled -> "Canceled outgoing call invitation"
        is CallInvitationDeclined -> "Declined incoming call invitation"
        is CallInvitationMissed -> "Missed incoming call invitation"
        is CallInviteAcceptedElsewhere -> "Call invitation accepted elsewhere"
        is CallInvitationAccepted -> "Call invitation accepted; call session established"
        is CallSessionFailed -> "Call session failed: ${callHistoryUpdate.errorReason}"
        is CallSessionFinishedByCallee -> when (callHistoryUpdate.callDirection) {
            CallDirection.OUTGOING -> "Call session finished by your correspondent"
            CallDirection.INCOMING -> "Call session finished by you"
        }
        is CallSessionFinishedByCaller -> when (callHistoryUpdate.callDirection) {
            CallDirection.OUTGOING -> "Call session finished by you"
            CallDirection.INCOMING -> "Call session finished by the destination account"
        }
        else -> ""
    }
}
