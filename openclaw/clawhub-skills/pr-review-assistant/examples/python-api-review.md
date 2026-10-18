# Worked example: Python API endpoint

## User message

> Review this before I merge. PR: "Add endpoint to download invoice PDFs"
>
> ```diff
> --- a/app/routes/invoices.py
> +++ b/app/routes/invoices.py
> @@ -1,3 +1,20 @@
>  from flask import Blueprint, send_file, request, abort
> +import os
> +from app.models import Invoice
> +from app.auth import login_required, current_user
>
>  bp = Blueprint("invoices", __name__)
> +INVOICE_DIR = "/srv/invoices"
> +
> +@bp.route("/invoices/<int:invoice_id>/pdf")
> +@login_required
> +def download_invoice(invoice_id):
> +    invoice = Invoice.query.get(invoice_id)
> +    path = os.path.join(INVOICE_DIR, invoice.filename)
> +    return send_file(path, as_attachment=True)
> +
> +@bp.route("/invoices/export")
> +@login_required
> +def export_invoices():
> +    name = request.args.get("file")
> +    return send_file(os.path.join(INVOICE_DIR, name))
> ```

## Expected output

## Verdict
Request changes — any logged-in user can download any other customer's invoice, and the export route allows reading arbitrary files on the server.

## Summary
Adds two routes to download invoice PDFs. Overall risk: High.

## Findings
### 1. 🔴 Any user can download any invoice (missing ownership check) — `app/routes/invoices.py:12`
`Invoice.query.get(invoice_id)` loads the invoice by ID without checking that it belongs to `current_user`. A logged-in user can iterate `/invoices/1/pdf`, `/invoices/2/pdf`, ... and download every customer's invoices.
**Suggested fix:**
```python
invoice = Invoice.query.filter_by(id=invoice_id, account_id=current_user.account_id).first()
if invoice is None:
    abort(404)
```

### 2. 🔴 Path traversal in export route — `app/routes/invoices.py:20`
`name` comes straight from the query string, so `/invoices/export?file=../../etc/passwd` resolves outside `INVOICE_DIR` and `send_file` returns it. The route also skips any ownership check.
**Suggested fix:** remove the route if it is not needed. Otherwise, look the file up by an invoice ID the user owns (as in finding 1), or use `send_from_directory`, which rejects paths outside the directory:
```python
from flask import send_from_directory
return send_from_directory(INVOICE_DIR, name, as_attachment=True)
```
and still verify the user owns the invoice that `name` belongs to.

### 3. 🟠 Crash when the invoice does not exist — `app/routes/invoices.py:13`
If no invoice has that ID, `invoice` is `None` and `invoice.filename` raises `AttributeError`, returning a 500 instead of a 404. Fixed by the `abort(404)` in finding 1.

### 4. 🟡 Missing file returns 500 — `app/routes/invoices.py:14`
If the PDF has not been generated yet, `send_file` raises `FileNotFoundError`. Consider checking `os.path.exists(path)` and returning 404, or generating the PDF on demand.

## Missing tests
- User A requests user B's invoice ID and gets 404.
- `file=../../etc/passwd` on the export route is rejected (or the route is removed).
- Nonexistent invoice ID returns 404, not 500.

## What looks good
- Both routes are behind `@login_required`, and `<int:invoice_id>` prevents non-numeric IDs.
