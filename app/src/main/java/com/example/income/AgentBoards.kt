package com.example.income

/**
 * A hand-checked directory of job boards where AI agents can take paid work. It's split by how
 * well the escrow (money locked before work starts) is confirmed.
 *
 * Nothing here was audited on-chain. "Confirmed" means the escrow is described in the platform's
 * own docs or open-source code, or reported by an independent outlet; everything else is
 * self-reported by listings, unclear, or has reported payout problems. Review again before
 * changing a board's tier.
 */
object AgentBoards {
    const val CHECKED = "October 2026"

    enum class Tier { ESCROW_CONFIRMED, NOT_VERIFIED }

    data class Board(
        val name: String,
        /** The board itself, or null when no official address could be confirmed. */
        val url: String?,
        val chain: String,
        val summary: String,
        val escrow: String,
        val caveat: String,
        val tier: Tier,
        /** Where the escrow claim comes from. */
        val sourceUrl: String,
        /** True for Claw Earn, which Kora can show live inside the app. */
        val liveInApp: Boolean = false
    )

    val ALL = listOf(
        Board(
            name = "TaskMarket (Daydreams)",
            url = null,
            chain = "Base · USDC",
            summary = "Requesters post tasks as bounties, claims, pitches, benchmarks or auctions; agents and humans submit work.",
            escrow = "Payment is locked in USDC escrow and released on-chain only when a submission is accepted.",
            caveat = "Independent coverage reported 373 tasks and 16,640 submissions by August 2026; no official address confirmed here, so open it from the article.",
            tier = Tier.ESCROW_CONFIRMED,
            sourceUrl = "https://cryptobriefing.com/daydreams-taskmarket-agent-economy-outsourcing/"
        ),
        Board(
            name = "Claw Earn",
            url = "https://aiagentstore.ai/claw-earn",
            chain = "Base · USDC",
            summary = "Buyers post tasks, one agent stakes to start, delivers and is paid on approval. Live inside Kora.",
            escrow = "Official docs: funds are locked in non-custodial escrow contracts with no admin pause or emergency withdrawal.",
            caveat = "Beta. No independent audit found, and one researcher found little evidence of completed payouts. Fees differ between its own pages.",
            tier = Tier.ESCROW_CONFIRMED,
            sourceUrl = "https://aiagentstore.ai/claw-earn/docs",
            liveInApp = true
        ),
        Board(
            name = "BasedAgents",
            url = "https://github.com/maxfain/basedagents",
            chain = "Base · USDC",
            summary = "Open-source identity, reputation and task marketplace: verified agents claim tasks and get signed receipts.",
            escrow = "Open code: the bounty goes into an escrow wallet when posted and is released on acceptance.",
            caveat = "Posters can switch escrow off per task, so only take tasks that show an escrowed bounty.",
            tier = Tier.ESCROW_CONFIRMED,
            sourceUrl = "https://github.com/maxfain/basedagents"
        ),
        Board(
            name = "BountyBook",
            url = "https://bountybook.ai",
            chain = "Base · USDC",
            summary = "Agent-first bounties: claim a task, submit output, an AI oracle checks it against the spec.",
            escrow = "Claims USDC escrow on Base with oracle-checked release.",
            caveat = "An unverified report describes payout and verifier problems; its API status is unclear.",
            tier = Tier.NOT_VERIFIED,
            sourceUrl = "https://www.producthunt.com/products/bountybook/makers"
        ),
        Board(
            name = "ClawTasks",
            url = "https://clawtasks.com",
            chain = "Base · USDC",
            summary = "Agent-to-agent bounty board, closest in style to Claw Earn.",
            escrow = "Described as staked bounties; escrow details not documented in what was found.",
            caveat = "Beta and currently free tasks only; a tester saw paid bounties paused and heavy rate limits.",
            tier = Tier.NOT_VERIFIED,
            sourceUrl = "https://aiindigo.com/tutorials/getting-started-with-clawtasks-automate-tasks-and-earn-usdc-on-base-l2"
        ),
        Board(
            name = "0xWork",
            url = "https://0xwork.org",
            chain = "Base · USDC",
            summary = "Task marketplace with a CLI; open tasks can be viewed without a wallet.",
            escrow = "Third-party skill listings say payments are escrowed on-chain; no official docs found.",
            caveat = "Very low usage, and one listing mentions staking its own token.",
            tier = Tier.NOT_VERIFIED,
            sourceUrl = "https://www.skills.sh/bankrbot/skills/0xwork"
        ),
        Board(
            name = "Superteam Earn",
            url = "https://earn.superteam.fun",
            chain = "Solana · USDC",
            summary = "Long-running board of sponsor-funded crypto bounties and grants, about \$200 to \$10,000+.",
            escrow = "Sponsors fund the rewards; escrow is mentioned only by a third-party profile.",
            caveat = "Human-reviewed with long applications; winners can be asked for KYC. Better for people than agents.",
            tier = Tier.NOT_VERIFIED,
            sourceUrl = "https://superteamdao.notion.site/Superteam-Earn-FAQ-aedaa039b25741b1861167d68aa880b1"
        ),
        Board(
            name = "toku.agency",
            url = "https://toku.agency",
            chain = "Crypto",
            summary = "Open marketplace where agents bid on jobs, from about \$3 research tasks to \$50+ deep dives.",
            escrow = "Not confirmed: sources describe payment after approval but don't say it's escrowed.",
            caveat = "Figures come from a single blog guide.",
            tier = Tier.NOT_VERIFIED,
            sourceUrl = "https://dev.to/neilvolner/the-complete-guide-to-ai-agent-marketplaces-in-2026-where-agents-get-hired-and-paid-8ja"
        ),
        Board(
            name = "dealwork.ai",
            url = "https://dealwork.ai",
            chain = "Unclear",
            summary = "Job marketplace where agents bid on work.",
            escrow = "Reviews mention escrow, but no documentation of how it works was found.",
            caveat = "Reported fees conflict (10% vs 15%).",
            tier = Tier.NOT_VERIFIED,
            sourceUrl = "https://dealwork.ai/blog/the-state-of-ai-agent-marketplaces-in-2026-a-comprehensive-comparison"
        ),
        Board(
            name = "TheJobCafe",
            url = "https://thejobcafe.com",
            chain = "Unclear",
            summary = "Bounties agents can claim with no account; some are tagged \"funded\".",
            escrow = "Says funded payouts are deposited with the platform; how they're held isn't documented.",
            caveat = "Only take bounties tagged funded.",
            tier = Tier.NOT_VERIFIED,
            sourceUrl = "https://thejobcafe.com/"
        ),
        Board(
            name = "ClawGig",
            url = "https://clawgig.ai",
            chain = "Solana · USDC",
            summary = "Freelance gigs posted by humans that agents pick up by API or webhook.",
            escrow = "The founder says USDC is held in escrow until the client approves.",
            caveat = "New and self-reported only.",
            tier = Tier.NOT_VERIFIED,
            sourceUrl = "https://www.indiehackers.com/post/i-built-a-freelance-marketplace-where-ai-agents-are-the-freelancers-46785ceb8f"
        ),
        Board(
            name = "Agent Bounties",
            url = null,
            chain = "Base · USDC",
            summary = "Open-source protocol for agents to post, fund, claim and solve digital work.",
            escrow = "Listing mentions on-chain settlement but gives no escrow contract details.",
            caveat = "Several unrelated projects share this name; no official site confirmed.",
            tier = Tier.NOT_VERIFIED,
            sourceUrl = "https://mcpmarket.com/server/agent-bounties-1"
        ),
        Board(
            name = "MuleRun",
            url = null,
            chain = "Card / platform payouts",
            summary = "Not a task board: you publish an agent and earn when people run it.",
            escrow = "No escrow; earnings depend on its creator revenue share.",
            caveat = "Revenue-share figures in blogs aren't in its official material.",
            tier = Tier.NOT_VERIFIED,
            sourceUrl = "https://www.globenewswire.com/news-release/2025/12/24/3210283/0/en/MuleRun-Launches-Creator-Studio-the-World-s-First-Platform-Built-for-AI-Agent-Monetization.html"
        )
    )

    val escrowConfirmed: List<Board> get() = ALL.filter { it.tier == Tier.ESCROW_CONFIRMED }
    val notVerified: List<Board> get() = ALL.filter { it.tier == Tier.NOT_VERIFIED }

    /** Shown above both lists. */
    const val WARNING = "These boards are new. One operator ran an agent on seven of them and couldn't get clean payouts to his own wallet. " +
        "Before you stake or connect a wallet: check that money is locked before work starts, look for real completed payouts, and read the fees and dispute rules. " +
        "Never share a seed phrase or private key with any site or app, Kora included."

    /** Source for [WARNING]. */
    const val WARNING_SOURCE = "https://podcast.trends.vc/episodes/how-ai-became-worth-less-than-the-workers-it-replaced/transcript"
}
