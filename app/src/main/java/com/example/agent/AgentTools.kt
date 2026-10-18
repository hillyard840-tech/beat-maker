package com.example.agent

import com.example.api.FunctionDeclaration
import com.example.api.Schema
import com.example.api.Tool
import com.example.util.AgentTime
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import java.util.TimeZone

/**
 * The tools Kora can call. Web tools read the internet; phone tools act on the phone. Every
 * phone tool that changes something or leaves the app only prepares a card — it runs when the
 * user taps Confirm.
 */
object AgentTools {
    const val SEARCH_WEB = "search_live_web"
    const val FETCH_WEBSITE = "fetch_website"
    const val SEARCH_JOBS = "search_jobs"
    const val FIND_BUILD_DEMAND = "find_build_demand"
    const val SET_REMINDER = "set_reminder"
    const val SET_ALARM = "set_alarm"
    const val SET_TIMER = "set_timer"
    const val ADD_CALENDAR_EVENT = "add_calendar_event"
    const val READ_CALENDAR = "read_calendar"
    const val DRAFT_TEXT = "draft_text_message"
    const val DRAFT_EMAIL = "draft_email"
    const val OPEN_MAP = "open_map"
    const val DIAL_NUMBER = "dial_number"
    const val OPEN_APP = "open_app"
    const val SCHEDULE_TASK = "schedule_task"
    const val LIST_REMINDERS = "list_reminders_and_tasks"
    const val LOG_EARNING = "log_earning"
    const val PM_SEARCH = "polymarket_search"
    const val PM_POSITIONS = "polymarket_positions"
    const val PM_TICKET = "polymarket_trade_ticket"
    const val READ_EARNINGS = "read_earnings"

    /** Not a model tool: the card that asks for calendar access when read_calendar can't run. */
    const val GRANT_CALENDAR = "grant_calendar_access"

    /** Extra argument added after the app was found on the phone. */
    const val ARG_PACKAGE = "_package"

    private fun str(description: String) = Schema(type = "STRING", description = description)
    private fun int(description: String) = Schema(type = "INTEGER", description = description)
    private fun obj(props: Map<String, Schema>, required: List<String> = emptyList()) =
        Schema(type = "OBJECT", properties = props, required = required.ifEmpty { null })

    private const val WHEN = "Local date and time as YYYY-MM-DDTHH:MM, e.g. 2026-10-18T07:30. Work it out from the current date and time given in your instructions."

    val webDeclarations = listOf(
        FunctionDeclaration(
            name = SEARCH_WEB,
            description = "Searches the live internet in real time and returns current web search results, news, live facts, and website URLs.",
            parameters = obj(mapOf("query" to str("The specific search query to execute on the live internet")), listOf("query"))
        ),
        FunctionDeclaration(
            name = FETCH_WEBSITE,
            description = "Connects directly to any live URL or website on the internet to fetch and read its current text content.",
            parameters = obj(mapOf("url" to str("The complete URL of the webpage to browse and read")), listOf("url"))
        ),
        FunctionDeclaration(
            name = SEARCH_JOBS,
            description = "Searches live remote job and gig listings on public job boards (Remotive, Jobicy, Arbeitnow). Returns real listings with links. Use it whenever the user wants work, gigs or freelance clients.",
            parameters = obj(
                mapOf(
                    "query" to str("Skill or role to search for, e.g. 'kotlin', 'copywriter', 'data entry'"),
                    "remote_only" to Schema(type = "BOOLEAN", description = "Only remote jobs. Default true.")
                ),
                listOf("query")
            )
        ),
        FunctionDeclaration(
            name = FIND_BUILD_DEMAND,
            description = "Finds real recent Hacker News posts where people describe a problem or ask for a tool in an area. Use it to back up ideas for programs or products worth building.",
            parameters = obj(mapOf("topic" to str("Area to look in, e.g. 'invoicing for freelancers'")), listOf("topic"))
        )
    )

    val readCalendarDeclaration = FunctionDeclaration(
        name = READ_CALENDAR,
        description = "Reads the user's upcoming calendar events from the phone. Use it for questions about their schedule.",
        parameters = obj(mapOf("days" to int("How many days ahead to read, 1 to 14. Default 1 (today).")))
    )

