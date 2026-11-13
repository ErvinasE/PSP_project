# AI usage log

Running log for the "AI usage" section of the final architecture document (course requirement:
what was generated, what was rejected or rewritten, and why).

| Date | Tool | What AI generated / suggested | Accepted, changed or rejected | Why | How I checked |
|---|---|---|---|---|---|
| 2026-11-13 | Claude Code | project setup: config, Dockerfile, compose, DB roles, Checkstyle/SpotBugs, security baseline, README skeleton | Accepted | standard setup; DB roles needed for RLS to apply | mvnw verify + DatabaseRolesIT + Checkstyle failure check + compose up + health check |
