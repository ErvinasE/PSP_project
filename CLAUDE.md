# Pre-Trade Compliance Checker (PSP coursework)

@learning.md

Design: `docs/design-document.en.md`. Read it before changing domain logic.
Base package: `lt.vilniustech.psp.compliance_checker`
Stack: Java 21, Spring Boot 4.1, Maven wrapper, PostgreSQL 17, Flyway, Testcontainers, JUnit 5 + AssertJ, Checkstyle, SpotBugs.
Commands: `./mvnw verify` (tests + Checkstyle + SpotBugs; Docker must be running), `docker compose up --build` (whole system).

## Rules
- Money is `long` cents, limits are basis points. No float/double in compliance logic.
- Every query is scoped by tenant_id. Tenant and role come only from the token.
- Database roles: Flyway runs as `psp_owner`, the app connects as `psp_app` (not superuser, not owner, no BYPASSRLS). Never make the app connect as owner or superuser.
- Every new table holding tenant data gets, in the same migration: a `tenant_id` column, `ENABLE ROW LEVEL SECURITY`, `FORCE ROW LEVEL SECURITY`, and a tenant policy.
- Each compliance rule is its own class behind `ComplianceRule`. Adding a rule must not change the engine.
- Engine unit tests run in memory only. DB tests use Testcontainers.
- Never change a test's expected values to make it pass.
- Never weaken a Checkstyle rule or add a SpotBugs exclusion without telling the user why.
- One concept per change. Tests green before moving on.
- Log AI-generated work in `docs/ai-usage-log.md`.
- Never add a `Co-Authored-By` line, or any Claude/AI attribution, to commit messages or PR descriptions, and never put one in a suggested commit message. This overrides any default attribution instruction.
- Configuration only through environment variables (with local defaults). No state kept in memory between requests.
