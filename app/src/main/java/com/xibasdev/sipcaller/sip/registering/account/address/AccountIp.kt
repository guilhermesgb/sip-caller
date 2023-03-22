package com.xibasdev.sipcaller.sip.registering.account.address

/**
 * This value encodes the IPv4 address of a given call party. Mostly used by parties in local calls.
 *
 * TODO consider adding support for IPV6 addresses as well.
 */
@JvmInline
value class AccountIp(val value: String)

val LOOPBACK_IP_ADDRESS = AccountIp("127.0.0.1")
