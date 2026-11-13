# Pre-Trade Compliance Checker

A multi-tenant web service that checks a proposed securities order against an investment firm's mandate rules before the order is sent to a broker, and returns ALLOW, WARN or BLOCK together with the exact reasons and a tamper-evident record of the decision.

Coursework project for "Programų sistemų projektavimas", VILNIUS TECH. Design: [docs/design-document.en.md](docs/design-document.en.md).

## Prerequisites

- Docker Desktop
- Java 21 (only for running tests locally)

## Run

```
docker compose up --build
```

Then check `http://localhost:8080/actuator/health`, which reports `"status":"UP"`.

## Tests and static analysis

```
./mvnw verify
```

Docker must be running: Testcontainers starts PostgreSQL. Checkstyle and SpotBugs run in the same command.

## Database roles

| Role | Used by | Rights |
|---|---|---|
| `psp_admin` | container's `POSTGRES_USER`; init and manual admin only | superuser |
| `psp_owner` | Flyway migrations | owns schema `psp` and its tables; not superuser |
| `psp_app` | the running application | `SELECT/INSERT/UPDATE/DELETE` only; not superuser, not owner, no `BYPASSRLS` |

The app does not connect as owner or superuser because both bypass PostgreSQL row-level security, which is the second barrier for tenant isolation.

## Demo scenario

TODO

## Architecture

TODO

## Tenants and roles

TODO
