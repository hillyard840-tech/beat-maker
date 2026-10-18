# Content Creator Suite: setup guide

Content Studio is an OpenClaw agent that plans your content, outlines blog posts and videos, writes platform-native captions, researches keywords and sends you a content plan every Monday, all in your brand voice. It drafts; you post.

## What's included
- **Agent**: Content Studio (`content-studio`), with its persona, operating rules and a first-run brand interview.
- **Skills**: blog post outliner, social caption writer (9 platforms), SEO keyword researcher, content calendar planner (with repurpose mode).
- **Brand files**: brand profile and content pillars, filled in during the interview.
- **Schedule**: a weekly content plan every Monday at 08:00 UTC (you can change the time).
- **Model**: Claude Sonnet 5.5, with GPT-5.5 as fallback. Swap in any model you have configured.
- **Optional phone access**: a ready config to chat with it privately over your own Telegram bot.

## Requirements
- OpenClaw 2026.10 or newer, with the Gateway running (`openclaw gateway status`).
- An API key or subscription for at least one model provider (Anthropic, OpenAI, or another; see "Change the model").
- For keyword research: a web search provider configured in OpenClaw (Brave, Tavily, Exa, Perplexity, DuckDuckGo or another). Without one, research still works but is labelled unverified.

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
   You'll see the new agent, its files, its tool permissions, the Monday schedule, and a `planIntegrity` value.
5. **Install** by approving that exact plan:
   ```bash
   openclaw claws add ./package --yes --plan-integrity <value from step 4>
   ```
6. **Check** it: `openclaw claws status content-studio`.
7. **Start the brand interview**: open the Control UI (`openclaw dashboard`), choose the **Content Studio** agent and say hi. It will ask about your audience, voice, channels and pillars, and fill in its brand files. This takes about 10 minutes and makes every draft sound like you.

## Optional: chat from your phone over Telegram
1. Create a bot with @BotFather in Telegram and copy the token.
2. `export TELEGRAM_BOT_TOKEN=<token>` (or store it with `openclaw secrets`).
3. Edit `config/openclaw.telegram.patch.json5`: replace `123456789` with your Telegram user id (message @userinfobot to get it).
4. Preview, then apply:
   ```bash
   openclaw config patch --file ./config/openclaw.telegram.patch.json5 --dry-run
   openclaw config patch --file ./config/openclaw.telegram.patch.json5
   ```
   Note: the patch sets the `bindings` list. If you already have bindings in your config, add the one line from the patch to your existing list by hand instead.
5. Message your bot. Only your user id can talk to it.

## Change the model
The agent uses `anthropic/claude-sonnet-5-5` with `openai/gpt-5.5` as fallback. To use something else, edit `package/profiles/openclaw.yml` before installing, or after installing run:
```bash
openclaw config set agents.entries.content-studio.model.primary "<provider/model>"
```

## Change the Monday plan time
```bash
openclaw cron list
openclaw cron edit <job-id> --cron "0 8 * * 1" --tz "America/New_York"
```

## Try it
- "Plan my content for next week."
- "Outline a blog post about <topic> for <audience>."
- "Turn this post into captions for LinkedIn, Instagram and X: <paste>."
- "Find keyword opportunities for <pillar>."
- "Repurpose this podcast transcript into 8 pieces."

## Safety and permissions
- The agent can read and write only inside its own workspace, search the web, use memory, and manage its own schedule. Shell commands and the browser are turned off.
- It never posts, publishes or emails anything. All output is drafts.
- It won't invent statistics, testimonials or search volumes.

## Update or remove
- Update to a newer version: `openclaw claws update content-studio --from ./package --dry-run`, then apply with `--yes --plan-integrity <value>`. Files you've edited are kept.
- Remove: `openclaw claws remove content-studio --dry-run`, then apply. Your edited brand files and drafts are preserved.

## Troubleshooting
- **"unknown command claws"**: set `OPENCLAW_EXPERIMENTAL_CLAWS=1` and check `openclaw --version` is 2026.10 or newer.
- **Install says "partial" and mentions the Gateway**: the agent and its files were created, but the schedule needs the Gateway. Start it (`openclaw gateway start`), then run `openclaw claws update content-studio --dry-run` and apply that plan with `--yes --plan-integrity <value>` to create the schedule.
- **Model not found notice**: configure that provider (`openclaw configure`) or change the model as above.
- **Searches fail**: set up a web search provider in OpenClaw.

Need help? The setup service includes installation, connecting your channels and a tuned brand profile.
