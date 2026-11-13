# project_setup.md: finish setting up the project

Instructions for **Claude Code**. The user has already generated the Spring Boot project with the VS Code Spring Initializr and opened it. Your job is to finish the setup so the repo is ready for development. Don't write any business logic (no compliance engine, no entities, no endpoints). That comes later, guided by `CLAUDE.md` and `learning.md`.

Work through the steps in order. After each step, briefly say what you did. At the end, give the report described in step 13.

> **If an earlier version of this file was already carried out:** apply only the steps marked **★** (they're new or changed), then rerun step 12.

**Context:** Pre-Trade Compliance Checker, a coursework project for the "Programų sistemų projektavimas" course at VILNIUS TECH. The design is in `docs/design-document.en.md`. The machine runs Windows with Docker Desktop. Use `mvnw.cmd` / `./mvnw` as appropriate for your shell.

**What the setup has to support:**
- The system must start with **one command from the README** (`docker compose up`).
- The API must be **stateless**, run in **containers**, and get its **configuration from environment variables**.
- The design doc uses **PostgreSQL row-level security (RLS)** as the second barrier for tenant isolation. That only works if the app connects as a role that is **neither superuser nor table owner** (both bypass RLS).
- The design doc says that AI-generated code is checked with tests **and static analysis (Checkstyle, SpotBugs)**, run by `mvn verify`.

---

## Step 1: Inspect what was generated

1. Read `pom.xml` and confirm the following. Report any mismatch to the user; don't silently "fix" versions.
   - Spring Boot parent is **4.1.x** (not 3.x, not a SNAPSHOT)
   - `java.version` is **21**
   - packaging is jar
2. Note the `groupId` and the base package under `src/main/java/`. Use them in later steps; don't assume a package name.
3. Confirm these dependencies are present: Spring Web MVC, Spring Security, OAuth2 Resource Server, Spring Data JPA, PostgreSQL driver, Flyway, Validation, Actuator, Testcontainers (PostgreSQL), plus the test starters.
   - In Spring Boot 4, Flyway needs `spring-boot-starter-flyway` (not just `flyway-core`). Flyway also needs `flyway-database-postgresql` for PostgreSQL.
   - The Testcontainers 2 artifact is `testcontainers-postgresql`.
   - If something is missing, add the Spring Boot 4 starter for it, and tell the user what you added and why.
4. Look at the Testcontainers test configuration the Initializr generated. It will be **replaced in step 5**, so don't spend time on it now.

## Step 2: Folder structure

Make sure these exist. Create what's missing. Don't overwrite files that already have content.

```
CLAUDE.md
learning.md                      (user provides it; if missing, ask the user, don't invent it)
docs/design-document.en.md       (user provides it; if missing, ask the user)
docs/ai-usage-log.md
docker/db/init/                  (database init scripts, step 4)
config/checkstyle/               (step 9)
config/spotbugs/                 (step 9)
```

Content for `docs/ai-usage-log.md` (create only if it doesn't exist):

```markdown
# AI usage log

Running log for the "AI usage" section of the final architecture document (course requirement:
what was generated, what was rejected or rewritten, and why).

| Date | Tool | What AI generated / suggested | Accepted, changed or rejected | Why | How I checked |
|---|---|---|---|---|---|
```

## Step 3 ★: CLAUDE.md

If `CLAUDE.md` doesn't exist, create it with exactly this content, replacing `<base package>` with the real one from step 1. If it exists, show the user the difference and ask before changing it.

```markdown
# Pre-Trade Compliance Checker (PSP coursework)

@learning.md

Design: `docs/design-document.en.md`. Read it before changing domain logic.
Base package: `<base package>`
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
- Configuration only through environment variables (with local defaults). No state kept in memory between requests.
```

## Step 4 ★: Database roles (required for row-level security)

The official `postgres` image makes `POSTGRES_USER` a **superuser**. Superusers always bypass RLS, and so do table owners unless the table has `FORCE ROW LEVEL SECURITY`. So we use three roles:

| Role | Used by | Rights |
|---|---|---|
| `psp_admin` | the container's `POSTGRES_USER`; only for init and manual admin (e.g. the SEC-3 tamper script later) | superuser |
| `psp_owner` | Flyway migrations | owns schema `psp` and its tables; not superuser |
| `psp_app` | the running application | `SELECT/INSERT/UPDATE/DELETE` on tables, sequence usage; **NOSUPERUSER, NOBYPASSRLS, not owner** |

Create `docker/db/init/01-roles.sql`. It runs only when the database volume is first created.

```sql
-- Local development roles. Passwords are dev-only defaults.
CREATE ROLE psp_owner LOGIN PASSWORD 'psp_owner' NOSUPERUSER NOCREATEDB NOCREATEROLE;
CREATE ROLE psp_app   LOGIN PASSWORD 'psp_app'   NOSUPERUSER NOCREATEDB NOCREATEROLE NOBYPASSRLS;

GRANT CONNECT ON DATABASE psp TO psp_owner, psp_app;

CREATE SCHEMA psp AUTHORIZATION psp_owner;
GRANT USAGE ON SCHEMA psp TO psp_app;

ALTER DEFAULT PRIVILEGES FOR ROLE psp_owner IN SCHEMA psp
    GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO psp_app;
ALTER DEFAULT PRIVILEGES FOR ROLE psp_owner IN SCHEMA psp
    GRANT USAGE, SELECT ON SEQUENCES TO psp_app;

ALTER ROLE psp_owner SET search_path = psp;
ALTER ROLE psp_app   SET search_path = psp;
```

**Note for later:** the decision log is append-only. When its table is created, the same migration should `REVOKE UPDATE, DELETE` on it from `psp_app`. Don't do it now (the table doesn't exist yet), but mention it in the final report.

## Step 5 ★: Testcontainers must use the same roles

If tests connect as the container's default superuser, the tenant-isolation tests would pass even with broken RLS. So:

1. Replace the generated Testcontainers configuration with one that:
   - creates a `PostgreSQLContainer` from `postgres:17` with database name `psp` and username/password `psp_admin`
   - copies `docker/db/init/01-roles.sql` into `/docker-entrypoint-initdb.d/01-roles.sql` (e.g. with `withCopyFileToContainer(MountableFile.forHostPath(...), ...)`)
   - **doesn't use `@ServiceConnection`** for the datasource (it would wire the app to the superuser). Instead, use `@DynamicPropertySource`, or equivalent, to set:
     - `spring.datasource.url` = container JDBC URL + `?currentSchema=psp`, user `psp_app`, password `psp_app`
     - `spring.flyway.url` = same URL, `spring.flyway.user` = `psp_owner`, `spring.flyway.password` = `psp_owner`
   - starts the container once per test run (static / singleton container)
2. Add an integration test `DatabaseRolesIT` (or similar, a Testcontainers test) that proves the setup:
   - the app's datasource connects as `psp_app` (`SELECT current_user`)
   - `psp_app` has `rolsuper = false` and `rolbypassrls = false` in `pg_roles`
   - the Flyway history table in schema `psp` is owned by `psp_owner`

   Test names should describe the behaviour, e.g. `appConnectsAsNonSuperuserWithoutBypassRls`. This test protects the RLS claim in the design doc (SEC-1).

## Step 6 ★: Application configuration

Replace the contents of `src/main/resources/application.properties` with:

```properties
spring.application.name=compliance-checker

# App runtime connection: non-owner, non-superuser role (RLS applies)
spring.datasource.url=${DB_URL:jdbc:postgresql://localhost:5432/psp?currentSchema=psp}
spring.datasource.username=${DB_APP_USER:psp_app}
spring.datasource.password=${DB_APP_PASSWORD:psp_app}

# Migrations run as the schema owner
spring.flyway.enabled=true
spring.flyway.user=${DB_OWNER_USER:psp_owner}
spring.flyway.password=${DB_OWNER_PASSWORD:psp_owner}
spring.flyway.schemas=psp
spring.flyway.default-schema=psp
spring.flyway.create-schemas=false

spring.jpa.hibernate.ddl-auto=validate
spring.jpa.open-in-view=false

management.endpoints.web.exposure.include=health
management.endpoint.health.probes.enabled=true

server.port=${PORT:8080}
```

Why these settings:
- Flyway owns the schema. Hibernate only validates it and never changes it.
- There are two credentials, so RLS applies to the app's connection.
- `open-in-view=false` avoids lazy-loading queries running outside the service layer.
- The health endpoint is needed for the Docker Compose healthcheck.

Create the empty folder `src/main/resources/db/migration/` with a `.gitkeep` file. **Don't create any migrations yet.**

## Step 7: Temporary security config so health checks work

Spring Security locks everything by default, which would block `/actuator/health` from Docker. Add a minimal configuration class in `<base package>.config` named `SecurityConfig`:
- allows unauthenticated `GET /actuator/health/**`
- requires authentication for everything else (deny by default)
- disables the form login and default login page
- is stateless (no HTTP session)
- has a short class comment: "Temporary baseline. Token authentication and role checks are added in the security phase."

Keep it under ~30 lines, using the Spring Security 7 lambda DSL. Don't add JWT configuration yet.

## Step 8 ★: Docker

**Dockerfile** in the root, as a multi-stage build:
- Build stage: `eclipse-temurin:21-jdk`
  - copy the Maven wrapper, `pom.xml` and `config/` first, run `./mvnw -q dependency:go-offline` (dependency layer caching), then copy `src` and run `./mvnw -q -DskipTests -Dcheckstyle.skip -Dspotbugs.skip package` (tests and static analysis run in `verify`, not in the image build)
  - make `mvnw` executable (`chmod +x`), because the repo is created on Windows
- Runtime stage: `eclipse-temurin:21-jre`
  - copy the jar
  - run as a non-root user
  - `EXPOSE 8080`
  - `ENTRYPOINT ["java", "-jar", "/app/app.jar"]`

**`.dockerignore`**, excluding: `target/`, `.git/`, `.vscode/`, `.idea/`, `docs/`, `*.md`.

**`.gitattributes`**: `mvnw text eol=lf`, `*.sh text eol=lf`, `*.sql text eol=lf`.

**`docker-compose.yml`** in the root (create or replace):

```yaml
services:
  db:
    image: postgres:17
    environment:
      POSTGRES_DB: psp
      POSTGRES_USER: psp_admin
      POSTGRES_PASSWORD: psp_admin
    ports:
      - "5432:5432"
    volumes:
      - pgdata:/var/lib/postgresql/data
      - ./docker/db/init:/docker-entrypoint-initdb.d:ro
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U psp_admin -d psp"]
      interval: 5s
      timeout: 3s
      retries: 10

  api:
    build: .
    environment:
      DB_URL: jdbc:postgresql://db:5432/psp?currentSchema=psp
      DB_APP_USER: psp_app
      DB_APP_PASSWORD: psp_app
      DB_OWNER_USER: psp_owner
      DB_OWNER_PASSWORD: psp_owner
    ports:
      - "8080:8080"
    depends_on:
      db:
        condition: service_healthy

volumes:
  pgdata:
```

**If a `pgdata` volume already exists from an earlier run**, the init script won't run. In that case, ask the user before running `docker compose down -v`, because it deletes local database data.

Leave the `api` service on a fixed host port for now. Running multiple replicas (`--scale`, scenario SCA-1) will need a small load balancer in front later. Don't add it now.

## Step 9 ★: Static analysis (Checkstyle + SpotBugs in `mvn verify`)

Add both as Maven plugins. Use the **latest stable versions from Maven Central**, pin them as explicit properties in `pom.xml`, and check they resolve. Don't guess version numbers.

**Checkstyle** (`maven-checkstyle-plugin`, recent `com.puppycrawl.tools:checkstyle` as a plugin dependency):
- goal `check`, bound to the `verify` phase, `failOnViolation=true`, `violationSeverity=warning`, `consoleOutput=true`, main sources only for now
- config file `config/checkstyle/checkstyle.xml`, written by you, small and matching the course's clean-code rules:
  - **Naming:** `TypeName`, `MethodName`, `MemberName`, `ParameterName`, `LocalVariableName`, `ConstantName`, `PackageName`
  - **Size:** `MethodLength` (max 40 lines), `ParameterNumber` (max 5), `FileLength` (max 500)
  - **Complexity:** `CyclomaticComplexity` (max 10), `NestedIfDepth` (max 2)
  - **Clarity:** `MagicNumber` (ignore hashCode methods and annotations; allow -1, 0, 1, 2), `NeedBraces`, `EmptyBlock`, `EmptyCatchBlock`, `MissingSwitchDefault`, `OneStatementPerLine`, `MultipleVariableDeclarations`
  - **Imports:** `AvoidStarImport`, `UnusedImports`, `RedundantImport`
  - **No indentation or whitespace-style rules.** Formatting is handled by the VS Code formatter, and the two would fight.
- In `.vscode/settings.json` (step 10), point the Checkstyle extension at the same file, so the editor and the build agree.

**SpotBugs** (`com.github.spotbugs:spotbugs-maven-plugin`, a version that supports Java 21 bytecode):
- goal `check`, bound to `verify`, `effort=Max`, `threshold=Medium`, `failOnError=true`
- exclusion filter `config/spotbugs/exclude.xml`, **empty** at the start (valid `<FindBugsFilter/>` root). Any future exclusion must be narrow (one class + one bug pattern) and have an XML comment explaining why.

If the generated code (application class, `SecurityConfig`, test config) triggers violations, **fix the code**, don't weaken the rules. If a finding really is a false positive, explain it to the user before excluding it.

## Step 10: Editor settings

Create `.vscode/settings.json` if it doesn't exist (merge if it does):

```json
{
  "editor.formatOnSave": true,
  "java.configuration.updateBuildConfiguration": "automatic",
  "java.saveActions.organizeImports": true,
  "files.eol": "\n",
  "java.checkstyle.configuration": "${workspaceFolder}/config/checkstyle/checkstyle.xml"
}
```

Make sure `.gitignore` contains: `target/`, `.idea/`, `*.iml`, `.env`, `CLAUDE.local.md`. Keep `.vscode/settings.json` tracked: if `.vscode/` is ignored, change it to `.vscode/*` plus `!.vscode/settings.json`.

## Step 11 ★: README.md

Replace the generated `README.md` (if any) with a short skeleton:
- Title and a one-sentence description, taken from section 1 of the design doc.
- **Prerequisites:** Docker Desktop. Java 21 only for running tests locally.
- **Run:** `docker compose up --build`, then the health check `http://localhost:8080/actuator/health` returns `{"status":"UP"}`.
- **Tests and static analysis:** `./mvnw verify` (Docker must be running; Testcontainers starts PostgreSQL; Checkstyle and SpotBugs run in the same command).
- **Database roles:** a 3-row table (`psp_admin` / `psp_owner` / `psp_app`), with one sentence on why the app doesn't connect as owner or superuser.
- Placeholder headings with "TODO", nothing more:
  - "Demo scenario"
  - "Architecture"
  - "Tenants and roles"

## Step 12 ★: Verify

Run these and fix problems until all pass. Each must actually succeed; don't report success based on reading the files.

1. `./mvnw verify`: the build passes. That means the context-load test and `DatabaseRolesIT` run against Testcontainers, and Checkstyle and SpotBugs run with no violations.
2. Sanity check of the static analysis: temporarily add a method with a magic number (e.g. `return x * 37;`) to any class, run `./mvnw verify`, confirm **Checkstyle fails the build**, then remove it. Report that it failed as expected.
3. `docker compose up --build -d`: both containers start, and `db` becomes healthy.
4. `GET http://localhost:8080/actuator/health` returns `UP`.
5. `GET http://localhost:8080/` returns **401** (proves deny-by-default works).
6. In the running `db` container, as `psp_admin`, check that `psp_app` has `rolsuper=f` and `rolbypassrls=f`, and that `flyway_schema_history` in schema `psp` is owned by `psp_owner`.
7. `docker compose down` (keep the volume).

If something fails because of the environment (e.g. Docker Desktop isn't running), stop and tell the user what to do; don't work around it.

## Step 13: Commit and report

1. Check `git config user.name` and `user.email`. If they're not set, **ask the user** for their name and email; don't guess.
2. Stage everything and commit with the message:
   `chore: project setup (config, Docker, DB roles for RLS, static analysis, security baseline, docs)`
   Don't push unless the user asks.
3. Add the first row to `docs/ai-usage-log.md`: today's date, Claude Code, "project setup: config, Dockerfile, compose, DB roles, Checkstyle/SpotBugs, security baseline, README skeleton", accepted, "standard setup; DB roles needed for RLS to apply", "mvnw verify + DatabaseRolesIT + Checkstyle failure check + compose up + health check".
4. Report to the user:
   - what was created or changed (one line per file)
   - anything that didn't match expectations in step 1
   - the plugin versions you pinned
   - the results of the 7 checks in step 12
   - the reminder about revoking `UPDATE/DELETE` on the decision log table later (step 4)
   - **"What to know for the defence"**: 4–6 short bullets on the setup decisions a lecturer might ask about. Include:
     - why the app connects as a non-owner, non-superuser role
     - why Flyway owns the schema
     - why config comes from environment variables and the API is stateless
     - why Checkstyle and SpotBugs run in `verify`
     - why the security baseline is deny-by-default

## Don't do

- Don't create entities, repositories, migrations, endpoints or the compliance engine.
- Don't add libraries or plugins beyond what the steps above need. Checkstyle and SpotBugs are the only plugins to add; no Lombok, no MapStruct.
- Don't change the Spring Boot or Java version.
- Don't let the app connect as `psp_admin` or `psp_owner`, in any environment, including tests.
- Don't run `docker compose down -v` without asking the user.
- Don't push to GitHub.