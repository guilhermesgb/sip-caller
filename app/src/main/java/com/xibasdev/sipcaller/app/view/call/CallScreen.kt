package com.xibasdev.sipcaller.app.view.call

import android.view.SurfaceView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.AbsoluteRoundedCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rxjava3.subscribeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import com.xibasdev.sipcaller.app.view.common.VideoFeed
import com.xibasdev.sipcaller.app.view.common.toRawString
import com.xibasdev.sipcaller.app.view.identity.CurrentIdentityView
import com.xibasdev.sipcaller.app.viewmodel.call.CallViewModel
import com.xibasdev.sipcaller.app.viewmodel.common.ScreenRendered
import com.xibasdev.sipcaller.sip.calling.CallDirection.INCOMING
import com.xibasdev.sipcaller.sip.calling.CallDirection.OUTGOING
import com.xibasdev.sipcaller.sip.calling.CallId
import com.xibasdev.sipcaller.sip.calling.CallStage.SESSION
import com.xibasdev.sipcaller.sip.calling.CallStatus
import com.xibasdev.sipcaller.sip.calling.CallStatus.ABORTED_DUE_TO_ERROR
import com.xibasdev.sipcaller.sip.calling.CallStatus.ACCEPTED
import com.xibasdev.sipcaller.sip.calling.CallStatus.ACCEPTED_ELSEWHERE
import com.xibasdev.sipcaller.sip.calling.CallStatus.CANCELED
import com.xibasdev.sipcaller.sip.calling.CallStatus.DECLINED
import com.xibasdev.sipcaller.sip.calling.CallStatus.FINISHED_BY_LOCAL_PARTY
import com.xibasdev.sipcaller.sip.calling.CallStatus.FINISHED_BY_REMOTE_PARTY
import com.xibasdev.sipcaller.sip.calling.CallStatus.FINISHED_DUE_TO_ERROR
import com.xibasdev.sipcaller.sip.calling.CallStatus.MISSED
import com.xibasdev.sipcaller.sip.calling.CallStatus.RINGING
import com.xibasdev.sipcaller.sip.calling.details.CallInvitationUpdate
import com.xibasdev.sipcaller.sip.calling.details.CallSessionUpdate
import com.xibasdev.sipcaller.sip.calling.details.CallUpdate
import com.xibasdev.sipcaller.sip.calling.details.NoCallUpdateAvailable
import com.xibasdev.sipcaller.sip.identity.UnreachableIdentity
import com.xibasdev.sipcaller.sip.registering.account.isCaller

@Composable
fun CallScreen(
    viewModel: CallViewModel = hiltViewModel(),
    lifecycle: Lifecycle,
    callId: CallId
) {

    viewModel.onUpdateLifecycle(lifecycle)

    val eventUpdates = viewModel.observeEvents()
        .subscribeAsState(initial = ScreenRendered)
    
    val callInProgressUpdates = viewModel.observeCallInProgress(callId)
        .subscribeAsState(initial = true)

    val callDetailsUpdates = viewModel.observeCallDetails(callId)
        .subscribeAsState(initial = NoCallUpdateAvailable)

    val identityUpdates = viewModel.observeIdentity()
        .subscribeAsState(initial = UnreachableIdentity)

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.fillMaxSize()
    ) {
        LiveVideoFeeds(
            callInProgressUpdatesProvider = { callInProgressUpdates.value },
            onUpdateLocalSurfaceView = viewModel::onUpdateLocalSurfaceView,
            onUpdateRemoteSurfaceView = viewModel::onUpdateRemoteSurfaceView
        )

        BasicCallInfo(
            callDetailsUpdatesProvider = { callDetailsUpdates.value },
            modifier = Modifier.align(alignment = Alignment.TopCenter)
                .fillMaxWidth(fraction = 0.97f)
                .wrapContentHeight()
                .padding(top = 20.dp)
        )

        CentralMessageText(
            callDetailsUpdatesProvider = { callDetailsUpdates.value },
            modifier = Modifier.align(alignment = Alignment.Center)
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceAround,
            modifier = Modifier.align(alignment = Alignment.BottomCenter)
                .fillMaxWidth(fraction = 0.97f)
                .wrapContentHeight()
                .padding(bottom = 20.dp)
        ) {

            CallActionButtons(
                callId = callId,
                callDetailsUpdatesProvider = { callDetailsUpdates.value },
                onCancelCallInvitation = viewModel::cancelCallInvitation,
                onDeclineCallInvitation = viewModel::declineCallInvitation,
                onAcceptCallInvitation = viewModel::acceptCallInvitation,
                onTerminateCallSession = viewModel::terminateCallSession
            )

            CurrentIdentityView(
                eventUpdatesProvider = { eventUpdates.value },
                identityUpdatesProvider = { identityUpdates.value }
            )
        }
    }
}

