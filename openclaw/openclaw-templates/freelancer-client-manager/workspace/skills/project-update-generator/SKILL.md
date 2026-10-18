---
name: project-update-generator
description: Turn rough notes, task lists or commits into a clear client status update covering progress, next steps, blockers and what's needed from the client.
---

# Project Update Generator

Clients mostly want to know three things: is it on track, what happened, and do they need to do anything. Lead with those.

## Inputs
- Rough notes from the owner ("finished homepage, waiting on logo, contact page next"), a task list, commits, or a call summary.
- The project row in `tracking/projects.csv` (deadline, next milestone) and the client row.

## Procedure
1. Decide the **status**: 🟢 On track, 🟡 At risk (say why and what would fix it), 🔴 Delayed (new date and reason). Base it on deadline vs progress; if unclear, ask the owner.
2. Translate work into **outcomes** the client cares about ("The booking form now sends confirmations automatically" rather than "fixed webhook").
3. Make **client actions** impossible to miss: what, by when, and why it matters ("Please send the final logo by Wednesday so we keep the 24 October launch").
4. Keep it under 200 words unless it's a milestone report.

## Format
```
Subject: <Project> update – <date> – <status word>

Hi <name>,

Status: <🟢/🟡/🔴> <one sentence>

Done this week
- <outcome>

Next
- <what's coming, with dates>

Needed from you
- <action, by date> (or "Nothing this week")

<optional: one line on budget/hours used if the project is hourly>

<signature>
```

For a **milestone report**, add a short summary of everything delivered in the milestone, links to deliverables, and the approval being requested.

After the owner confirms it was sent, set `last_update_sent` in `tracking/projects.csv` to today, and set status to `waiting-on-client` if the update asks the client for something blocking.

## Rules
- Be honest about delays; bad news early is better than surprises. Pair it with a plan.
- Don't promise new dates the owner hasn't confirmed.
- Never mention other clients.
