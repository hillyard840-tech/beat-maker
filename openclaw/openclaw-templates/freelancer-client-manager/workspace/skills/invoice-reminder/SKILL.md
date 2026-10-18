---
name: invoice-reminder
description: Draft polite, escalating payment reminders for upcoming and overdue invoices from the invoice tracker, and keep the tracker up to date.
---

# Invoice Reminder

Getting paid on time is mostly about reminding early, clearly and without awkwardness. Read the invoice row in `tracking/invoices.csv`, the client in `tracking/clients.csv`, and terms in `business/rate-card.md`.

## Which reminder
Count days relative to `due_date` (today minus due date):

| Stage | When | Tone |
|---|---|---|
| 0. Heads-up | 3 days before due | Friendly, helpful, assumes it's in hand |
| 1. Due today / just overdue | 0 to 3 days after | Light, "just a reminder" |
| 2. Overdue | 7 to 14 days after | Clear, mentions the date and amount, asks for an expected payment date |
| 3. Firm | 21 to 30 days after | Direct, references terms (late fee if the rate card has one), mentions pausing work if applicable |
| 4. Final notice | 45+ days after | Formal, states next steps the owner has chosen (pause work, late fee, collections). Only when the owner confirms |

Skip a stage if a reminder was sent fewer than 5 days ago (`last_reminder_date`). For `disputed` invoices, don't send a reminder; draft a message that addresses the dispute and flag it to the owner.

## Every reminder includes
- Invoice number, amount with currency, original due date.
- How to pay (method or link from the rate card or invoice).
- One clear ask ("Could you let me know when this will be paid?").
- A subject line that's easy to find later: `Invoice <number> – <amount> due <date>`.
- The owner's signature.

## Examples
Stage 1:
> Subject: Invoice INV-024 – $1,500 due 15 Oct
> Hi Alex, a quick reminder that invoice INV-024 for $1,500 was due yesterday (15 October). I've attached it again in case it got buried. You can pay by bank transfer to the details on the invoice. Thanks!

Stage 3:
> Subject: Invoice INV-024 – $1,500, now 21 days overdue
> Hi Alex, invoice INV-024 for $1,500 was due on 15 October and is now three weeks overdue. Per our agreement, payment is due within 14 days, and I'll need to pause work on the next phase until it's settled. Could you confirm a payment date by Friday? Happy to help if anything is holding it up on your side.

## After drafting
- Save drafts to `clients/<client-slug>/YYYY-MM-DD-reminder-<invoice>.md`.
- When the owner confirms a reminder was sent, update `last_reminder_date` and `reminder_count`. When they say it's paid, set `status` to `paid`.

## Rules
- Never threaten legal action, collections or credit reporting unless the owner explicitly decides to, and suggest they check local rules first.
- Never add a late fee that isn't in the rate card or the client's contract.
- Keep it short. Long reminders read as anxious.
