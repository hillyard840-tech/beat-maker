# Security checklist for code review

Check each item that applies to the changed code. Report only real, reachable problems.

## Input handling
- User input reaches a SQL query by string concatenation or formatting instead of parameters.
- User input reaches a shell command (`os.system`, `subprocess(..., shell=True)`, `exec`, backticks, `child_process.exec`).
- User input forms a file path without normalisation and a base-directory check (path traversal: `../`).
- User input is rendered into HTML without escaping, or through `dangerouslySetInnerHTML`, `v-html`, `|safe`, `innerHTML`.
- User input is used in a template engine as the template itself (server-side template injection).
- User-controlled URLs are fetched server-side without an allow-list (SSRF), including redirects.
- Regular expressions built from user input, or vulnerable to catastrophic backtracking on user input (ReDoS).
- Deserialisation of untrusted data with `pickle`, `yaml.load` (not `safe_load`), Java native serialisation, `unserialize` in PHP.

## Authentication and authorization
- New endpoint or handler without an auth check where similar endpoints have one.
- Object fetched by ID without checking it belongs to the current user or tenant (IDOR).
- Role or permission checks done on the client only.
- Password, token or session comparisons not done in constant time where it matters.
- JWTs accepted without signature verification, or with `alg: none` allowed.
- Session or token lifetime extended or made non-expiring.

## Secrets and data exposure
- API keys, passwords, private keys or tokens committed in code, config or tests.
- Secrets or personal data written to logs, error messages or analytics.
- Stack traces or internal errors returned to end users.
- API responses that now include fields the client does not need (password hashes, internal IDs, emails of other users).

## Cryptography
- Home-made crypto, MD5/SHA1 for passwords, unsalted hashes. Passwords should use bcrypt, scrypt or Argon2.
- `Math.random()` / `random` used for tokens or secrets instead of a CSPRNG (`crypto.randomBytes`, `secrets`).
- TLS verification disabled (`verify=False`, `rejectUnauthorized: false`, `InsecureSkipVerify`).

## Web specifics
- State-changing GET requests; missing CSRF protection on cookie-authenticated forms.
- CORS set to `*` with credentials, or reflecting any origin.
- Cookies without `HttpOnly`, `Secure`, `SameSite` where they carry sessions.
- Open redirects from a `next` / `redirect` parameter.
- File uploads without type and size limits, or stored under a user-chosen name in a served directory.

## Infrastructure and dependencies
- New dependency that is unmaintained, typo-squatted, or pulled from an unpinned URL.
- Dockerfile running as root unnecessarily, or copying secrets into the image.
- IAM / permission changes that widen access (`*` actions or resources).
- CI changes that expose secrets to pull requests from forks.

## Concurrency and money
- Check-then-act races on balances, inventory, or unique constraints (use transactions or atomic updates).
- Floating point used for currency.
- Idempotency missing on payment or webhook handlers that can be retried.
