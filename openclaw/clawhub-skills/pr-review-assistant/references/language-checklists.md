# Language and framework checklists

Apply the sections that match the files in the diff. These are common, high-value defects, not style rules.

## Python
- Mutable default arguments (`def f(x=[])`).
- Bare `except:` or `except Exception: pass` hiding real errors.
- Resources opened without `with` (files, locks, DB connections).
- `==` vs `is` confusion (`is` for `None` only).
- Timezone-naive `datetime.now()` / `utcnow()` mixed with aware datetimes.
- Iterating and mutating the same list or dict.
- Blocking I/O (`requests`, `time.sleep`) inside `async def`.
- Django: queries in loops (missing `select_related` / `prefetch_related`), `.raw()` or `extra()` with formatted strings, missing migrations for model changes, `DEBUG=True` paths.
- FastAPI / Flask: missing response models leaking fields, missing dependency for auth on new routes.
- Pandas: chained assignment (`df[a][b] = x`), silent dtype changes, `inplace=True` returning `None`.

## JavaScript / TypeScript
- Missing `await` on a promise, or `forEach` with an async callback (does not wait).
- Unhandled promise rejections; `try/catch` that doesn't cover the awaited call.
- `==` instead of `===` where types can differ.
- TypeScript `any`, non-null assertions (`!`) or `as` casts that hide real nullability.
- Mutating props or state directly.
- React: missing or wrong `useEffect` dependencies, effects without cleanup (listeners, intervals, subscriptions), keys using array index on reorderable lists, state updates after unmount, expensive work on every render.
- Next.js: server-only secrets imported into client components, data fetched on the client that should be server-side, missing `revalidate`/cache settings where freshness matters.
- Node: synchronous fs calls in request handlers, `child_process.exec` with interpolated strings, unbounded `JSON.parse` of request bodies.

## Go
- Errors ignored (`_ =`) or checked but not returned.
- Goroutine leaks: goroutines blocked forever on channels with no cancellation; missing `context` propagation.
- Loop variable capture in goroutines/closures (pre-Go 1.22 semantics if the module's `go` version is older).
- Concurrent map access without a mutex.
- `defer` inside loops (resources held until the function returns).
- Nil pointer dereference on returned structs when `err != nil` paths are skipped.
- `http.Client` without a timeout.

## Java / Kotlin
- Resources not closed (use try-with-resources / `use`).
- `equals` overridden without `hashCode`.
- Catching `Exception`/`Throwable` broadly and continuing.
- Shared mutable state in singletons/Spring beans without synchronisation.
- JPA: N+1 from lazy loading in loops, missing `@Transactional` on multi-step writes, `@Transactional` on private methods (no effect).
- Kotlin: `!!` on values that can be null, `lateinit` accessed before init.

## C#
- `async void` methods other than event handlers; `.Result` / `.Wait()` causing deadlocks.
- `IDisposable` not disposed (`using`).
- EF Core: queries in loops, client-side evaluation of large sets, missing `AsNoTracking` for read-only queries.

## Ruby / Rails
- Mass assignment without strong parameters.
- `where("name = '#{params[:name]}'")` string interpolation in queries.
- N+1 without `includes`.
- Callbacks with hidden side effects (emails, payments) in model saves.
- Missing database index for new `where`/`order` columns or uniqueness validations.

## PHP
- Queries built with string concatenation instead of prepared statements.
- `==` loose comparison in auth or hash checks (use `===` / `hash_equals`).
- Output not escaped in templates.
- `unserialize` on user input.

## Rust
- `unwrap()` / `expect()` on values that can fail at runtime in library or server code.
- `unsafe` blocks without a comment explaining the invariant.
- Holding a `Mutex` guard across `.await`.
- Unbounded channels or collections fed by external input.

## SQL and migrations
- Migration that locks a large table (adding a non-null column with default on older databases, adding an index without `CONCURRENTLY` in Postgres).
- Destructive migrations (drop column/table) without a rollout plan for code still reading the column.
- Missing down migration where the project uses them.
- New query patterns without a supporting index.

## Shell
- Unquoted variables (`$var` instead of `"$var"`).
- Missing `set -euo pipefail` in scripts where failures should stop execution.
- `rm -rf` with a variable that could be empty.

## Infrastructure as code (Terraform, Kubernetes, Docker)
- Public buckets or security groups open to `0.0.0.0/0` on non-public ports.
- Secrets in plain text in manifests or variables.
- Containers running privileged or as root without need; missing resource limits.
- `latest` image tags in production manifests.
