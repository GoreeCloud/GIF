package com.goreecloud.gifs.data

import okhttp3.Dns
import java.net.InetAddress
import java.net.UnknownHostException

/**
 * Network address policy for untrusted media hosts.
 * Denies loopback, local, reserved and benchmark ranges, including IPv6 ULA.
 */
object PublicAddressPolicy {
    fun allows(address: InetAddress): Boolean {
        if (address.isAnyLocalAddress || address.isLoopbackAddress ||
            address.isLinkLocalAddress || address.isSiteLocalAddress ||
            address.isMulticastAddress) return false

        val octets = address.address.map { it.toInt() and 0xff }
        return when (octets.size) {
            4 -> {
                val (a, b, c) = octets
                when {
                    a == 0 || a == 10 || a == 127 || a >= 224 -> false
                    a == 100 && b in 64..127 -> false // CGNAT
                    a == 169 && b == 254 -> false
                    a == 172 && b in 16..31 -> false
                    a == 192 && b == 168 -> false
                    a == 192 && b == 0 && c == 0 -> false
                    a == 192 && b == 0 && c == 2 -> false // TEST-NET-1
                    a == 198 && b in 18..19 -> false // benchmark
                    a == 198 && b == 51 && c == 100 -> false // TEST-NET-2
                    a == 203 && b == 0 && c == 113 -> false // TEST-NET-3
                    else -> true
                }
            }
            16 -> {
                val a = octets[0]
                val b = octets[1]
                val c = octets[2]
                val e = octets[3]
                when {
                    (a and 0xfe) == 0xfc -> false // fc00::/7 ULA
                    a == 0x20 && b == 0x01 && c == 0x0d && e == 0xb8 -> false
                    else -> true
                }
            }
            else -> false
        }
    }
}

/** Rejects a DNS name entirely if any returned address could reach a private endpoint. */
class PublicDns(private val delegate: Dns = Dns.SYSTEM) : Dns {
    override fun lookup(hostname: String): List<InetAddress> {
        if (!SourcePolicy.isSafeHttps("https://$hostname/")) {
            throw UnknownHostException("Unsafe media host.")
        }
        val addresses = delegate.lookup(hostname)
        if (addresses.isEmpty() || addresses.any { !PublicAddressPolicy.allows(it) }) {
            throw UnknownHostException("Media host resolved to a blocked address.")
        }
        return addresses
    }
}
