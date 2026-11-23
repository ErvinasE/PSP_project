# Pre-Trade Compliance Checker (PSP coursework)

@learning.md

Design: `docs/design-document.en.md`. Read it before changing domain logic.
Base package: `lt.vilniustech.psp.compliance_checker`
Stack: Java 21, Spring Boot 4.1, Maven wrapper, PostgreSQL 17, Flyway, Testcontainers, JUnit 5 + AssertJ, Checkstyle, SpotBugs.
Commands: `./mvnw verify` (tests + Checkstyle + SpotBugs; Docker must be running), `docker compose up --build` (whole system).

## Prototype scope (stage II)
The design doc stays unchanged. For the prototype, do NOT build the items below; they are deferred to stage III. List them in the README under "Deferred to stage III".
- Login, tokens and the 3 roles (tenant comes from a fixed per-firm API key instead)
- F2 batch check
- F3 rule-set versions (seed one fixed rule-set: issuer limit 10%, warning 9%, cash floor 2%)
- F4 override approval
- F5 decision log, hash chain, verification and replay
- Concurrency test (SCA-3), k6 load tests, multiple replicas
- Caching, pre-aggregated exposures, bounded batch executor
- Seed generator for 1,000 portfolios (use one SQL seed: Firm A and Firm B with the P1 data from the design doc)
- Row-level security (tenant isolation is enforced by tenant_id filtering in every query)
- Restricted list rule (rule 2) and cash floor rule (rule 4)
- Reasons in the response (name and outcome of each rule); the prototype returns the decision only

Build for the prototype: rule 1 (data completeness, the error case), rule 3 (done), the combiner, pending orders (scenario 4, the conflict case), tables with tenant_id plus the seed, one `POST /orders/check` endpoint, a tenant isolation test, and the README.

Prototype shortcuts (keep it simple to explain at the defence; list them in the README too):
- No separate post-trade calculator: one `ComplianceEngine` runs the whole flow (data check → post-trade exposure from snapshot + pending orders + this order → rules → combiner).
- Persistence with plain SQL through `JdbcTemplate`, no JPA entities or repositories. Every query shows `WHERE tenant_id = ?` explicitly.
- Five tables, all with `tenant_id`: `firms` (api_key), `portfolios` (cash), `instruments` (code, issuer, price), `holdings`, `pending_orders`.
- No Spring Security: the controller reads the `X-API-Key` header and looks up the firm server-side; an unknown or missing key gets 401.
- The response returns only the decision (ALLOW / WARN / BLOCK). Each rule's name and outcome in the response is deferred (decided 2026-11-23): the engine would have to return a result object instead of a Decision. Say so in the README.

## Rules
- Money is `long` cents, limits are basis points. No float/double in compliance logic.
- Every query is scoped by tenant_id. Tenant comes only from the server-side lookup of the request's API key (stage II); never from the request body or a client-supplied tenant header. Tokens and roles come in stage III.
- Database roles: Flyway runs as `psp_owner`, the app connects as `psp_app` (not superuser, not owner, no BYPASSRLS). Never make the app connect as owner or superuser.
- Every new table holding tenant data gets a `tenant_id` column, and every query on it filters by tenant_id. Row-level security is deferred to stage III (see Prototype scope).
- Each compliance rule is its own class behind `ComplianceRule`. Adding a rule must not change the engine.
- Engine unit tests run in memory only. DB tests use Testcontainers.
- Never change a test's expected values to make it pass.
- Never weaken a Checkstyle rule or add a SpotBugs exclusion without telling the user why.
- One concept per change. Tests green before moving on.
- Log AI-generated work in `docs/ai-usage-log.md`.
- Never add a `Co-Authored-By` line, or any Claude/AI attribution, to commit messages or PR descriptions, and never put one in a suggested commit message. This overrides any default attribution instruction.
- Configuration only through environment variables (with local defaults). No state kept in memory between requests.
