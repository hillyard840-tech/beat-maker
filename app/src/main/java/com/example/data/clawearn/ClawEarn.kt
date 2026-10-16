package com.example.data.clawearn

import com.example.data.remote.JsonHttp
import com.example.data.remote.arrayOf
import com.example.data.remote.bool
import com.example.data.remote.num
import com.example.data.remote.obj
import com.example.data.remote.str
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import java.net.URLEncoder

data class ClawEarnTask(
    val taskId: String,
    val contractAddress: String,
    val title: String,
    val description: String,
    /** Null when the API didn't report an amount; never guessed. */
    val amountUsdc: Double?,
    val status: String,
    val category: String?,
    val poster: String?,
    val worker: String?,
    val createdAtEpochSec: Long?
)

data class ClawEarnHealth(
    val ok: Boolean,
    val chainId: String?,
    val latestBlock: Long?,
    val contracts: List<String>
)

data class ClawEarnTaskPage(
    val tasks: List<ClawEarnTask>,
    val available: Int?,
    val occupied: Int?,
    val completed: Int?
)

/** Parses aiagentstore.ai Claw Earn responses. Missing values stay null instead of being invented. */
object ClawEarnParser {
    fun health(root: JsonElement): ClawEarnHealth {
        val o = root as? JsonObject ?: throw IllegalArgumentException("Unexpected health response")
        val contracts = (o["contracts"] as? JsonArray)
            ?.mapNotNull { (it as? JsonPrimitive)?.contentOrNull ?: (it as? JsonObject)?.str("address") }
            .orEmpty()
        return ClawEarnHealth(
            ok = o.bool("ok") ?: (o.str("status")?.lowercase() in setOf("ok", "online", "healthy")),
            chainId = o.str("chainId", "chain_id"),
            latestBlock = o.num("latestBlock", "latest_block")?.toLong(),
            contracts = contracts
        )
    }

    fun taskPage(root: JsonElement): ClawEarnTaskPage {
        val counts = (root as? JsonObject)?.obj("counts")
        return ClawEarnTaskPage(
            tasks = root.arrayOf("items", "tasks", "data").mapNotNull { task(it) },
            available = counts?.num("available")?.toInt(),
            occupied = counts?.num("occupied")?.toInt(),
            completed = counts?.num("completed")?.toInt()
        )
    }

    fun task(o: JsonObject): ClawEarnTask? {
        val taskId = o.str("taskId", "task_id", "id") ?: return null
        val metadata = o.obj("metadata")
        val amount = o.num("amountUsdc", "amount_usdc")
            ?: o.num("amount")?.let { it / 1_000_000.0 } // on-chain USDC has 6 decimals
        return ClawEarnTask(
            taskId = taskId,
            contractAddress = o.str("contractAddress", "contract_address").orEmpty(),
            title = o.str("title") ?: metadata?.str("title") ?: "Task #$taskId",
            description = o.str("description") ?: metadata?.str("description").orEmpty(),
            amountUsdc = amount,
            status = o.str("status") ?: "UNKNOWN",
            category = o.str("category"),
            poster = o.str("poster"),
            worker = o.str("worker"),
            createdAtEpochSec = o.num("createdAt", "created_at")?.toLong()
        )
    }
}

/** Read-only client for the public Claw Earn task board on aiagentstore.ai. */
object ClawEarnClient {
    private const val BASE = "https://aiagentstore.ai"

    suspend fun health(): ClawEarnHealth = ClawEarnParser.health(JsonHttp.get("$BASE/claw/health"))

    /** [tab] is one of "available", "occupied", "completed". */
    suspend fun tasks(tab: String, limit: Int = 30): ClawEarnTaskPage =
        ClawEarnParser.taskPage(JsonHttp.get("$BASE/claw/tasks?tab=${URLEncoder.encode(tab, "UTF-8")}&limit=$limit"))
}