    /** Polymarket reading tools: they call Polymarket's public APIs, so they follow the Live web switch. */
    val marketReadDeclarations = listOf(
        FunctionDeclaration(
            name = PM_SEARCH,
            description = "Searches live Polymarket prediction markets (or the top markets by 24h volume when the query is empty). Returns questions, outcome probabilities, volume, end dates, slugs and links.",
            parameters = obj(mapOf("query" to str("Topic to search, e.g. 'fed rate cut' or 'world cup'. Empty for trending.")))
        ),
        FunctionDeclaration(
            name = PM_POSITIONS,
            description = "Reads the user's open Polymarket positions (from the public wallet address they saved in Kora) with value and profit/loss, plus Kora's prediction track record."
        )
    )

    val marketTicketDeclaration = FunctionDeclaration(
        name = PM_TICKET,
        description = "Prepares a Polymarket trade ticket the user reviews. Kora never places trades: on Confirm it opens the market in Polymarket where the user places the order themselves.",
        parameters = obj(
            mapOf(
                "question" to str("The market question"),
                "url" to str("The market's polymarket.com link, copied exactly from polymarket_search"),
                "outcome" to str("Outcome to buy, e.g. Yes or No"),
                "price" to Schema(type = "NUMBER", description = "Current price of that outcome, 0.01-0.99"),
                "amount" to Schema(type = "NUMBER", description = "Dollars to spend, as the user said"),
                "reason" to str("One line on why, including that it's a risk")
            ),
            listOf("question", "url", "outcome", "price", "amount")
        )
    )

    val readEarningsDeclaration = FunctionDeclaration(
        name = READ_EARNINGS,
        description = "Reads the user's earnings tracker: this month vs their goal, totals and real hourly rate per income source, and recent entries."
    )

    val phoneDeclarations = listOf(
        FunctionDeclaration(
            name = SET_REMINDER,
            description = "Creates a reminder that Kora delivers as a notification at the given time. The user confirms first.",
            parameters = obj(
                mapOf("title" to str("What to remind the user about, short"), "when" to str(WHEN), "note" to str("Optional extra detail")),
                listOf("title", "when")
            )
        ),
        FunctionDeclaration(
            name = SET_ALARM,
            description = "Sets an alarm in the phone's clock app. The user confirms first.",
            parameters = obj(
                mapOf("hour" to int("Hour, 0-23"), "minute" to int("Minute, 0-59"), "label" to str("Optional alarm label")),
                listOf("hour", "minute")
            )
        ),
        FunctionDeclaration(
            name = SET_TIMER,
            description = "Starts a countdown timer in the phone's clock app. The user confirms first.",
            parameters = obj(mapOf("seconds" to int("Length in seconds, 1-86400"), "label" to str("Optional timer label")), listOf("seconds"))
        ),
        FunctionDeclaration(
            name = ADD_CALENDAR_EVENT,
            description = "Opens the calendar app with a new event filled in, ready to save. The user confirms first.",
            parameters = obj(
                mapOf(
                    "title" to str("Event title"),
                    "start" to str(WHEN),
                    "end" to str("Optional end, same format. Defaults to one hour after start."),
                    "location" to str("Optional location"),
                    "description" to str("Optional notes")
                ),
                listOf("title", "start")
            )
        ),
        readCalendarDeclaration,
        FunctionDeclaration(
            name = DRAFT_TEXT,
            description = "Opens the messaging app with a text message drafted. The user reviews and sends it themselves.",
            parameters = obj(mapOf("to" to str("Phone number, if known"), "body" to str("The message text")), listOf("body"))
        ),
        FunctionDeclaration(
            name = DRAFT_EMAIL,
            description = "Opens the email app with an email drafted. The user reviews and sends it themselves.",
            parameters = obj(
                mapOf("to" to str("Email address, if known"), "subject" to str("Subject line"), "body" to str("Email body")),
                listOf("subject", "body")
            )
        ),
        FunctionDeclaration(
            name = OPEN_MAP,
            description = "Opens the maps app searching for a place or address.",
            parameters = obj(mapOf("query" to str("Place, address or search such as 'coffee near me'")), listOf("query"))
        ),
        FunctionDeclaration(
            name = DIAL_NUMBER,
            description = "Opens the phone dialer with a number filled in. The user places the call themselves.",
            parameters = obj(mapOf("number" to str("The phone number")), listOf("number"))
        ),
        FunctionDeclaration(
            name = OPEN_APP,
            description = "Opens an app installed on the phone by its name.",
            parameters = obj(mapOf("name" to str("The app's name, e.g. Spotify")), listOf("name"))
        ),
        FunctionDeclaration(
            name = SCHEDULE_TASK,
            description = "Schedules Kora to run a prompt by itself at a set time and notify the user with the answer, e.g. a morning news briefing. The user confirms first.",
            parameters = obj(
                mapOf(
                    "title" to str("Short name for the task"),
                    "prompt" to str("What Kora should do each time, written as an instruction"),
                    "hour" to int("Hour, 0-23"),
                    "minute" to int("Minute, 0-59"),
                    "repeat" to str("daily, weekdays, weekly (Sundays) or once")
                ),
                listOf("title", "prompt", "hour", "minute")
            )
        ),
        FunctionDeclaration(
            name = LIST_REMINDERS,
            description = "Lists the user's upcoming reminders and scheduled tasks in Kora."
        ),
        FunctionDeclaration(
            name = LOG_EARNING,
            description = "Logs money the user says they earned into their earnings tracker. The user confirms first.",
            parameters = obj(
                mapOf(
                    "source" to str("Where it came from, e.g. 'Fiverr logo gigs'"),
                    "amount" to Schema(type = "NUMBER", description = "Amount earned in the tracker's currency, e.g. 45.5"),
                    "hours" to Schema(type = "NUMBER", description = "Hours spent, if the user said"),
                    "date" to str("YYYY-MM-DD if it wasn't today"),
                    "note" to str("Optional note")
                ),
                listOf("source", "amount")
            )
        ),
        readEarningsDeclaration
    )

