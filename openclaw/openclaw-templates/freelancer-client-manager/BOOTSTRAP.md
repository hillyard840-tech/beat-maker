# First-run setup: freelancer interview

Run this short interview in your first conversation so proposals and emails come out right the first time. One group of questions at a time; short answers are fine; pasting existing material (a past proposal, rate sheet, email signature) is even better.

1. **What you do.** Your name or studio name, services, who your typical clients are, and 2–3 past projects or results you're proud of (these go into proposals).
2. **Rates and terms.** Hourly or day rate, typical package prices, deposit, payment terms (e.g. 50% upfront, net 14), late fee if any, accepted payment methods, currency.
3. **Process.** How a project usually runs (discovery, drafts, revisions, delivery), number of revision rounds included, typical timelines.
4. **Voice.** Paste one or two emails you've sent clients that you liked, or describe your tone in three words. Your email signature.
5. **Current clients.** List active clients and projects (name, contact person, project, deadline, next milestone) and any unpaid invoices (number, client, amount, due date). I'll add them to the tracking files.
6. **Timezone.** For the Monday check (scheduled 09:00 UTC).

Then:
- Fill in `business/freelancer-profile.md` and `business/rate-card.md`, and add their clients, projects and invoices to `tracking/clients.csv`, `tracking/projects.csv` and `tracking/invoices.csv` (remove the EXAMPLE rows). Show a summary and ask them to confirm.
- If their timezone differs, show how to move the Monday check: `openclaw cron list`, then `openclaw cron edit <job-id> --tz "<their timezone>"`.
- Offer a first quick win: "Any overdue invoices I can draft a reminder for right now?"

When everything is confirmed, delete this file (BOOTSTRAP.md).
