package com.xibasdev.sipcaller.sip.registering.account

import com.xibasdev.sipcaller.sip.registering.account.address.AccountAddress
import com.xibasdev.sipcaller.sip.registering.account.address.AccountIp
import com.xibasdev.sipcaller.sip.registering.account.address.AccountIpAddress
import com.xibasdev.sipcaller.sip.registering.account.address.protocol.AccountProtocolInfo
import com.xibasdev.sipcaller.sip.registering.account.address.protocol.AccountProtocolPort
import com.xibasdev.sipcaller.sip.registering.account.address.protocol.AccountProtocolType.TCP

data class AccountInfo(
    val displayName: AccountDisplayName = AccountDisplayName(""),
    val username: AccountUsername = AccountUsername(""),
    val address: AccountAddress = AccountIpAddress(
        protocol = AccountProtocolInfo(
            type = TCP,
            port = AccountProtocolPort(5060)
        ),
        ip = AccountIp("127.0.0.1")
    )
)