    /** Tools for a chat turn. Web tools only when the user's Live web switch is on. */
    fun forChat(webEnabled: Boolean): List<Tool> =
        listOf(
            Tool(
                functionDeclarations = (if (webEnabled) webDeclarations + marketReadDeclarations + marketTicketDeclaration else emptyList()) +
                    phoneDeclarations
            )
        )

    /** Tools for a background task: reading only, never acting. */
    fun forTask(webEnabled: Boolean, calendarReadable: Boolean): List<Tool>? {
        val list = (if (webEnabled) webDeclarations + marketReadDeclarations else emptyList()) + (if (calendarReadable) listOf(readCalendarDeclaration) else emptyList()) +
            readEarningsDeclaration
        return if (list.isEmpty()) null else listOf(Tool(functionDeclarations = list))
    }

    /** Tools that only show a card; the work happens on Confirm. */
    val CONFIRM_TOOLS = setOf(
        SET_REMINDER, SET_ALARM, SET_TIMER, ADD_CALENDAR_EVENT, DRAFT_TEXT, DRAFT_EMAIL,
        OPEN_MAP, DIAL_NUMBER, OPEN_APP, SCHEDULE_TASK, LOG_EARNING, PM_TICKET
    )

    // ------------------------------------------------------------------ arguments

    fun string(args: Map<String, JsonElement>?, key: String): String? =
        (args?.get(key) as? JsonPrimitive)?.contentOrNull?.trim()?.takeIf { it.isNotEmpty() }

    /** Models send numbers as 7, 7.0 or "7"; all are accepted. */
    fun int(args: Map<String, JsonElement>?, key: String): Int? =
        string(args, key)?.toDoubleOrNull()?.takeIf { !it.isNaN() && it >= Int.MIN_VALUE && it <= Int.MAX_VALUE }?.toInt()

    fun argsOf(json: String): Map<String, JsonElement> = try {
        (kotlinx.serialization.json.Json.parseToJsonElement(json) as? JsonObject).orEmpty()
    } catch (_: Exception) {
        emptyMap()
    }

