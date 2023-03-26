package com.xibasdev.sipcaller.app.view.registration

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
fun RegistrationItemCard(
    itemIndex: Int,
    visibleProvider: () -> Boolean,
    registrationUpdateProvider: () -> AccountRegistrationUpdate,
    modifier: Modifier
) {

    ItemCard(
        itemIndex = itemIndex,
        visibleProvider = visibleProvider,
        modifier = modifier
    ) { contentHeight, actionButtonsHeight ->

        val registrationUpdate = registrationUpdateProvider()

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly,
            modifier = Modifier
                .fillMaxWidth()
                .height(contentHeight)
        ) {
            val text = when (registrationUpdate) {
                RegistryOffline -> "Registry offline"
                NoAccountRegistered -> "No account currently registered"
                is RegisteringAccount -> "Registering account..."
                is RegisterAccountFailed -> "Account registration failed"
                is RegisteredAccount -> "Account registration succeeded"
                is UnregisteringAccount -> "Unregistering account..."
                is UnregisterAccountFailed -> "Account unregistration failed"
                is UnregisteredAccount -> "Account unregistration succeeded"
            }

            Text(
                text = text,
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
            val text = when (registrationUpdate) {
                RegistryOffline -> "Processing suspended"
                NoAccountRegistered -> "Use the form above to register an account"
                is RegisteringAccount -> registrationUpdate.account.toRawString()
                is RegisterAccountFailed -> registrationUpdate.account.toRawString()
                is RegisteredAccount -> registrationUpdate.account.toRawString()
                is UnregisteringAccount -> registrationUpdate.account.toRawString()
                is UnregisterAccountFailed -> registrationUpdate.account.toRawString()
                is UnregisteredAccount -> registrationUpdate.account.toRawString()
            }

            Text(
                text = text,
                fontSize = 12.sp,
                fontWeight = FontWeight.Normal,
                letterSpacing = 0.8.sp,
                color = Color.White,
                modifier = Modifier.wrapContentSize()
            )
        }
    }
}
