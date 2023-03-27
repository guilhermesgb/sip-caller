package com.xibasdev.sipcaller.app.view.identity

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xibasdev.sipcaller.app.view.common.toRawString
import com.xibasdev.sipcaller.app.viewmodel.common.OperationFailed
import com.xibasdev.sipcaller.app.viewmodel.common.OperationRunning
import com.xibasdev.sipcaller.app.viewmodel.common.OperationSucceeded
import com.xibasdev.sipcaller.app.viewmodel.common.ViewModelEvent
import com.xibasdev.sipcaller.app.viewmodel.events.account.registering.CreatingRegistration
import com.xibasdev.sipcaller.app.viewmodel.events.account.registering.RegistrationCreateFailed
import com.xibasdev.sipcaller.app.viewmodel.events.account.registering.RegistrationCreated
import com.xibasdev.sipcaller.app.viewmodel.events.account.unregistering.DestroyingRegistration
import com.xibasdev.sipcaller.app.viewmodel.events.account.unregistering.RegistrationDestroyFailed
import com.xibasdev.sipcaller.app.viewmodel.events.account.unregistering.RegistrationDestroyed
import com.xibasdev.sipcaller.app.viewmodel.events.calling.accept.AcceptCallInvitationFailed
import com.xibasdev.sipcaller.app.viewmodel.events.calling.accept.AcceptingCallInvitation
import com.xibasdev.sipcaller.app.viewmodel.events.calling.accept.CallInvitationAccepted
import com.xibasdev.sipcaller.app.viewmodel.events.calling.cancel.CallInvitationCanceled
import com.xibasdev.sipcaller.app.viewmodel.events.calling.cancel.CancelCallInvitationFailed
import com.xibasdev.sipcaller.app.viewmodel.events.calling.cancel.CancelingCallInvitation
import com.xibasdev.sipcaller.app.viewmodel.events.calling.decline.CallInvitationDeclined
import com.xibasdev.sipcaller.app.viewmodel.events.calling.decline.DeclineCallInvitationFailed
import com.xibasdev.sipcaller.app.viewmodel.events.calling.decline.DecliningCallInvitation
import com.xibasdev.sipcaller.app.viewmodel.events.calling.send.CallInvitationSent
import com.xibasdev.sipcaller.app.viewmodel.events.calling.send.SendCallInvitationFailed
import com.xibasdev.sipcaller.app.viewmodel.events.calling.send.SendingCallInvitation
import com.xibasdev.sipcaller.app.viewmodel.events.calling.surfaces.local.destroy.DestroyLocalSurfaceFailed
import com.xibasdev.sipcaller.app.viewmodel.events.calling.surfaces.local.destroy.DestroyingLocalSurface
import com.xibasdev.sipcaller.app.viewmodel.events.calling.surfaces.local.destroy.LocalSurfaceDestroyed
import com.xibasdev.sipcaller.app.viewmodel.events.calling.surfaces.local.update.LocalSurfaceUpdated
import com.xibasdev.sipcaller.app.viewmodel.events.calling.surfaces.local.update.UpdateLocalSurfaceFailed
import com.xibasdev.sipcaller.app.viewmodel.events.calling.surfaces.local.update.UpdatingLocalSurface
import com.xibasdev.sipcaller.app.viewmodel.events.calling.surfaces.remote.destroy.DestroyRemoteSurfaceFailed
import com.xibasdev.sipcaller.app.viewmodel.events.calling.surfaces.remote.destroy.DestroyingRemoteSurface
import com.xibasdev.sipcaller.app.viewmodel.events.calling.surfaces.remote.destroy.RemoteSurfaceDestroyed
import com.xibasdev.sipcaller.app.viewmodel.events.calling.surfaces.remote.update.RemoteSurfaceUpdated
import com.xibasdev.sipcaller.app.viewmodel.events.calling.surfaces.remote.update.UpdateRemoteSurfaceFailed
import com.xibasdev.sipcaller.app.viewmodel.events.calling.surfaces.remote.update.UpdatingRemoteSurface
import com.xibasdev.sipcaller.app.viewmodel.events.calling.terminate.CallSessionTerminated
import com.xibasdev.sipcaller.app.viewmodel.events.calling.terminate.TerminateCallSessionFailed
import com.xibasdev.sipcaller.app.viewmodel.events.calling.terminate.TerminatingCallSession
import com.xibasdev.sipcaller.sip.identity.IdentityUpdate
import com.xibasdev.sipcaller.sip.identity.LocalIdentity
import com.xibasdev.sipcaller.sip.identity.RemoteIdentity
import com.xibasdev.sipcaller.sip.identity.UnreachableIdentity

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CurrentIdentityView(
    eventUpdatesProvider: () -> ViewModelEvent,
    identityUpdatesProvider: () -> IdentityUpdate
) {

    val eventUpdate = eventUpdatesProvider()

    if (eventUpdate is OperationFailed) {
        val text = when (eventUpdate) {
            is RegistrationCreateFailed -> {
                "Failed to create registration: ${eventUpdate.error.message}"
            }
            is RegistrationDestroyFailed -> {
                "Failed to destroy registration: ${eventUpdate.error.message}"
            }
            is SendCallInvitationFailed -> {
                "Failed to send call invitation: ${eventUpdate.error.message}"
            }
            is CancelCallInvitationFailed -> {
                "Failed to cancel call invitation: ${eventUpdate.error.message}"
            }
            is DeclineCallInvitationFailed -> {
                "Failed to decline call invitation: ${eventUpdate.error.message}"
            }
            is AcceptCallInvitationFailed -> {
                "Failed to accept call invitation: ${eventUpdate.error.message}"
            }
            is TerminateCallSessionFailed -> {
                "Failed to terminate call session: ${eventUpdate.error.message}"
            }
            is UpdateLocalSurfaceFailed -> {
                "Failed to update local surface: ${eventUpdate.error.message}"
            }
            is DestroyLocalSurfaceFailed -> {
                "Failed to destroy local surface: ${eventUpdate.error.message}"
            }
            is UpdateRemoteSurfaceFailed -> {
                "Failed to update remote surface: ${eventUpdate.error.message}"
            }
            is DestroyRemoteSurfaceFailed -> {
                "Failed to destroy remote surface: ${eventUpdate.error.message}"
            }
            else -> "Unknown error"
        }

        Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .clip(
                    shape = RoundedCornerShape(7.dp)
                )
                .background(color = Color.Red)
        ) {
            Text(
                text = text,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp,
                color = Color.White,
                maxLines = 1,
                modifier = Modifier.wrapContentSize()
                    .padding(horizontal = 8.dp)
                    .basicMarquee()
            )
        }

    } else {
        val text = when (val identityUpdate = identityUpdatesProvider()) {
            is LocalIdentity -> {
                val ip = identityUpdate.address.ip.value
                val port = identityUpdate.address.protocol.port.value

                "You're listening on local address: $ip:$port"
            }
            is RemoteIdentity -> {
                val account = identityUpdate.account.toRawString(includeDisplayName = false)

                "You're listening using remote account: $account"
            }
            UnreachableIdentity -> "You're currently unreachable"
        }

        val complement = when (eventUpdate) {
            is OperationRunning -> when (eventUpdate) {
                is CreatingRegistration -> {
                    "; Creating registration..."
                }
                is DestroyingRegistration -> {
                    "; Destroying registration..."
                }
                is SendingCallInvitation -> {
                    "; Sending call invitation..."
                }
                is CancelingCallInvitation -> {
                    "; Canceling call invitation..."
                }
                is DecliningCallInvitation -> {
                    "; Declining call invitation..."
                }
                is AcceptingCallInvitation -> {
                    "; Accepting call invitation..."
                }
                is TerminatingCallSession -> {
                    "; Terminating call session..."
                }
                is UpdatingLocalSurface -> {
                    "; Updating local surface..."
                }
                is DestroyingLocalSurface -> {
                    "; Destroying local surface..."
                }
                is UpdatingRemoteSurface -> {
                    "; Updating remote surface..."
                }
                is DestroyingRemoteSurface -> {
                    "; Destroying remote surface..."
                }
                else -> ""
            }
            is OperationSucceeded -> when (eventUpdate) {
                is RegistrationCreated -> {
                    "; Registration created"
                }
                is RegistrationDestroyed -> {
                    "; Registration destroyed"
                }
                is CallInvitationSent -> {
                    "; Call invitation sent"
                }
                is CallInvitationCanceled -> {
                    "; Call invitation canceled"
                }
                is CallInvitationDeclined -> {
                    "; Call invitation declined"
                }
                is CallInvitationAccepted -> {
                    "; Call invitation accepted"
                }
                is CallSessionTerminated -> {
                    "; Call session terminated"
                }
                is LocalSurfaceUpdated -> {
                    "; Local surface updated"
                }
                is LocalSurfaceDestroyed -> {
                    "; Local surface destroyed"
                }
                is RemoteSurfaceUpdated -> {
                    "; Remote surface updated"
                }
                is RemoteSurfaceDestroyed -> {
                    "; Remote surface destroyed"
                }
                else -> ""
            }
            else -> ""
        }

        Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .clip(
                    shape = RoundedCornerShape(7.dp)
                )
                .background(color = MaterialTheme.colorScheme.secondary)
        ) {
            Text(
                text = text + complement,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp,
                color = Color.White,
                maxLines = 1,
                modifier = Modifier.wrapContentSize()
                    .padding(horizontal = 8.dp)
                    .basicMarquee()
            )
        }
    }
}