    fun argsJson(args: Map<String, JsonElement>?): String = JsonObject(args.orEmpty()).toString()

    // ------------------------------------------------------------------ cards

    sealed class Prepared {
        /** Show a confirmation card with this text. */
        data class Card(val title: String, val details: String) : Prepared()

        /** The arguments can't be used; tell the model why so it can fix or ask. */
        data class Invalid(val error: String) : Prepared()
    }

    /**
     * Checks a confirm-type tool call and words its card. Pure: the phone is only touched when
     * the user confirms.
     */
    fun prepare(
        tool: String,
        args: Map<String, JsonElement>?,
        now: Long = System.currentTimeMillis(),
        zone: TimeZone = TimeZone.getDefault()
    ): Prepared = when (tool) {
        SET_REMINDER -> {
            val title = string(args, "title")
            val at = AgentTime.parseLocal(string(args, "when"), zone)
            when {
                title == null -> Prepared.Invalid("A reminder needs a title.")
                at == null -> Prepared.Invalid("'when' must be a date and time like 2026-10-18T07:30.")
                at <= now -> Prepared.Invalid("That time has already passed. Ask the user for a time in the future.")
                else -> Prepared.Card("Remind me: $title", listOfNotNull(AgentTime.label(at), string(args, "note")).joinToString("\n"))
            }
        }
        SET_ALARM -> {
            val hour = int(args, "hour")
            val minute = int(args, "minute") ?: 0
            if (hour == null || hour !in 0..23 || minute !in 0..59) {
                Prepared.Invalid("An alarm needs hour 0-23 and minute 0-59.")
            } else {
                Prepared.Card("Set alarm for ${AgentTime.clock(hour, minute)}", string(args, "label").orEmpty())
            }
        }
        SET_TIMER -> {
            val seconds = int(args, "seconds")
            if (seconds == null || seconds !in 1..86_400) {
                Prepared.Invalid("A timer needs 1 to 86400 seconds.")
            } else {
                Prepared.Card("Start a ${AgentTime.duration(seconds)} timer", string(args, "label").orEmpty())
            }
        }
        ADD_CALENDAR_EVENT -> {
            val title = string(args, "title")
            val start = AgentTime.parseLocal(string(args, "start"), zone)
            val end = AgentTime.parseLocal(string(args, "end"), zone)
            when {
                title == null -> Prepared.Invalid("An event needs a title.")
                start == null -> Prepared.Invalid("'start' must be a date and time like 2026-10-18T07:30.")
                end != null && end <= start -> Prepared.Invalid("'end' must be after 'start'.")
                else -> Prepared.Card(
                    "Add to calendar: $title",
                    listOfNotNull(AgentTime.label(start), string(args, "location")).joinToString("\n")
                )
            }
        }
        DRAFT_TEXT -> {
            val body = string(args, "body")
            if (body == null) Prepared.Invalid("A text message needs a body.")
            else Prepared.Card("Draft a text" + (string(args, "to")?.let { " to $it" } ?: ""), body)
        }
        DRAFT_EMAIL -> {
            val subject = string(args, "subject")
            val body = string(args, "body")
            if (subject == null && body == null) Prepared.Invalid("An email needs a subject or body.")
            else Prepared.Card(
                "Draft an email" + (string(args, "to")?.let { " to $it" } ?: ""),
                listOfNotNull(subject, body).joinToString("\n\n")
            )
        }
        OPEN_MAP -> string(args, "query")?.let { Prepared.Card("Open in Maps", it) }
            ?: Prepared.Invalid("Say what to look up on the map.")
        DIAL_NUMBER -> {
            val number = string(args, "number")?.filter { it.isDigit() || it in "+*#" }
            if (number.isNullOrEmpty()) Prepared.Invalid("That doesn't look like a phone number.")
            else Prepared.Card("Call $number", "Opens the dialer; you place the call.")
        }
        OPEN_APP -> {
            val name = string(args, "name")
            when {
                name == null -> Prepared.Invalid("Say which app to open.")
                string(args, ARG_PACKAGE) == null -> Prepared.Invalid("No app called \"$name\" is installed.")
                else -> Prepared.Card("Open $name", "")
            }
        }
        SCHEDULE_TASK -> {
            val title = string(args, "title")
            val prompt = string(args, "prompt")
            val hour = int(args, "hour")
            val minute = int(args, "minute") ?: 0
            val repeat = normalizeRepeat(string(args, "repeat"))
            when {
                title == null || prompt == null -> Prepared.Invalid("A task needs a title and a prompt.")
                hour == null || hour !in 0..23 || minute !in 0..59 -> Prepared.Invalid("A task needs hour 0-23 and minute 0-59.")
                else -> Prepared.Card(
                    "Schedule: $title",
                    "${AgentTime.repeatLabel(repeat)} at ${AgentTime.clock(hour, minute)}\n$prompt"
                )
            }
        }
        LOG_EARNING -> {
            val source = string(args, "source")
            val cents = string(args, "amount")?.let { com.example.income.EarningsMath.parseAmount(it) }
            val hours = string(args, "hours")?.toDoubleOrNull()
            val date = string(args, "date")
            val dateMillis = date?.let { AgentTime.parseLocal(it, zone) }
            when {
                source == null -> Prepared.Invalid("Say where the money came from.")
                cents == null -> Prepared.Invalid("'amount' must be a positive number like 45.50.")
                hours != null && (hours <= 0 || hours > 1000) -> Prepared.Invalid("'hours' must be between 0 and 1000.")
                date != null && dateMillis == null -> Prepared.Invalid("'date' must look like 2026-10-17.")
                dateMillis != null && dateMillis > now + 24 * 60 * 60 * 1000L -> Prepared.Invalid("That date is in the future.")
                else -> Prepared.Card(
                    "Log ${"%.2f".format(java.util.Locale.US, cents / 100.0)} from $source",
                    listOfNotNull(hours?.let { "${"%.1f".format(java.util.Locale.US, it)} h" }, dateMillis?.let { AgentTime.label(it).substringBeforeLast(',') }, string(args, "note")).joinToString(" · ")
                )
            }
        }
        PM_TICKET -> {
            val url = string(args, "url")
            val outcome = string(args, "outcome")
            val price = string(args, "price")?.toDoubleOrNull()
            val amount = string(args, "amount")?.toDoubleOrNull()
            val ticket = if (outcome != null && price != null && amount != null) com.example.markets.MarketMath.ticket(outcome, price, amount) else null
            when {
                url == null || !com.example.markets.Polymarket.isPolymarketUrl(url) -> Prepared.Invalid("Use the market's polymarket.com link from polymarket_search.")
                outcome == null || price == null || amount == null -> Prepared.Invalid("A ticket needs the outcome, its price and the amount.")
                price !in 0.01..0.99 -> Prepared.Invalid("'price' must be between 0.01 and 0.99.")
                amount !in 1.0..10_000.0 -> Prepared.Invalid("'amount' must be between 1 and 10000 dollars.")
                ticket == null -> Prepared.Invalid("Those numbers don't make a valid ticket.")
                else -> Prepared.Card(
                    "Trade ticket: $outcome on \"${string(args, "question") ?: "this market"}\"",
                    com.example.markets.MarketMath.describeTicket(ticket) + (string(args, "reason")?.let { "\nWhy: $it" } ?: "") +
                        "\nConfirm opens Polymarket; you place the order there. Kora doesn't trade for you."
                )
            }
        }
        else -> Prepared.Invalid("Unknown tool $tool.")
    }

    fun normalizeRepeat(raw: String?): String = when (raw?.lowercase()?.trim()) {
        "weekdays", "weekday", "workdays" -> "weekdays"
        "once", "one time", "one-time", "none" -> "once"
        "weekly", "sundays", "sunday", "every sunday" -> "sundays"
        else -> "daily"
    }

    /** What the model is told after it proposes an action. */
    fun awaitingConfirmation(title: String): String =
        "A confirmation card \"$title\" is now shown to the user. It has NOT happened yet; it runs only if the user taps Confirm. " +
            "In one short sentence, tell the user to confirm it. Don't claim it is done."
}
