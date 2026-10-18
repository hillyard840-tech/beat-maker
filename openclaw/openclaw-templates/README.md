# OpenClaw workspace templates

Three pre-configured OpenClaw agents, packaged in OpenClaw's own agent-package format ("Claws") and ready to sell, each with a buyer setup guide, channel configs and a setup-service offer.

| Template | Agent | Who it's for | What it does |
|---|---|---|---|
| `content-creator-suite/` | Content Studio | Creators, coaches, small brands | Brand interview, blog/video outlines, captions for 9 platforms, keyword research, a content plan every Monday |
| `small-business-support-kit/` | Front Desk | Shops, cafés, salons, e-commerce stores | Answers customers on WhatsApp or Telegram from the owner's FAQ and policies, checks orders, handles complaints with handoffs, drafts review replies, sends a daily digest |
| `freelancer-client-manager/` | Client Desk | Freelancers and small studios | Proposals from a rate card, 5-stage invoice reminders, project updates, tricky client emails, contract review, call notes to actions, a Monday money check |

**Ready-to-sell files are in `dist/`:** one `.zip` per template (agent package, setup guide, configs), plus the built `.tgz` package artifact.

## What "fully configured" means here

Each template is a Claw package: OpenClaw's format for shipping one complete agent. A buyer previews it and installs it with two commands, and OpenClaw creates the agent with everything below:

| Piece | Where it lives |
|---|---|
| Model selection (primary and fallback) | `profiles/openclaw.yml` |
| Tool permissions (what the agent may and may not do) | `profiles/openclaw.yml` |
| Memory settings | `profiles/openclaw.yml` |
| System prompt / persona | body of `CLAW.md` (becomes `SOUL.md`) |
| Operating rules and skill routing | `workspace/AGENTS.md` |
| Skills (4 to 6 each) | `workspace/skills/` |
| Business/brand data files to fill in | `workspace/business/`, `workspace/brand/`, `workspace/tracking/` |
| First-run onboarding interview | `BOOTSTRAP.md` |
| Scheduled routine (weekly plan, daily digest, Monday check) | `cronJobs` in `CLAW.md` |
| Platform configuration (WhatsApp / Telegram, routing, privacy) | `config/*.patch.json5` (applied with `openclaw config patch`) |

Channels live in the buyer's main OpenClaw config, not inside the agent package, so they ship as patch files.

## How it was checked

Using OpenClaw 2026.10.4 (the current release), installed in my environment:
- `openclaw claws validate`: all three pass.
- `openclaw claws dev` (full offline install preview): all three show 0 blocked actions.
- Real install with `openclaw claws add`: all three created their agent, workspace, skills and files correctly. The scheduled job needs a running Gateway, which my environment doesn't have, so that one step reported "partial", as expected. The setup guides tell buyers to start the Gateway first, and how to finish the step if they forget.
- `openclaw config patch --dry-run`: all four channel config files pass OpenClaw's config validation.
- `openclaw claws build`: produced the `.tgz` artifacts in `dist/`.
- Every CLI command in the setup guides exists in that version.

