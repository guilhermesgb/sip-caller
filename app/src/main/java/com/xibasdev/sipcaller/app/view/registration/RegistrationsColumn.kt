package com.xibasdev.sipcaller.app.view.registration

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
fun RegistrationsColumn(
    registrationUpdatesProvider: () -> List<Indexed<AccountRegistrationUpdate>>,
    onCreateRegistration: (rawAccountRegistration: String) -> Unit,
    onDestroyRegistration: () -> Unit,
    modifier: Modifier
) {

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top,
        modifier = modifier
    ) {

        RegisterAccountView(
            registrationUpdatesProvider = registrationUpdatesProvider,
            onCreateRegistration = onCreateRegistration,
            onDestroyRegistration = onDestroyRegistration
        )

        RecentRegistrationsLog(
            registrationUpdatesProvider = registrationUpdatesProvider,
            modifier = Modifier.fillMaxWidth()
                .weight(1.0f, fill = true)
        )
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun RegisterAccountView(
    registrationUpdatesProvider: () -> List<Indexed<AccountRegistrationUpdate>>,
    onCreateRegistration: (rawAccountRegistration: String) -> Unit,
    onDestroyRegistration: () -> Unit
) {
    val textInput = remember { mutableStateOf("") }

    val registrationUpdates = registrationUpdatesProvider()

    val (enableInputForm, showRegisterButton) = when {
        registrationUpdates.isEmpty() -> false to true
        else -> when (registrationUpdates.first().value) {
            RegistryOffline,
            is RegisteringAccount -> false to true
            is UnregisteringAccount,
            is RegisteredAccount -> false to false
            NoAccountRegistered,
            is RegisterAccountFailed,
            is UnregisterAccountFailed,
            is UnregisteredAccount -> true to true
        }
    }

    if (registrationUpdates.isNotEmpty()) {
        val registrationUpdate = registrationUpdates.first().value

        if (registrationUpdate is RegisteredAccount) {
            textInput.value = registrationUpdate.account.toRawString(includeDisplayName = false)
        }
    }

    TextField(
        value = textInput.value,
        onValueChange = { updatedText ->

            textInput.value = updatedText
        },
        enabled = enableInputForm,
        label = {
            Text(
                text = "Account information: <Display Name> username:password@domain:port",
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
        onClick = {
            if (showRegisterButton) {
                onCreateRegistration(textInput.value)

            } else {
                onDestroyRegistration()
            }
        },
        enabled = enableInputForm || !showRegisterButton,
        shape = AbsoluteRoundedCornerShape(size = 7.dp),
        modifier = Modifier
            .fillMaxWidth(fraction = 0.8f)
            .wrapContentHeight()
    ) {

        Text(
            text = if (showRegisterButton) "Register account" else "Unregister account",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.8.sp
        )
    }
}

@Composable
private fun RecentRegistrationsLog(
    registrationUpdatesProvider: () -> List<Indexed<AccountRegistrationUpdate>>,
    modifier: Modifier
) {

    ItemsColumn(
        columnName = "Registrations log (recent)",
        sizeProvider = { registrationUpdatesProvider().size },
        itemKeyProvider = { itemIndex -> registrationUpdatesProvider()[itemIndex].index },
        modifier = modifier
    ) { itemIndex, visibleProvider, _, itemModifier ->

        RegistrationItemCard(
            itemIndex = itemIndex,
            visibleProvider = visibleProvider,
            registrationUpdateProvider = { registrationUpdatesProvider()[itemIndex].value },
            modifier = itemModifier
        )
    }
}
