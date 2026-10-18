# Worked example: freelance design agreement

## User message

> I'm a freelance designer. A client sent me this. Anything I should push back on?
>
> **Design Services Agreement** between Northwind Retail Ltd ("Client") and Jamie Doe ("Designer").
> 1. Services. Designer will provide brand design services as requested by Client from time to time.
> 2. Fees. Client will pay $6,000, payable within 60 days of Client's approval of the final deliverables.
> 3. Intellectual Property. All work product, concepts, drafts and materials created or used by Designer in connection with the Services, including any pre-existing materials, are the sole property of Client upon creation.
> 4. Liability. Designer shall indemnify Client against any and all claims arising out of the Services. Client's liability shall not exceed $500.
> 5. Non-compete. Designer shall not provide design services to any retail business for 24 months after this Agreement ends.
> 6. Term. This Agreement continues until terminated by Client.
> 7. Governing law. Laws of the State of Delaware.

## Expected output

## Summary
Verdict: Negotiate before signing
This is a fixed-fee design contract that is heavily tilted toward the client. The biggest issue is section 3, which hands over your pre-existing work and tools the moment you create anything, before you are paid.
Reviewed as: Designer (service provider), freelance services agreement, governing law Delaware

## Findings
| # | Severity | Section | Issue | Fix |
|---|---|---|---|---|
| 1 | Critical | 3 | Assigns your pre-existing materials and transfers IP on creation, before payment | Assign only final deliverables, on full payment (redline 1) |
| 2 | Critical | 4 | Uncapped "any and all" indemnity from you; client's liability capped at $500 | Mutual cap at the fee; indemnity limited to your breach or negligence (redline 2) |
| 3 | High | 5 | 24-month non-compete across the entire retail sector | Delete, or limit to named competitors for 6 months (redline 3) |
| 4 | High | 1 | Scope is "as requested from time to time" for a fixed fee, so there is no limit on work | Define deliverables and revision rounds; extra work at a day rate (redline 4) |
| 5 | High | 2 | Payment 60 days after an approval with no deadline, so the client can delay payment indefinitely | 50% deposit, balance net 30, deemed approval after 10 business days (redline 5) |
| 6 | Medium | 6 | Only the client can terminate, and there is no payment for work done on termination | Either party on 14 days' notice; pay for work done |
| 7 | Low | — | No portfolio right | Add a right to display the work after public launch |

## Redlines
### 1. Section 3, Intellectual Property
Current: "All work product, concepts, drafts and materials created or used by Designer in connection with the Services, including any pre-existing materials, are the sole property of Client upon creation."
Proposed: "Upon receipt of full payment, Designer assigns to Client all rights in the final deliverables listed in Schedule A. Designer retains ownership of all pre-existing materials, tools, templates and unused concepts, and grants Client a non-exclusive, perpetual licence to use any pre-existing materials incorporated in the final deliverables."
Why: you keep your toolkit and only transfer what you were paid for.

### 2. Section 4, Liability
Current: "Designer shall indemnify Client against any and all claims arising out of the Services. Client's liability shall not exceed $500."
Proposed: "Each party shall indemnify the other against third-party claims to the extent caused by its breach of this Agreement or its negligence. Each party's total liability under this Agreement shall not exceed the fees paid under it."
Why: makes risk mutual and limits your exposure to the value of the job.

### 3. Section 5, Non-compete
Current: "Designer shall not provide design services to any retail business for 24 months after this Agreement ends."
Proposed: delete the section. Fallback: "For six (6) months after this Agreement ends, Designer will not provide brand identity services to the following direct competitors of Client: [list]."
Why: as written it could bar you from a large part of your market for two years.

### 4. Section 1, Services
Current: "Designer will provide brand design services as requested by Client from time to time."
Proposed: "Designer will provide the deliverables described in Schedule A, including up to two (2) rounds of revisions. Additional work will be quoted separately or billed at $[rate] per day."
Why: caps a fixed fee to a fixed scope.

### 5. Section 2, Fees
Current: "payable within 60 days of Client's approval of the final deliverables."
Proposed: "Client will pay 50% on signature and 50% within 30 days of delivery. Deliverables are deemed approved if Client does not reject them in writing with reasons within ten (10) business days."
Why: stops payment being delayed by withholding approval.

## Missing clauses
- Termination payment: you should be paid for work done if the client ends the project early.
- Portfolio rights: lets you show the work once it is public.
- Independent contractor status: confirms you are not an employee.
- Late-payment remedy: the right to pause work if invoices are overdue.

## Message to the other side
Hi, thanks for sending this over. I'm happy to proceed and have a few changes that are standard for design engagements: IP in the final deliverables transfers to you on payment, while I keep my pre-existing tools and templates; liability is mutual and capped at the project fee; the non-compete is removed; scope is defined in a short schedule with two rounds of revisions; and payment is 50% upfront with the balance 30 days after delivery. I've attached suggested wording. Happy to talk through any of it.

## Questions to ask
- Which specific deliverables and file formats does the client expect?
- Is there a launch date? It affects the approval window and the portfolio right.

---
This is an automated first-pass review, not legal advice. For high-value or unusual agreements, have a qualified lawyer in the governing jurisdiction review it.
