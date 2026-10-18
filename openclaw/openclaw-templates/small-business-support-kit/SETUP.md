# Small Business Customer Service Kit: setup guide

Front Desk is an OpenClaw agent that answers your customers on WhatsApp or Telegram. It answers questions from your own FAQ and policies, looks up order status, calms down unhappy customers and hands them to you with a clear summary, and drafts replies to your online reviews. Every weekday evening it sends you a digest of handoffs and questions it couldn't answer, so your FAQ gets better every week.

## What's included
- **Agent**: Front Desk (`front-desk`), with its persona, operating rules and a first-run business interview.
- **Skills**: FAQ answerer, order status checker (with customer verification), complaint handler (with severity levels and clean handoffs), review response writer.
- **Business files**: business facts, FAQ, policies and escalation rules, filled in during the interview.
- **Order data**: a CSV template for your order export.
- **Schedule**: a weekday 18:00 UTC support digest (you can change the time).
- **Model**: Claude Haiku 5.5 (fast and low-cost for high message volume), with Claude Sonnet 5.5 as fallback.
- **Channel configs**: WhatsApp (default) and Telegram, both with per-customer privacy and customer restrictions already set.

## Requirements
- OpenClaw 2026.10 or newer, running on a computer or server that stays on (the Gateway must be running for customers to get replies).
- An API key for the model provider.
- A WhatsApp number for the business (a separate number from your personal one is strongly recommended), or a Telegram bot.

## Install (about 20 minutes)
1. **Unzip** the download. You'll have a `package/` folder (the agent) plus this guide and `config/`.
2. **Make sure the Gateway is running** (`openclaw gateway status`; start it with `openclaw gateway start`), so the schedule is created during install.
3. **Turn on Claws**:
   ```bash
   export OPENCLAW_EXPERIMENTAL_CLAWS=1
   ```
4. **Preview** the install. Nothing changes yet:
   ```bash
   openclaw claws add ./package --dry-run
   ```
5. **Install** by approving that exact plan:
   ```bash
   openclaw claws add ./package --yes --plan-integrity <value from step 4>
   ```
6. **Run the business interview before connecting customers.** Open the Control UI (`openclaw dashboard`), choose **Front Desk** and say hi. Have your FAQ page, returns and shipping policy, opening hours and the number you want handoffs sent to. Then test it: ask it questions as if you were a customer.

## Connect your customer channel
**WhatsApp (default)**
1. Edit `config/openclaw.patch.json5`: replace `+15551234567` with your own (owner) WhatsApp number. This keeps your access to updating the agent; every other sender is treated as a customer.
2. Preview, then apply:
   ```bash
   openclaw config patch --file ./config/openclaw.patch.json5 --dry-run
   openclaw config patch --file ./config/openclaw.patch.json5
   ```
   If you already have `bindings` in your config, add the binding line from the patch to your existing list by hand, because the patch replaces the list.
3. Link the business number: `openclaw channels login --channel whatsapp` and scan the QR code from the business phone.
4. Send a test message from a different phone.

**Telegram (alternative)**: create a bot with @BotFather, `export TELEGRAM_BOT_TOKEN=<token>`, put your numeric Telegram id in `config/openclaw.telegram.patch.json5`, and apply it the same way.

### What the channel config does
- `session.dmScope: "per-channel-peer"` gives every customer a separate, private conversation. Don't remove it; without it customers could share context.
- `dmPolicy: "open"` lets anyone message the business line.
- Customers can't edit the agent's files or schedules (`toolsBySender`), and group chats are off.
- Shell commands, the browser and web access are disabled for this agent in every session.

## Keep order status current
Export orders from your shop (Shopify, WooCommerce, Square or Etsy) as CSV, map the columns to the headers in `data/orders.csv` in the agent's workspace, and replace the file daily. Customers must give the order number plus the email or postcode on the order before anything is shared. A live store integration can replace the CSV; that's part of the setup service.

## Change the digest time or model
```bash
openclaw cron list
openclaw cron edit <job-id> --cron "0 18 * * 1-5" --tz "America/Chicago"
openclaw config set agents.entries.front-desk.model.primary "<provider/model>"
```

## Try it (as a customer)
- "What time do you close on Saturday?"
- "Where's my order #1042?"
- "My package arrived damaged and I'm really annoyed."
- "Can I talk to a person?"

As the owner: "Draft a reply to this 2-star Google review: <paste>", "Add to the FAQ: we now deliver on Sundays."

## Important limits
- Front Desk only knows what's in its business files. Keep them accurate; it will say "I'll check with the team" rather than guess.
- AI can still make mistakes. Read the daily digest, spot-check conversations in the Control UI during the first week, and tighten the FAQ.
- Check that automated replies are allowed for your WhatsApp account type and region, and tell customers they're chatting with an assistant if your local rules require it.

## Update or remove
- Update: `openclaw claws update front-desk --from ./package --dry-run`, then apply with `--yes --plan-integrity <value>`. Your edited business files are kept.
- Remove: `openclaw claws remove front-desk --dry-run`, then apply. Then remove the WhatsApp binding from your config.

## Troubleshooting
- **"unknown command claws"**: set `OPENCLAW_EXPERIMENTAL_CLAWS=1` and check `openclaw --version`.
- **Install says "partial" and mentions the Gateway**: the agent and its files were created, but the schedule needs the Gateway. Start it (`openclaw gateway start`), then run `openclaw claws update front-desk --dry-run` and apply that plan with `--yes --plan-integrity <value>` to create the schedule.
- **Customers get no reply**: check `openclaw gateway status`, `openclaw channels status`, and that the binding points to `front-desk`.
- **Owner gets treated as a customer**: the number in `toolsBySender` must match exactly, in +country-code format.

Need help? The setup service covers the WhatsApp connection, your FAQ and policies, order data, and a week of tuning.
