package com.example.income

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AgentBoardsTest {
    @Test
    fun `every board is in exactly one list`() {
        assertEquals(AgentBoards.ALL.size, AgentBoards.escrowConfirmed.size + AgentBoards.notVerified.size)
        assertEquals(AgentBoards.ALL.size, AgentBoards.ALL.map { it.name }.toSet().size)
        assertTrue(AgentBoards.escrowConfirmed.isNotEmpty() && AgentBoards.notVerified.isNotEmpty())
    }

    @Test
    fun `links are https and every board cites a source`() {
        AgentBoards.ALL.forEach { b ->
            assertTrue(b.name, b.sourceUrl.startsWith("https://"))
            b.url?.let { assertTrue(b.name, it.startsWith("https://")) }
            assertTrue(b.name, b.escrow.isNotBlank() && b.caveat.isNotBlank())
        }
    }

    @Test
    fun `claw earn is confirmed and shown live`() {
        val claw = AgentBoards.ALL.single { it.liveInApp }
        assertEquals("Claw Earn", claw.name)
        assertEquals(AgentBoards.Tier.ESCROW_CONFIRMED, claw.tier)
    }
}
