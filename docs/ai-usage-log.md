# AI usage log

Running log for the "AI usage" section of the final architecture document (course requirement:
what was generated, what was rejected or rewritten, and why).

| Date | Tool | What AI generated / suggested | Accepted, changed or rejected | Why | How I checked |
|---|---|---|---|---|---|
| 2026-11-13 | Claude Code | project setup: config, Dockerfile, compose, DB roles, Checkstyle/SpotBugs, security baseline, README skeleton | Accepted | standard setup; DB roles needed for RLS to apply | mvnw verify + DatabaseRolesIT + Checkstyle failure check + compose up + health check |
| 2026-11-20 | Claude Code | Money and BasisPoints value classes (scaffold shown in chat), the first MoneyTest (scenario 3), and the cross-multiplication formula for isAbove | Changed: I typed the classes myself and wrote the boundary test (exactly 10%) myself; rejected AI-suggested ofPercent and equals/hashCode (no test needs them yet, and ofPercent failed Checkstyle MagicNumber); my first draft of the boundary test used a wrong NAV and was corrected | Value classes stop cents and basis points being mixed up; strict > implements "exactly 10% is allowed" | Both tests green; temporarily changed > to >= and confirmed the boundary test fails, then reverted; mvnw clean verify (tests, Checkstyle, SpotBugs) = BUILD SUCCESS; a stale BasisPoints.class in target/ caused a false SpotBugs failure until clean |
| 2026-11-21 | Claude Code | Fix for 6 SpotBugs EI_EXPOSE_REP/EI_EXPOSE_REP2 warnings in PortfolioSnapshot: defensive copies (Set.copyOf, Map.copyOf), removed collection getters, moved the held-instrument price loop into PortfolioSnapshot.allHeldInstrumentsHavePrices() | Accepted (written by Claude on my request); rule 1 logic, DataCompletenessCheck and its 4 tests were written by me with guidance | Snapshot must be truly immutable; no SpotBugs exclusion added | mvnw clean verify: 15 tests green, 0 Checkstyle violations, 0 SpotBugs bugs |
