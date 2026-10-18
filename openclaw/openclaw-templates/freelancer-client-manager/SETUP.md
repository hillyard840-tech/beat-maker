# Freelancer Client Manager: setup guide

Client Desk is an OpenClaw agent that runs your client back office. It writes proposals from your rate card, chases unpaid invoices with the right tone at the right time, drafts project updates and tricky client emails, reviews contracts before you sign, and turns call notes into action items. Every Monday it checks your invoices and projects and hands you a to-do list with drafts ready to send. It drafts; you send.

## What's included
- **Agent**: Client Desk (`client-desk`), with its persona, operating rules and a first-run setup interview.
- **Skills**: proposal writer, invoice reminder (5-stage escalation), project update generator, client communication assistant (scope creep, rate rises, saying no, onboarding and more), contract redline reviewer, meeting-to-actions.
- **Tracking files**: clients, projects and invoices as CSV files you can also open in a spreadsheet.
- **Profile files**: your services, proof, process, voice, rate card and terms.
- **Schedule**: a Monday 09:00 UTC money-and-projects check (you can change the time).
- **Model**: Claude Sonnet 5.5, with GPT-5.5 as fallback. Swap in any model you have configured.
- **Optional phone access**: a ready config to chat with it privately over your own Telegram bot.

## Requirements
- OpenClaw 2026.10 or newer, with the Gateway running (`openclaw gateway status`).
- An API key or subscription for at least one model provider.
- Optional: a web search provider in OpenClaw, for researching prospects before a proposal.

## Install (about 10 minutes)
1. **Unzip** the download. You'll have a `package/` folder (the agent) plus this guide and `config/`.
2. **Make sure the Gateway is running** (`openclaw gateway status`; start it with `openclaw gateway start`), so the schedule is created during install.
3. **Turn on Claws** (OpenClaw's agent-package feature is still experimental):
   ```bash
   export OPENCLAW_EXPERIMENTAL_CLAWS=1
   ```
4. **Preview** what will be installed. Nothing changes yet:
   ```bash
   openclaw claws add ./package --dry-run
   ```
5. **Install** by approving that exact plan:
   ```bash
   openclaw claws add ./package --yes --plan-integrity <value from step 4>
   ```
6. **Check** it: `openclaw claws status client-desk`.
7. **Run the setup interview**: open the Control UI (`openclaw dashboard`), choose **Client Desk** and say hi. Have your rates, a past proposal or two, your email signature, and a list of current clients and unpaid invoices handy.

## Optional: chat from your phone over Telegram
1. Create a bot with @BotFather and copy the token.
2. `export TELEGRAM_BOT_TOKEN=<token>`
3. Edit `config/openclaw.telegram.patch.json5`: replace `123456789` with your Telegram user id (from @userinfobot).
4. Preview, then apply:
   ```bash
   openclaw config patch --file ./config/openclaw.telegram.patch.json5 --dry-run
   openclaw config patch --file ./config/openclaw.telegram.patch.json5
   ```
   If you already have `bindings` in your config, add the one binding from the patch to your existing list by hand instead, because the patch replaces the list.
5. Forward client emails or paste call notes to your bot from anywhere.

## Keeping the tracker current
Tell the agent when things change: "INV-031 was paid", "Acme project started today, deadline 30 Nov", "sent the update to Priya". It updates the CSV and confirms. You can also edit the CSVs directly in a spreadsheet; keep the header row unchanged and dates as YYYY-MM-DD.

## Change the model or Monday time
```bash
openclaw config set agents.entries.client-desk.model.primary "<provider/model>"
openclaw cron list
openclaw cron edit <job-id> --cron "0 9 * * 1" --tz "Europe/London"
```

## Try it
- "Write a proposal for this enquiry: <paste email>."
- "Which invoices are overdue? Draft the reminders."
- "Turn these notes into a weekly update for Acme: <notes>."
- "The client wants two extra pages. Draft a reply that quotes it as an add-on."
- "Review this contract before I sign: <paste or attach PDF>."
- "Here's the call transcript, pull out action items and update the project."

## Safety and permissions
- The agent reads and writes only inside its own workspace, can read PDFs, search the web, use memory and manage its own schedule. Shell commands and the browser are off.
- It never sends emails, messages or invoices, and never agrees to prices, dates or terms for you.
- Contract reviews are a first pass, not legal advice.

## Update or remove
- Update: `openclaw claws update client-desk --from ./package --dry-run`, then apply with `--yes --plan-integrity <value>`. Your edited profile and tracking files are kept.
- Remove: `openclaw claws remove client-desk --dry-run`, then apply. Edited files and your client folders are preserved.

## Troubleshooting
- **"unknown command claws"**: set `OPENCLAW_EXPERIMENTAL_CLAWS=1` and check `openclaw --version` is 2026.10 or newer.
- **Install says "partial" and mentions the Gateway**: the agent and its files were created, but the schedule needs the Gateway. Start it (`openclaw gateway start`), then run `openclaw claws update client-desk --dry-run` and apply that plan with `--yes --plan-integrity <value>` to create the schedule.
- **Model not found notice**: configure that provider (`openclaw configure`) or change the model.

Need help? The setup service includes installation, importing your clients and invoices, and tuning proposals to your past wins.