@Composable
fun LiveVideoFeeds(
    callInProgressUpdatesProvider: () -> Boolean,
    onUpdateLocalSurfaceView: (surfaceView: SurfaceView?) -> Unit,
    onUpdateRemoteSurfaceView: (surfaceView: SurfaceView?) -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        val callInProgress = callInProgressUpdatesProvider()

        if (callInProgress) {
            VideoFeed(
                onUpdateSurfaceView = onUpdateRemoteSurfaceView,
                modifier = Modifier.fillMaxSize()
            )

            VideoFeed(
                onUpdateSurfaceView = onUpdateLocalSurfaceView,
                modifier = Modifier
                    .fillMaxSize(0.3f)
                    .align(Alignment.CenterEnd)
            )

        } else {
            onUpdateLocalSurfaceView(null)
            onUpdateRemoteSurfaceView(null)

            Box(
                modifier = Modifier.fillMaxSize()
                    .background(color = Color.Black)
            ) {}
        }
    }
}

@Composable
private fun BasicCallInfo(
    callDetailsUpdatesProvider: () -> CallUpdate,
    modifier: Modifier
) {
    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.clip(shape = RoundedCornerShape(7.dp))
            .background(color = MaterialTheme.colorScheme.secondary)
    ) {

        val call = when (val callDetailsUpdate = callDetailsUpdatesProvider()) {
            is CallInvitationUpdate -> callDetailsUpdate.call
            is CallSessionUpdate -> callDetailsUpdate.call
            NoCallUpdateAvailable -> null
        } ?: return@Column

        with (call) {
            val rawDirection = direction.name.lowercase().replaceFirstChar { it.uppercase() }
            val rawStage = stage.name.lowercase()
            val rawRemoteAccount = parties.remote.toRawString(includeDisplayName = false)
            val remoteIsCaller = parties.remote.isCaller()
            val perspective = if (remoteIsCaller) "from" else "to"
            val rawAudioCodec = streams.audio.codec.codecName
            val rawVideoCodec = streams.video.codec.codecName

            val text = "$rawDirection call $rawStage $perspective $rawRemoteAccount" +
                    (if (!status.isTerminal) " / $rawAudioCodec, $rawVideoCodec" else "") +
                    (if (stage == SESSION) " / ${durationMs / 1000} seconds" else "")

            Text(
                text = text,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp,
                color = Color.White,
                modifier = Modifier.wrapContentSize()
            )
        }
    }
}

@Composable
fun CentralMessageText(
    callDetailsUpdatesProvider: () -> CallUpdate,
    modifier: Modifier
) {

    val text = when (val callDetailsUpdate = callDetailsUpdatesProvider()) {
        is CallInvitationUpdate -> if (callDetailsUpdate.call.status.isTerminal) {
            getDescription(callDetailsUpdate.call.status)
        } else "Ringing..."
        is CallSessionUpdate -> if (callDetailsUpdate.call.status.isTerminal) {
            getDescription(callDetailsUpdate.call.status)
        } else ""
        NoCallUpdateAvailable -> "No call update available"
    }

    Text(
        text = text,
        fontSize = 24.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.8.sp,
        color = Color.White,
        modifier = modifier
    )
}

fun getDescription(status: CallStatus): String {
    return when (status) {
        CANCELED -> "Canceled outgoing call invitation"
        MISSED -> "Missed incoming call invitation"
        ACCEPTED_ELSEWHERE -> "Call invitation accepted elsewhere"
        DECLINED -> "Declined incoming call invitation"
        ABORTED_DUE_TO_ERROR -> "Failed to send outgoing call invitation"
        FINISHED_BY_LOCAL_PARTY -> "Call session finished by you"
        FINISHED_BY_REMOTE_PARTY -> "Call session finished by your correspondent"
        FINISHED_DUE_TO_ERROR -> "Call session finished due to failure"
        else -> ""
    }
}

@Composable
private fun CallActionButtons(
    callId: CallId,
    callDetailsUpdatesProvider: () -> CallUpdate,
    onCancelCallInvitation: (CallId) -> Unit,
    onDeclineCallInvitation: (CallId) -> Unit,
    onAcceptCallInvitation: (CallId) -> Unit,
    onTerminateCallSession: (CallId) -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentSize()
    ) {
        when (val callDetailsUpdate = callDetailsUpdatesProvider()) {
            is CallInvitationUpdate -> if (callDetailsUpdate.call.status == RINGING) {
                when (callDetailsUpdate.call.direction) {
                    OUTGOING -> CallActionButton(
                        onClick = { onCancelCallInvitation(callId) },
                        text = "Cancel outgoing call invitation"
                    )
                    INCOMING -> {
                        CallActionButton(
                            onClick = { onAcceptCallInvitation(callId) },
                            text = "Accept incoming call invitation"
                        )

                        CallActionButton(
                            onClick = { onDeclineCallInvitation(callId) },
                            text = "Decline incoming call invitation"
                        )
                    }
                }
            }
            is CallSessionUpdate -> if (callDetailsUpdate.call.status == ACCEPTED) {
                CallActionButton(
                    onClick = { onTerminateCallSession(callId) },
                    text = "Terminate ongoing call session"
                )
            }
            NoCallUpdateAvailable -> {}
        }
    }
}

@Composable
private fun CallActionButton(
    onClick: () -> Unit,
    text: String
) {
    Button(
        onClick = onClick,
        shape = AbsoluteRoundedCornerShape(size = 7.dp),
        modifier = Modifier.wrapContentSize()
    ) {

        Text(
            text = text,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.8.sp
        )
    }
}
