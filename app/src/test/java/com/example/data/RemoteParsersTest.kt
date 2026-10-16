package com.example.data

import com.example.data.clawearn.ClawEarnParser
import com.example.data.moltbook.MoltbookParser
import com.example.data.remote.JsonHttp
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RemoteParsersTest {
    private fun json(s: String) = Json.parseToJsonElement(s)

    @Test
    fun `parses moltbook registration with nested agent`() {
        val reg = MoltbookParser.registration(
            json("""{"agent":{"api_key":"moltbook_abc","claim_url":"https://www.moltbook.com/claim/x","verification_code":"reef-X4B2"},"important":"save it"}""")
        )
        assertEquals("moltbook_abc", reg.apiKey)
        assertEquals("https://www.moltbook.com/claim/x", reg.claimUrl)
        assertEquals("reef-X4B2", reg.verificationCode)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `registration without a key is an error, not a made-up key`() {
        MoltbookParser.registration(json("""{"success":true}"""))
    }

    @Test
    fun `parses moltbook posts with nested or flat author and submolt`() {
        val posts = MoltbookParser.posts(
            json(
                """{"success":true,"posts":[
                  {"id":"p1","title":"Hello","content":"Body","author":{"name":"alice"},"submolt":{"name":"general"},"upvotes":5,"downvotes":1,"comment_count":3,"created_at":"2026-01-01T00:00:00Z"},
                  {"id":"p2","title":"Link","url":"https://example.com","author_name":"bob","submolt":"tech"},
                  {"title":"no id, skipped"}
                ]}"""
            )
        )
        assertEquals(2, posts.size)
        assertEquals("alice", posts[0].authorName)
        assertEquals("general", posts[0].submoltName)
        assertEquals(4, posts[0].score)
        assertEquals(3, posts[0].commentCount)
        assertEquals("bob", posts[1].authorName)
        assertEquals("tech", posts[1].submoltName)
        assertEquals("https://example.com", posts[1].url)
        assertEquals(0, posts[1].upvotes)
    }

    @Test
    fun `parses claim status and agent profile`() {
        assertEquals("pending_claim", MoltbookParser.claimStatus(json("""{"status":"pending_claim"}""")))
        val agent = MoltbookParser.agent(json("""{"success":true,"agent":{"name":"kora","karma":12,"is_claimed":true}}"""))!!
        assertEquals("kora", agent.name)
        assertEquals(12, agent.karma)
        assertEquals(true, agent.isClaimed)
        assertNull(MoltbookParser.agent(json("""{"success":true}""")))
    }

    @Test
    fun `parses claw earn task page without inventing values`() {
        val page = ClawEarnParser.taskPage(
            json(
                """{"counts":{"available":2,"occupied":1,"completed":9},"items":[
                  {"taskId":"7","contractAddress":"0xabc","title":"Write a post","amountUsdc":5.5,"status":"FUNDED"},
                  {"taskId":"8","contractAddress":"0xdef","metadata":{"title":"From metadata","description":"d"},"amount":2500000,"status":"STAKED"},
                  {"taskId":"9","status":"FUNDED"}
                ]}"""
            )
        )
        assertEquals(2, page.available)
        assertEquals(9, page.completed)
        assertEquals(3, page.tasks.size)
        assertEquals(5.5, page.tasks[0].amountUsdc!!, 0.0)
        assertEquals("From metadata", page.tasks[1].title)
        assertEquals(2.5, page.tasks[1].amountUsdc!!, 0.0)
        assertNull("missing amount must stay unknown", page.tasks[2].amountUsdc)
    }

    @Test
    fun `claw earn health keeps missing fields null`() {
        val h = ClawEarnParser.health(json("""{"ok":true}"""))
        assertTrue(h.ok)
        assertNull(h.chainId)
        assertTrue(h.contracts.isEmpty())
    }

    @Test
    fun `extracts server error messages`() {
        val body = json("""{"success":false,"error":"Agent not claimed","hint":"Ask your human to claim"}""") as JsonObject
        assertEquals("Agent not claimed (Ask your human to claim)", JsonHttp.errorMessageOf(body))
        assertEquals("quota", JsonHttp.errorMessageOf(json("""{"error":{"message":"quota"}}""") as JsonObject))
        assertNull(JsonHttp.errorMessageOf(json("""{"error":false}""") as JsonObject))
    }
}
