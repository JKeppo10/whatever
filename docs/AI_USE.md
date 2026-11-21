# AI Use Log

Required by spec §13. Add an entry whenever an AI tool meaningfully contributes to the project.

| Date | Member | Tool | What it was used for | Artifact(s) affected | How we verified it | What we changed |
|---|---|---|---|---|---|---|
| 2026-10-01 | Abdelrahman | Claude Code | Repo setup, README, project timeline draft | README.md, docs/TIMELINE.md, .gitignore | Team reviewed it at kickoff | |
| 2026-10-14 | Abdelrahman | Claude Code | First draft of the Machines module M1 design: business rules, state model, UC2/3/6/7/12, tables, endpoints, architecture write-up, event list (Google Doc) | M1 design doc, Machines sections | _To fill: reviewed by Abdelrahman + team on Oct 15_ | _To fill_ |
| 2026-11-04 | Ringo | Claude Code | F1 project skeleton and F5 event infrastructure: Spring Boot app (start.spring.io), module packages, dev/test profiles, health endpoint, `EventPublisher` with in-process version, empty event records, empty shared interfaces | backend/ | `./mvnw test` passes (health and event-publisher tests); `/actuator/health` returns UP on a local run | |
| 2026-11-18 | Ringo | Claude Code | F4 security foundation: Spring Security config (session, CSRF header, CORS, role rules), 401/403 in the shared error body, `CurrentUser` implementation, security tests; fixed `GlobalExceptionHandlerTest` for Boot 4 and security | backend/ (identity/config, identity/api, identity/service, application.yml, pom.xml, shared test) | `./mvnw test`: 19 tests pass; test log checked for passwords and session ids (none) | |
