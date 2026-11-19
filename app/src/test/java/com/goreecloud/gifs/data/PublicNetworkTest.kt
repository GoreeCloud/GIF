package com.goreecloud.gifs.data

import okhttp3.Dns
import org.junit.Assert.*
import org.junit.Test
import java.net.InetAddress
import java.net.UnknownHostException

class PublicNetworkTest {
    private fun ip(literal: String) = InetAddress.getByName(literal)

    @Test fun blocksNonPublicIpv4Ranges() {
        for (value in listOf(
            "0.0.0.0", "10.1.2.3", "127.0.0.1", "169.254.10.3",
            "172.16.0.1", "192.168.0.1", "100.64.1.5",
            "198.19.4.8", "192.0.2.1", "198.51.100.2", "203.0.113.2",
            "224.0.0.1", "255.255.255.255"
        )) assertFalse(value, PublicAddressPolicy.allows(ip(value)))
    }

    @Test fun blocksNonPublicIpv6Ranges() {
        for (value in listOf(
            "::1", "fc00::1", "fd12:3456::1", "fe80::1", "2001:db8::1"
        )) assertFalse(value, PublicAddressPolicy.allows(ip(value)))
    }

    @Test fun acceptsRoutablePublicAddresses() {
        assertTrue(PublicAddressPolicy.allows(ip("1.1.1.1")))
        assertTrue(PublicAddressPolicy.allows(ip("8.8.8.8")))
        assertTrue(PublicAddressPolicy.allows(ip("2606:4700:4700::1111")))
    }

    @Test fun redirectPoliciesAreExplicit() {
        val downloads = mediaHttpClient()
        assertFalse(downloads.followRedirects)
        assertFalse(downloads.followSslRedirects)
        val previews = mediaHttpClient(allowHttpsRedirects = true)
        assertTrue(previews.followRedirects)
        assertFalse(previews.followSslRedirects)
    }

    @Test fun mixedDnsAnswersFailClosed() {
        val dns = PublicDns(object : Dns {
            override fun lookup(hostname: String): List<InetAddress> =
                listOf(ip("8.8.8.8"), ip("127.0.0.1"))
        })
        assertThrows(UnknownHostException::class.java) { dns.lookup("example.org") }
    }

    @Test fun publicOnlyDnsAnswersArePermitted() {
        val dns = PublicDns(object : Dns {
            override fun lookup(hostname: String): List<InetAddress> =
                listOf(ip("1.1.1.1"))
        })
        assertEquals(1, dns.lookup("example.org").size)
        assertThrows(UnknownHostException::class.java) { dns.lookup("localhost") }
    }
}
