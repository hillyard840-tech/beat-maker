# Worked example: weekly product sync

Assume today's date is Saturday 2026-10-17 and the meeting was the day before, Friday 2026-10-16.

## User message

> Can you turn this into a recap with action items? Meeting was yesterday.
>
> [00:00] Priya: ok I think everyone's here, can you hear me? great
> [00:20] Priya: so main thing today is the Acme renewal and the onboarding redesign
> [01:05] Marcus: Acme's asking for 15% off. I think we can do 10 if they sign for two years
> [01:40] Priya: I'm fine with 10 on a two-year. Let's go with that
> [01:52] Marcus: ok I'll send them the revised quote Monday
> [02:30] Lena: on onboarding, the test results are in. Completion went from 41 to 58 percent with the new checklist
> [03:10] Priya: that's great. so we ship it to everyone?
> [03:15] Lena: I'd want design to fix the mobile layout first
> [03:40] Sam: I can do the mobile fixes, probably by end of next week
> [04:05] Priya: ok so ship after Sam's fixes. Lena can you write the release note
> [04:12] Lena: yep
> [05:00] Marcus: also, does anyone know if legal signed off on the new DPA template? Acme will ask
> [05:10] Priya: no idea, someone should check
> [05:30] Sam: by the way the staging server's been flaky, might slow me down
> [06:00] Priya: ok thanks all

## Expected output

# Product sync — 2026-10-16
Attendees: Priya, Marcus, Lena, Sam

## TL;DR
The team agreed the Acme renewal terms and decided to roll out the new onboarding checklist to all users once mobile layout fixes are done. Whether legal has approved the new DPA template is still unknown.

## Decisions
1. Offer Acme a 10% discount in exchange for a two-year renewal (Acme asked for 15%).
2. Ship the new onboarding checklist to all users after the mobile layout fixes are complete. It raised completion from 41% to 58% in testing.

## Action items
| # | Owner | Task | Due | Source |
|---|---|---|---|---|
| 1 | Marcus | Send Acme the revised quote (10% off, two-year term) | 2026-10-19 | "I'll send them the revised quote Monday" (01:52) |
| 2 | Sam | Fix the onboarding mobile layout | 2026-10-23 (said "probably by end of next week") | (03:40) |
| 3 | Lena | Write the release note for the onboarding rollout | No date set, after item 2 | "yep" to Priya's request (04:12) |
| 4 | Unassigned | Check whether legal approved the new DPA template | No date set | (05:00–05:10) |

## Open questions
- Who will check the DPA template status with legal? Acme is likely to ask. (raised by Marcus)

## Risks and blockers
- Staging server instability may delay Sam's mobile fixes (item 2), which would delay the onboarding rollout.

## Follow-up email
Subject: Product sync — recap and next steps

Hi all,

Thanks for today. Here's where we landed.

Decisions
- Acme: we'll offer 10% off for a two-year renewal.
- Onboarding: the new checklist ships to everyone once the mobile layout is fixed.

Next steps
- Marcus: send Acme the revised quote by Monday 19 October.
- Sam: mobile layout fixes, aiming for Friday 23 October.
- Lena: release note for the rollout, once Sam's fixes are in.

Open
- We still need someone to confirm with legal whether the new DPA template is approved, since Acme will likely ask. Can someone pick this up?

Note that the flaky staging server could slow down the mobile fixes.

Thanks,
Priya
