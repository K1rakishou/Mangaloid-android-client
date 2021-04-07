package com.github.mangaloid.client.core.misc

import okhttp3.Dns
import java.net.Inet4Address
import java.net.InetAddress
import java.net.UnknownHostException
import java.util.*

/**
 * Dns selector that only allows connections to ipv4 addresses (Yeah some people still have no ipv6).
 * */
class MangaloidDnsSelector : Dns {

  @Throws(UnknownHostException::class)
  override fun lookup(hostname: String): List<InetAddress> {
    val addresses = Dns.SYSTEM.lookup(hostname)

    val resultAddresses: MutableList<InetAddress> = ArrayList()
    for (address in addresses) {
      if (address is Inet4Address) {
        resultAddresses.add(address)
      }
    }

    return resultAddresses
  }

}