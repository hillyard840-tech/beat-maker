---
name: content-calendar-planner
description: Plan a weekly or monthly content calendar across channels from the owner's pillars, capacity and launches, and repurpose one piece into many.
---

# Content Calendar Planner

Turn the owner's goals and capacity into a calendar they can actually keep. A smaller plan that gets done beats an ambitious one that doesn't.

## Inputs (read from the workspace first)
- `brand/brand-profile.md`: channels, posts per week, timezone, offers and launches.
- `brand/content-pillars.md`: pillars, share of content, content mix.
- `calendar/`: previous weeks, to avoid repeats and carry over unfinished items.
- `research/`: keyword opportunities to schedule.
- Anything the owner adds: holidays, events, launches, a busy week.

## Weekly plan procedure
1. Count slots: the posts per week for each channel in the brand profile.
2. Choose **one theme for the week** that ties to a pillar or a launch.
3. Anchor with **one core piece** (blog post, newsletter, video) if the owner publishes long-form, then derive social posts from it.
4. Fill remaining slots following the pillar shares and the teach/prove/connect/sell mix.
5. Carry over unfinished items from last week first, and say so.
6. Add relevant dates (holidays, industry events) only if they fit the audience.

## Output format
Save to `calendar/YYYY-Www.md` (ISO week):

```
# Content plan: week <ww>, <start date> – <end date>
Theme: <theme>
Core piece: <title> (<format>) — keyword: <keyword or "none">

| Day | Channel | Pillar | Type | Idea / working title | Status |
|---|---|---|---|---|---|
| Mon | Blog | <pillar> | Teach | <title> | ☐ |
| Tue | LinkedIn | <pillar> | Connect | <hook idea> | ☐ |

## Prep list
- [ ] <what to film, photograph, gather, or ask a customer for>

## Carried over from last week
- <item> or "Nothing"
```

The owner marks items done by changing ☐ to ✓; read those marks next week.

## Monthly plan
Same table grouped by week, plus a one-line goal per week and launch dates highlighted. Keep it to the agreed capacity.

## Repurpose mode
Given one source piece (a post, video transcript, podcast, or newsletter), produce a repurposing map: 5–10 derivative pieces, each with channel, format, the specific idea or quote it uses, and a hook. Then offer to write them with `social-caption-writer` and `blog-post-outliner`.

## Rules
- Never schedule more than the capacity in the brand profile without asking.
- Don't schedule sales posts back to back; spread them.
- Plans are drafts; the owner decides what goes live.
