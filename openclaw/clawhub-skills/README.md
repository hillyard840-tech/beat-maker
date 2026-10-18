# ClawHub skills: research, picks and publishing guide

Four OpenClaw skills, each ready to publish to ClawHub, plus the research behind the choice and the exact steps to publish.

| Folder | Display name | What it does |
|---|---|---|
| `contract-redline-reviewer/` | Contract Redline Reviewer | Reviews NDAs, MSAs, SaaS, freelance and lease agreements for red flags and missing clauses, rates severity, and writes replacement wording and a message to the other side. |
| `meeting-to-actions/` | Meeting to Actions | Turns transcripts or rough notes into decisions, owned action items with real due dates, open questions and a follow-up email. Also standup, executive brief, client-safe and JSON formats. |
| `pr-review-assistant/` | PR Review Assistant | Reviews a PR, branch or diff for bugs, security and performance issues with checklists for 11 languages and stacks, and drafts ready-to-post GitHub comments. |
| `product-description-writer/` | Product Description Writer | Writes titles, bullets, descriptions and SEO fields for Shopify, Amazon, Etsy, WooCommerce and eBay, with platform limits, compliance checks and CSV bulk mode. |

Each skill folder contains `SKILL.md` (the skill itself, with ClawHub frontmatter), a `references/` folder the skill reads during use, and a worked example in `examples/`. `tools/` holds a validator and a publish script; it is not part of any skill.

## Important: ClawHub does not pay per install

ClawHub's own skill-format documentation says paid skills, pricing, paywalls and revenue sharing are not supported, and every published skill is licensed MIT-0 (anyone may use, copy and resell it without attribution). Source: https://docs.openclaw.ai/clawhub/skill-format

So the "earn per install" model in the project brief is not available on ClawHub today. Realistic ways to make money from these skills:
- **Funnel to a paid product.** Publish free skills that build a reputation and link (via the `homepage` frontmatter field) to a paid offering: a hosted version, a premium skill pack sold elsewhere (Gumroad, your site), templates, or consulting.
- **Services.** The contract and product-copy skills are natural lead magnets for freelance legal-ops, e-commerce copywriting, or agency work.
- **Your own API.** A skill can call a paid API you run, as long as it declares the required key in `metadata.openclaw` and explains the cost. That would be a later version, not these v1 skills.

## What the data says sells

Source: daily snapshot of the top 1,000 ClawHub skills by downloads (LinklyAI/best-skills dataset, 2026-10-18, which pulls from ClawHub's public API), plus ClawHub's docs and several 2026 roundups.

- **Downloads follow a power law.** #1 (`self-improving-agent`) has about 486k downloads; #100 has about 32k; #1,000 has about 6k. The registry median is 171 downloads (trent.ai analysis of 52,652 packages), and 65% of packages have never been installed.
- **Top categories:** agent self-improvement and memory, skill security vetting, CLI integrations (GitHub, Google Workspace, Notion, Obsidian, Slack), web search, office documents (Word, Excel, PDF, PowerPoint), and writing tools (`humanizer` is #13 with 133k).
- **Business workflow skills do well when they're the clear option for a task:** `copywriting` 25.6k, `resume-assistant` 21.7k, `code-review` 19.1k, `seo` 18.7k, `ai-meeting-notes` 12.1k. Summarisation skills together account for about 710k downloads.
- **Gaps:** no contract or legal-review skill and no dedicated product-description skill appears anywhere in the top 1,000; meeting notes and PR review have only small, generic entries.

## Why these four

| Skill | Demand signal | Competition in top 1,000 | Why it can win |
|---|---|---|---|
| Meeting to Actions | Summaries ~710k combined; `ai-meeting-notes` 12k | Weak, generic summarizers | Strict owner/date rules, source-traced actions, client-safe mode, JSON |
| PR Review Assistant | `github` 201k; `code-review` 19k; `pr-reviewer` 8k | Thin prompt-only reviewers | Verification step, 11 language and stack checklists, security checklist, GitHub-ready comments |
| Product Description Writer | `copywriting` 25.6k, `seo` 18.7k, Shopify/Woo integrations 8–16k | None dedicated | Platform limits, compliance rules, bulk CSV |
| Contract Redline Reviewer | Legal is a top AI use case generally; no ClawHub data yet | None | First mover; redlines plus negotiation email |

Two picks (meetings, PR review) are in proven categories; two (contracts, product copy) are gaps where demand is inferred rather than measured. I skipped the very top categories (self-improvement, memory, search, security vetting) because they are saturated by early, heavily starred entries and usually need code and API keys, which brings more security-scan risk.

All four are prompt-only (no scripts, no API keys, no network calls), which is the lowest-risk profile for ClawHub's automated security scan.

## Publishing steps (for dean)

Nothing has been published and no accounts were created.

1. **Have a GitHub account old enough to pass ClawHub's upload gate.** ClawHub signs in with GitHub and blocks very new accounts from publishing (the exact age isn't published).
2. **Install the CLI** (Node.js required): `npm i -g clawhub`. Tested with CLI v0.23.3.
3. **Log in:** `clawhub login`, approve the device code in your browser, then check with `clawhub whoami`.
4. **Copy this folder to your computer** (download `clawhub-skills/` from the project files).
5. **Validate:** `python3 tools/validate_skills.py` (all four currently pass).
6. **Dry run:** `bash tools/publish_all.sh` previews each publish without uploading. (A dry run could not be completed from my environment because it blocks clawhub.ai; run it on your machine.)
7. **Publish:** `bash tools/publish_all.sh --live`. Or one at a time:
   ```bash
   clawhub skill publish ./meeting-to-actions --slug meeting-to-actions --name "Meeting to Actions" --version 1.0.0 --topics "meetings,productivity,summarize,notes" --changelog "Initial release"
   ```
8. **Check the scan.** ClawHub runs automated security checks after upload. If a skill is held, it disappears from the public catalog but stays visible in your `/dashboard` on clawhub.ai with the findings.
9. **Optional:** add `homepage: https://your-site` to each `SKILL.md` frontmatter if you want a link back to a paid offering, then republish (the CLI bumps the patch version automatically).

Optional extra: `--categories <slugs>` is supported by the CLI, but ClawHub doesn't publish the list of valid category slugs. Pick categories in the web UI after publishing if you want them.

## Before you publish: test each skill once

Install locally by copying a skill folder into `~/.openclaw/workspace/skills/`, run `openclaw skills list` to confirm it loaded, start a new session (`/new`), and try the prompt from that skill's `examples/` file. Compare the result with the expected output in the same file.

## Updating later

Edit the files, then rerun `clawhub skill publish ./<folder> --slug <slug> --name "<name>" --changelog "<what changed>"` without `--version`; ClawHub publishes the next patch version and skips unchanged content.

## Sources
- ClawHub skill format, license and paid-skill policy: https://docs.openclaw.ai/clawhub/skill-format
- ClawHub quickstart and publishing: https://docs.openclaw.ai/clawhub/quickstart
- ClawHub CLI reference: https://docs.openclaw.ai/clawhub/cli
- ClawHub acceptable usage: https://docs.openclaw.ai/clawhub/acceptable-usage
- OpenClaw SKILL.md frontmatter: https://docs2.openclaw.ai/tools/creating-skills
- Top-1,000 download data: https://github.com/LinklyAI/best-skills (file `data/2026-10-18/raw/clawhub.csv`; a copy is in `research/clawhub-top1000-2026-10-18.csv`)
- Registry-wide statistics: https://trent.ai/blog/clawhub-by-the-numbers/
