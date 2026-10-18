# First-run setup: business interview

This first conversation is with the owner, before any customer is connected. Run this interview so you can answer customers correctly. Ask one group at a time, accept short answers, and tell the owner they can paste existing material (a website FAQ page, a returns policy, a menu) instead of typing.

1. **The business.** Name, what you sell, website, address or service area, opening hours, and how customers usually contact you.
2. **Top questions.** "What are the 10 questions customers ask most? Give me the answer you'd give." Or ask them to paste their FAQ page.
3. **Policies.** Shipping times and costs, returns and refunds, exchanges, cancellations, warranties, booking or deposit rules, payment methods. Ask for exact numbers (days, amounts) and any exceptions.
4. **Orders.** "Where do you check order status today?" (Shopify, WooCommerce, Square, Etsy, a spreadsheet.) Explain the order lookup options in `skills/order-status-checker/SKILL.md` and agree which one to use.
5. **Escalation.** Who should get handoffs, on which channel and number or username, during which hours, and what you're allowed to offer an upset customer without asking (for example: an apology and a 10% discount code, or nothing).
6. **Voice.** Three words for how the business should sound, plus any words to avoid. Emoji: yes or no.
7. **Timezone.** For the daily digest (scheduled weekdays 18:00 UTC).

Then:
- Fill in every `[placeholder]` in `business/business-facts.md`, `business/faq.md`, `business/policies.md` and `business/escalation.md`. Show the owner a summary of the policies and escalation rules and ask them to confirm, because customers will be told exactly this.
- If their timezone differs, tell them how to move the digest: `openclaw cron list`, then `openclaw cron edit <job-id> --tz "<their timezone>"`.
- Remind them to finish the channel and safety settings in the setup guide before connecting customers (separate sessions per customer, and no file editing for customers).
- Offer a test: "Ask me a few questions as if you were a customer."

When the files are complete and confirmed, delete this file (BOOTSTRAP.md).
