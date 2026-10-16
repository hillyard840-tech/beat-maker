package com.example.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.net.InetAddress

class UrlSafetyTest {
    // Fake DNS so the tests never touch the network.
    private fun dns(vararg ips: String): (String) -> List<InetAddress> =
        { _ -> ips.map { InetAddress.getByName(it) } }

    @Test
    fun `allows public http and https urls`() {
        assertNull(UrlSafety.rejectReason("https://example.com/page", dns("93.184.216.34")))
        assertNull(UrlSafety.rejectReason("http://example.com", dns("2606:2800:220:1:248:1893:25c8:1946")))
    }

    @Test
    fun `blocks non web schemes and embedded credentials`() {
        assertNotNull(UrlSafety.rejectReason("file:///data/data/com.example/databases/chat-database"))
        assertNotNull(UrlSafety.rejectReason("ftp://example.com/file", dns("93.184.216.34")))
        assertNotNull(UrlSafety.rejectReason("https://user:pass@example.com", dns("93.184.216.34")))
    }

    @Test
    fun `blocks local network hosts and addresses`() {
        listOf(
            "127.0.0.1", "10.0.0.5", "172.16.3.4", "192.168.1.1", "169.254.169.254",
            "100.64.0.1", "0.0.0.0", "::1", "fd00::1", "fe80::1", "::ffff:192.168.1.1"
        ).forEach { ip ->
            assertNotNull("expected $ip to be blocked", UrlSafety.rejectReason("https://evil.example", dns(ip)))
        }
        assertNotNull(UrlSafety.rejectReason("http://localhost:8080", dns("93.184.216.34")))
        assertNotNull(UrlSafety.rejectReason("http://printer.local", dns("93.184.216.34")))
    }

    @Test
    fun `blocks a host if any resolved address is private`() {
        assertNotNull(UrlSafety.rejectReason("https://rebind.example", dns("93.184.216.34", "192.168.0.1")))
    }

    @Test
    fun `rejects unresolvable hosts`() {
        assertEquals("Could not resolve host", UrlSafety.rejectReason("https://nope.example", dns()))
    }
}