Not tested: live conversations with a model, and a real WhatsApp or Telegram connection (there's no model key or phone in my environment). Run each template once yourself before selling (see "Your next steps").

## Research: what to sell and why these three

**Demand signals (ClawHub top-1,000 downloads, 2026-10-18 snapshot):**
- Content, social and SEO skills: 73 skills with about 980k downloads combined, the largest business category by far.
- Customer messaging (WhatsApp Business, Telegram, Shopify, WooCommerce, order tools): 22 skills with about 290k downloads.
- Freelancer back office (invoicing, Stripe, Xero, proposals, PDF): 10 skills with about 130k downloads.
- Real estate: almost nothing (5 loosely related skills, about 41k downloads, none real-estate specific).

**Pricing signals** (from seller blogs, unverified and mostly self-reported, so treat as rough):
- Template packs: $29 to $99. Content and SEO packs sit at the top of single-agent prices ($15 to $29 for one agent).
- Setup-as-a-service: $500 to $2,000 per custom build, or $50 to $200 per month managed.

**Choice:**
1. **Content Creator Suite.** The biggest demand, and buyers are often creators who already run OpenClaw themselves.
2. **Small Business Customer Service Kit.** OpenClaw's core strength is chat channels, and small businesses will pay for setup because they can't do it themselves. This is the best fit for the setup service.
3. **Freelancer Client Manager.** Freelancers buy their own tools and are comfortable installing them. It also reuses two skills you already built (contract reviewer, meeting-to-actions), so they work as free samples that lead to the paid pack.

**Skipped: Real Estate Agent AI Pack.** ClawHub shows no demand signal, and the useful version needs MLS and CRM integrations that vary by region and brokerage. It's better sold later as a custom setup-service build than as an off-the-shelf template.

## Where to sell

- **Not ClawHub, for now.** ClawHub accepts Claw packages (`clawhub package publish <tgz> --family claw`), but its public Claw catalog currently lists only official `@openclaw/*` packages, and ClawHub has no payments. One seller blog claims ClawHub has built-in payments with a 15% fee, but ClawHub's own documentation says paid listings aren't supported, so I went with the docs.
- **Sell directly** on Gumroad, Lemon Squeezy or your own site: upload the `.zip` from `dist/`.
- **Use your free ClawHub skills as the funnel.** Add `homepage: <your store URL>` to the four skills in `clawhub-skills/` before you publish them, and mention the matching pack in each description.

## Suggested pricing

| Offer | Price | Includes |
|---|---|---|
| Template (DIY) | Content $39, Freelancer $49, Support Kit $59 | The zip, setup guide, updates for 12 months |
| All three bundle | $119 | All three templates |
| Done-for-you setup | $299 (Content or Freelancer), $499 (Support Kit) | Install on the client's machine or VPS, run the interview with them, connect one channel, one week of tuning |
| Managed support (Support Kit) | $79 per month | Weekly FAQ updates from the digest, order-data refresh setup, monitoring, model/version updates |

Start with the prices in the middle of the observed ranges, and raise them once you have testimonials.

## Listing copy (paste into your store)

**Content Creator Suite: your AI content strategist for OpenClaw**
Stop staring at a blank calendar. Content Studio learns your voice in a 10-minute interview, then plans your week, outlines posts and videos, writes platform-native captions for Instagram, LinkedIn, TikTok, X and more, and finds keyword opportunities without making up numbers. Every Monday your plan is waiting. You approve and post; it never publishes anything without you. Includes 4 skills, brand profile templates, a weekly schedule and optional Telegram access. Requires OpenClaw 2026.10+ and your own model API key.

**Small Business Customer Service Kit: a front desk that never sleeps**
Answer customers on WhatsApp or Telegram around the clock, using only your own FAQ and policies. It never guesses. Front Desk checks order status after verifying the customer, calms down complaints, hands tricky cases to you with a full summary, drafts replies to Google and Yelp reviews, and sends you a daily digest of what customers asked. Every customer gets a private conversation, and customers can't change its settings. Requires OpenClaw 2026.10+, an always-on computer or VPS, and a model API key. Setup service available.

**Freelancer Client Manager: get paid on time and stop writing the same emails**
Client Desk writes proposals from your rate card, chases unpaid invoices with the right tone at the right time, turns notes into client updates, handles scope creep and rate increases, reviews contracts before you sign, and turns call transcripts into action items. Every Monday you get a to-do list with drafts ready to send. Includes 6 skills, client/project/invoice trackers and optional Telegram access. Requires OpenClaw 2026.10+ and a model API key.

## Your next steps

1. **Test each template once** on your own OpenClaw (follow its `SETUP.md`). Run the interview, try the "Try it" prompts, and fix anything that feels off. For the Support Kit, test with a second phone.
2. **Write your license terms** for buyers (for example, a single-business licence with no resale). These templates aren't on ClawHub, so the MIT-0 rule doesn't apply and you choose the terms.
3. **Open a store** (Gumroad or Lemon Squeezy), upload the three zips from `dist/`, and paste the listing copy and prices.
4. **Make the setup service bookable**: a booking link and a short intake form (business type, channel, current FAQ or rates).
5. **Publish the free ClawHub skills** from the earlier batch, with `homepage` pointing to your store.
6. **When you change a template**, bump `version` in its `package.json`, then rebuild: `openclaw claws validate <folder>` and `openclaw claws build <folder> --out dist/<name>-<version>.tgz`. Make a new zip with `package/` (from the tgz), `SETUP.md` and `config/`. Buyers update with `openclaw claws update <agent-id> --from ./package`.

Nothing has been published, listed or sold, and no accounts were created.

## Sources
- OpenClaw Claws CLI reference (OpenClaw 2026.10.4 package, `docs/cli/claws.md`); also at https://docs.openclaw.ai/cli/claws
- ClawHub Claw packages: https://docs.openclaw.ai/clawhub/claws
- OpenClaw tool profiles, agent workspace, WhatsApp, Telegram, bindings and security docs (same package, `docs/`)
- ClawHub top-1,000 downloads snapshot: `../clawhub-skills/research/clawhub-top1000-2026-10-18.csv` (from https://github.com/LinklyAI/best-skills)
- Pricing claims: https://crewclaw.com/blog/sell-openclaw-agents-marketplace and https://superframeworks.com/articles/openclaw-make-money-guide (seller blogs, unverified)
