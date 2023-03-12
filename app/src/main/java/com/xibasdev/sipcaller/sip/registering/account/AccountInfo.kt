package com.xibasdev.sipcaller.sip.registering.account

import com.xibasdev.sipcaller.sip.protocol.ProtocolInfo
import com.xibasdev.sipcaller.sip.protocol.ProtocolType.TCP
import com.xibasdev.sipcaller.sip.protocol.RandomPort
import com.xibasdev.sipcaller.sip.registering.account.address.AccountAddress
import com.xibasdev.sipcaller.sip.registering.account.address.AccountIp
import com.xibasdev.sipcaller.sip.registering.account.address.AccountIpAddress

data class AccountInfo(
    val displayName: AccountDisplayName = AccountDisplayName(""),
    val username: AccountUsername = AccountUsername(""),
    val address: AccountAddress = AccountIpAddress(
        protocol = ProtocolInfo(
            type = TCP,
            port = RandomPort
        ),
        ip = AccountIp("127.0.0.1")
    )
)
