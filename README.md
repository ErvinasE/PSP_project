# Pre-Trade Compliance Checker

A multi-tenant web service that checks a proposed securities order against an investment firm's mandate rules before the order is sent to a broker, and returns ALLOW, WARN or BLOCK together with the exact reasons and a tamper-evident record of the decision.

Coursework project for "Programų sistemų projektavimas", VILNIUS TECH. Design: [docs/design-document.en.md](docs/design-document.en.md).

## Stage II prototype: what is built

This repository is the stage II prototype. It is a thin vertical slice of the design, not the whole product (see [Deferred to stage III](#deferred-to-stage-iii)).

- One endpoint, `POST /orders/check`, authenticated by a per-firm API key.
- Rule 1, data completeness (an unknown or unpriced instrument is a BLOCK that nothing can override; it fails closed).
- Rule 3, issuer concentration (limit 10%, warning from 9%; exactly on the limit is a WARN, anything above it, even by one share, is a BLOCK).
- The decision combiner: any BLOCK means BLOCK, otherwise any WARN means WARN, otherwise ALLOW.
- Pending orders: an accepted (ALLOW or WARN) order is stored, and counts towards the limit of the next order (design scenario 4).
- Multi-tenancy: every table has `tenant_id`, and every query filters by it.
- Seed data for two firms: Firm A with the design document's portfolio P1, and Firm B.

## Prerequisites

- Docker Desktop
- Java 21 (only for running tests locally)

## Run

```
docker compose up --build
```

Then check `http://localhost:8080/actuator/health`, which reports `"status":"UP"`.

Flyway creates the tables (`V1__create_tables.sql`) and inserts the demo data (`V2__seed.sql`) when the API starts. The database is disposable: to go back to the seed data, run `docker compose down -v` and start again.

## Tests and static analysis

```
./mvnw verify
```

Docker must be running: Testcontainers starts PostgreSQL. Checkstyle and SpotBugs run in the same command.

- Domain tests (rules, engine, combiner, money) run in memory, with no database and no Spring.
- Integration tests (repositories, service, HTTP) run against a real PostgreSQL started by Testcontainers, connected as the same `psp_app` role the application uses.

## API

`POST /orders/check` with the header `X-API-Key`.

```json
{ "portfolio_id": "P1", "instrument": "X-EQ", "trade_type": "BUY", "quantity": 10000 }
```

The answer is the decision only:

```json
{ "decision": "WARN" }
```

| Status | When |
|---|---|
| 200 | the order was checked; the body holds `ALLOW`, `WARN` or `BLOCK` (a BLOCK is a normal answer, not an HTTP error) |
| 400 | the body is not valid (a missing field, `quantity` of 0 or less, a `trade_type` that is not `BUY` or `SELL`) |
| 401 | the `X-API-Key` header is missing or no firm has that key |
| 404 | this firm has no such portfolio. A portfolio of another firm gives the same answer, so the API does not reveal which portfolio ids exist |

The tenant is never sent by the client. It is found from the API key on the server.

## Tenants and API keys

| Firm | `tenant_id` | Demo API key | Portfolio |
|---|---|---|---|
| Firm A | `firm-a` | `demo-key-firm-a` | `P1` |
| Firm B | `firm-b` | `demo-key-firm-b` | `B1` |

Only the SHA-256 hash of a key is stored (`firms.api_key_hash`), so a copy of the table cannot be used to log in. The demo keys above are public on purpose; real keys would be long random strings given to each firm privately.

There are no user accounts and no roles in the prototype.

## Demo scenario

Run these in order on a fresh database (`docker compose down -v`, then `docker compose up --build`). The commands are written for a Bash shell (on Windows, Git Bash); PowerShell quotes JSON differently.

Portfolio P1 (Firm A) is worth EUR 100,000,000, and EUR 8,500,000 of that (8.50%) is one issuer, X (60,000 X-EQ shares at EUR 100 plus 2,500 X-BOND bonds at EUR 1,000).

```
# 1. BUY 10,000 X-EQ (EUR 1,000,000): X would be 9.50%, which is above the 9% warning
curl -s -X POST localhost:8080/orders/check -H "X-API-Key: demo-key-firm-a" -H "Content-Type: application/json" \
  -d '{"portfolio_id":"P1","instrument":"X-EQ","trade_type":"BUY","quantity":10000}'
# {"decision":"WARN"}   the order is accepted and stored as pending

# 2. The identical order: 8.5M owned + 1M pending + 1M new = 10.50%, above the 10% limit
curl -s -X POST localhost:8080/orders/check -H "X-API-Key: demo-key-firm-a" -H "Content-Type: application/json" \
  -d '{"portfolio_id":"P1","instrument":"X-EQ","trade_type":"BUY","quantity":10000}'
# {"decision":"BLOCK"}  alone it would have been a WARN; the pending order makes the difference

# 3. An instrument that is not in the firm's reference data: BLOCK, no rule runs
curl -s -X POST localhost:8080/orders/check -H "X-API-Key: demo-key-firm-a" -H "Content-Type: application/json" \
  -d '{"portfolio_id":"P1","instrument":"ZZ-EQ","trade_type":"BUY","quantity":100}'
# {"decision":"BLOCK"}

# 4. No API key: 401
curl -s -o /dev/null -w "%{http_code}\n" -X POST localhost:8080/orders/check -H "Content-Type: application/json" \
  -d '{"portfolio_id":"P1","instrument":"X-EQ","trade_type":"BUY","quantity":100}'
# 401

# 5. Firm B asks for Firm A's portfolio: 404, the same as for a portfolio that does not exist
curl -s -o /dev/null -w "%{http_code}\n" -X POST localhost:8080/orders/check -H "X-API-Key: demo-key-firm-b" -H "Content-Type: application/json" \
  -d '{"portfolio_id":"P1","instrument":"X-EQ","trade_type":"BUY","quantity":100}'
# 404
```

The same flows are automated tests: `OrderCheckControllerIntegrationTest` (steps 1 to 2, 4 and 5), `ComplianceEngineTest` (the design scenarios 1 to 4 and 7).

## Architecture

```
HTTP request
  → api          OrderCheckController   reads the API key, turns the service's answer into a status code
  → service      OrderCheckService      loads the portfolio and pending orders, asks the engine, saves accepted orders
  → domain       ComplianceEngine       data check → exposure after the order → rules → combiner (no Spring, no SQL)
  → persistence  *Repository            plain SQL through JdbcTemplate, every query filtered by tenant_id
  → PostgreSQL
```

| Package | Responsibility |
|---|---|
| `api` | HTTP only: request and response shapes, status codes. No business rules |
| `service` | One order check from start to finish. No HTTP, no rules |
| `domain` | The compliance logic. Knows nothing about Spring or the database, so its tests run in memory |
| `persistence` | Reads and writes the five tables. Builds the domain's `PortfolioSnapshot` |
| `auth` | Hashes API keys |
| `config` | Builds the `ComplianceEngine` with the fixed prototype rule-set (issuer limit 10%, warning 9%) |

Main design choices:

- **Strategy + Open/Closed:** each rule is its own class behind `ComplianceRule`. A new rule is added to the list in `ComplianceEngineConfig`; the engine class does not change.
- **Integer money:** money is `long` cents and limits are basis points, and the limit check is done by cross-multiplication, never by division. So exactly 10.00% and 10.0001% are told apart exactly (design scenarios 2 and 3).
- **Stateless application:** nothing is kept in memory between requests. Pending orders live in the database.
- **Constructor injection:** classes receive their collaborators in the constructor, so each layer can be tested on its own.
- **Tenant isolation, two levels:** every query has `WHERE tenant_id = ?`, and the composite foreign keys (`tenant_id` plus the id) make it impossible to link rows of different firms. Row-level security is the planned third level.

## Database roles

| Role | Used by | Rights |
|---|---|---|
| `psp_admin` | container's `POSTGRES_USER`; init and manual admin only | superuser |
| `psp_owner` | Flyway migrations | owns schema `psp` and its tables; not superuser |
| `psp_app` | the running application | `SELECT/INSERT/UPDATE/DELETE` only; not superuser, not owner, no `BYPASSRLS` |

The app does not connect as owner or superuser because both bypass PostgreSQL row-level security, which is the second barrier for tenant isolation.

## Prototype shortcuts

Simplifications made on purpose, to keep the prototype explainable:

- **One engine class, no separate post-trade calculator.** `ComplianceEngine` runs the whole flow.
- **Plain SQL through `JdbcTemplate`**, no JPA entities or repositories. Every query shows its `tenant_id` filter.
- **Five tables**, all with `tenant_id`: `firms`, `portfolios`, `instruments`, `holdings`, `pending_orders`.
- **No Spring Security.** The controller reads `X-API-Key` and looks the firm up. Spring would have refused every request without authentication by default, and that safety net is gone: each endpoint must check the key itself. A request with an invalid body and no key gets 400, not 401, because Spring reads the body before the controller runs.
- **The response is the decision only.** It does not say which rule caused a BLOCK.
- **The seed is a normal migration**, so the demo keys and data would also be created in a real environment.
- **Prices come from the database** (`instruments.price_cents`); there is no market data feed.

## Known limitations

- **Two identical orders sent at the same instant can both be accepted.** Reading the pending orders and saving the new one is not one atomic step, and there is no per-portfolio lock. This is the concurrency requirement SCA-3, planned for stage III.
- **A pending order stays pending.** Nothing executes or cancels orders, so pending orders keep counting until they are removed from the database by hand.

## Deferred to stage III

Not built in the prototype, and planned in the design document:

- Login, tokens and the three roles (the tenant comes from a fixed per-firm API key instead)
- F2 batch check
- F3 rule-set versions (the prototype has one fixed rule-set: issuer limit 10%, warning 9%)
- F4 override approval
- F5 decision log, hash chain, verification and replay
- Reasons in the response (the name and outcome of each rule)
- Concurrency test (SCA-3), k6 load tests, multiple replicas
- Caching, pre-aggregated exposures, bounded batch executor
- A seed generator for 1,000 portfolios (the prototype has one SQL seed for two firms)
- Row-level security (tenant isolation is enforced by `tenant_id` filtering in every query)
- Restricted list rule (rule 2) and cash floor rule (rule 4)
