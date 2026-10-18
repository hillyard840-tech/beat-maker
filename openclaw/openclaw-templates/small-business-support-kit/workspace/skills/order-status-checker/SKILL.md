---
name: order-status-checker
description: Look up a customer's order status, tracking and delivery estimate after verifying the order number and email or postcode, and handle late or missing parcels.
---

# Order Status Checker

## Where order data comes from
The owner picks one option during setup (see `business/escalation.md` notes or ask the owner).

1. **Order export file (default).** `data/orders.csv`, exported from the shop platform and replaced by the owner, ideally daily. Columns: `order_number,email,postcode,order_date,status,carrier,tracking_url,estimated_delivery,items`. Most platforms (Shopify, WooCommerce, Square, Etsy) can export orders as CSV; rename or map columns to these headers. The `EXAMPLE-1001` row is sample data; ignore it.
2. **Store integration (optional upgrade).** If the owner has installed a store skill or MCP server for their platform, use it in read-only mode to look up the order instead of the CSV. Only read; never edit, refund or cancel orders through it.
3. **No data access.** Give the customer the store's own order-tracking page from `business/business-facts.md` if there is one, otherwise hand off.

## Procedure
1. Ask for the **order number** and the **email or postcode** used on the order. Don't look anything up until you have both.
2. Look up the order. Both values must match the same row (ignore capitals and spaces). If they don't match, say you couldn't find an order with those details and ask them to double-check. After two failed tries, hand off. Never reveal whether an order number exists when the second detail doesn't match.
3. Report, in plain words: status, carrier, tracking link, estimated delivery. Example: "Your order #1042 shipped on 3 October with UPS. Here's your tracking link: <url>. It's due by 7 October."
4. Mention when the data was last updated if the file looks older than 2 days (check the newest order date) so the customer knows to check the tracking link for live updates.

## Problems
- **Late** (past the estimated delivery): apologise, share tracking, explain the shipping policy for late parcels from `business/policies.md`, and offer only what `business/escalation.md` allows. If the policy says to wait a number of days first, say so with the date.
- **Marked delivered but not received**: suggest checking with neighbours and around the property, and the carrier's tracking page; then follow the policy, or hand off.
- **Wrong or damaged item**: switch to the `complaint-handler` skill.
- **Change or cancel**: check the cancellation policy; if allowed, hand off with the exact request (you don't edit orders).

## Never
- Share any order details unless both identifiers match.
- Read out other orders from the file, or confirm whether someone else has ordered.
- Promise a delivery date the carrier hasn't given.
