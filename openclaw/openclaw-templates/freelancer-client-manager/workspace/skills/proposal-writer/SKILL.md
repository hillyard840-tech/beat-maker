---
name: proposal-writer
description: Write client proposals, quotes and statements of work from a brief or call notes, priced from the rate card, with clear scope, timeline and terms.
---

# Proposal Writer

A good proposal mirrors the client's problem back to them, shows a clear plan, and makes saying yes easy. It is short (1–3 pages), specific, and priced from `business/rate-card.md`.

## Inputs
- The brief, enquiry email, or call notes (if it's a transcript, run `meeting-to-actions` first to pull out goals and requirements).
- Client name and contact; budget or deadline if mentioned.
- Optionally, research on the client's business (`web_search` if the owner asks).

## Procedure
1. **Extract** the client's goals, problems, requirements, constraints (budget, deadline), and anything unclear. List unclear points as questions for the owner before drafting if they change price or scope by a lot; otherwise make a stated assumption.
2. **Choose the offer**: a package from the rate card, or a custom scope. Offer up to three options (good / better / best) when the budget is unknown, with the recommended one in the middle.
3. **Price** only from the rate card: hours × rate, day rate, or package price. Show your working to the owner (not in the proposal) so they can check it. Never discount unless the owner says so.
4. **Draft** using the structure below in the owner's voice.
5. **Check**: every deliverable is specific and countable, revisions are capped, exclusions are listed, dates are realistic against the process in the profile.

## Proposal structure
```
# Proposal: <project name> for <client>
Prepared by <name>, <date>. Valid until <date>.

## What you need
<2–4 sentences restating their goal and problem in their words>

## The plan
<phases from the owner's process, each with what happens and what the client gets>

## Deliverables
- <specific, countable items, e.g. "Homepage and 4 inner page designs (Figma)">

## Not included
- <common assumptions to rule out, e.g. copywriting, hosting, stock images>

## Timeline
<phase → date or week range; note what depends on client feedback times>

## Investment
<option table or single price; deposit and payment terms from the rate card>

## Why me
<1–2 relevant proof points from the profile; only real ones>

## Next steps
1. Reply to confirm the option you'd like.
2. I'll send the agreement and deposit invoice.
3. We start on <date>.
```

Save to `clients/<client-slug>/YYYY-MM-DD-proposal.md`, add or update the project in `tracking/projects.csv` with status `proposal`, and give the owner a short cover email to send with it.

## Rules
- No invented case studies, testimonials, logos or numbers. Mark missing proof as `[add: relevant example]`.
- Scope is the main protection against overruns: always include "Not included" and the revision limit.
- If the client's budget is clearly below the minimum project size, say so to the owner and offer a smaller scope option instead of under-pricing.
