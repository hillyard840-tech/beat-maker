# Red-flag wording patterns

Each pattern lists the wording to look for, why it matters, a typical severity for the receiving party, and a fair fix. Adjust severity to the contract's value and the user's side.

## Liability and indemnity

**Uncapped liability for one party only**
Look for: "in no event shall [Company]'s liability exceed..." with no matching cap for the other party.
Why: one side's maximum loss is fixed, the other's is unlimited.
Severity: Critical.
Fix: "Each party's total aggregate liability arising out of or relating to this Agreement shall not exceed the fees paid or payable under this Agreement in the twelve (12) months preceding the claim."

**Indemnify "any and all" claims**
Look for: "shall indemnify, defend and hold harmless ... from any and all claims, losses ... arising out of or in connection with this Agreement".
Why: covers the other party's own negligence and anything loosely connected.
Severity: High to Critical.
Fix: limit to claims "to the extent caused by the indemnifying party's breach of this Agreement, negligence or wilful misconduct", make it mutual, and subject it to the liability cap except for IP infringement and confidentiality breaches.

**Consequential damages excluded for one side only**
Severity: High. Fix: make the exclusion mutual.

## Money

**Pay-when-paid / pay-if-paid**
Look for: "payment will be made within X days of [Client] receiving payment from its customer".
Severity: High for a subcontractor. Fix: fixed payment terms from invoice date.

**Long payment terms**
Look for: net 60, net 90, or "upon approval" with no deadline for approval.
Severity: Medium (High for a small business). Fix: net 30 with deemed approval after 10 business days.

**No right to suspend**
Severity: Medium. Fix: "If any undisputed invoice is more than fifteen (15) days overdue, Provider may suspend performance on five (5) days' written notice until payment is received."

**Unilateral price changes**
Look for: "fees may be changed at any time", "pricing as published on our website".
Severity: High for subscriptions. Fix: price fixed for the term; renewal increases capped (for example at 5% or CPI) with 60 days' notice.

## Term and exit

**Auto-renewal with a short or hidden cancellation window**
Look for: "automatically renews for successive [12-month] terms unless notice is given at least 90 days prior".
Severity: High. Fix: shorten the notice window to 30 days, require the vendor to send a renewal reminder, or make renewals month-to-month.

**No termination for convenience for the user**
Severity: Medium to High depending on term length. Fix: either party may terminate on 30 days' written notice, with payment for work done.

**Termination for cause with no cure period**
Severity: Medium. Fix: 30-day cure period for curable breaches.

## Intellectual property

**Assignment of all IP including pre-existing**
Look for: "all intellectual property created, conceived or used in connection with the Services", "including any pre-existing materials".
Severity: Critical for a freelancer or agency.
Fix: assignment only of deliverables created specifically for the client, on full payment; provider keeps pre-existing IP and grants a non-exclusive perpetual licence to use it as part of the deliverables.

**IP transfers on creation, not on payment**
Severity: Medium for providers. Fix: "upon receipt of full payment".

**Employee IP clause covering personal projects**
Look for: "all inventions during the period of employment, whether or not during working hours".
Severity: High. Fix: limit to inventions related to the employer's business or made with its resources.

**Vendor may use customer data to train models or "improve services"**
Severity: Medium to High depending on data sensitivity. Fix: no use beyond providing the service without opt-in consent; aggregated and de-identified only.

## Restrictive covenants

**Broad non-compete**
Look for: any non-compete on an individual, especially longer than 12 months or with no geography.
Severity: High. Fix: delete, or limit to named direct competitors for 6 months. Note that enforceability varies widely by jurisdiction.

**Non-solicit of customers that also bans working with anyone who contacts you**
Severity: Medium.

## Confidentiality

**Perpetual confidentiality for ordinary information**
Severity: Low to Medium. Fix: 3 to 5 years, perpetual only for trade secrets.

**Missing standard exclusions**
Severity: High. Fix: add public, already known, independently developed, received from a third party without restriction.

**Residuals clause**
Look for: "information retained in the unaided memory".
Severity: High for the disclosing party. Fix: delete.

## Disputes and law

**Forum far away from the user**
Severity: Medium (High for small amounts where travel cost exceeds the claim). Fix: defendant's home courts, or online arbitration.

**Waiver of jury trial or class actions in consumer contexts**
Severity: Medium; note for the user's awareness.

**Unilateral amendment**
Look for: "we may modify these terms at any time by posting".
Severity: High. Fix: changes only by written agreement signed by both, or a right to terminate without penalty if terms change.

## Drafting problems (usually Low unless they change meaning)
- "Best efforts" vs "reasonable efforts": best efforts can imply spending beyond reason; prefer "commercially reasonable efforts".
- "Including" without "without limitation" in definitions that should be closed lists (or the reverse).
- Undefined capitalised terms.
- Order of precedence missing when an SOW conflicts with the MSA.
- "Days" without saying business or calendar days.
