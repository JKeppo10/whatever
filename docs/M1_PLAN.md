# Milestone 1 Plan: Requirements & Architecture

**Internal deadline:** Fri Oct 16, 2026 (internal review Wed Oct 14)
**Official due date:** _check eClass_ · **Template:** _get the "Milestone 1 / Design Document instructions and template" from eClass and follow its section order_

## Ownership (vertical by domain)

Each person owns the **whole design** for their domain: use cases, functional requirements, tables, endpoints and the sequence diagram. Each person also owns **one cross-cutting section**.

| Member | Domain (use cases) | Tables they design | Cross-cutting section |
|---|---|---|---|
| **A**:| UC1 Registration/Sign-in, UC11 Administration | users, roles, sessions/tokens | System description & glossary · Security considerations · Project plan & responsibilities · **Final doc assembly** |
| **B** | UC2 Browse/Monitor, UC3 Search/Select, UC6 Usage, UC7 Monitoring, UC12 Maintenance | machines, machine_status_log, usage_sessions, maintenance_records | **Architecture** & component/service decomposition · Business-events list · Machine state diagram |
| **C** | UC4 Reserve, UC5 Payment, UC9 Loyalty/Offers, UC13 Facility & Financial | reservations, payments, loyalty_accounts/transactions, offers, facility | **Database design** owner: master ERD, keys, constraints, indexes · Transactions & concurrency section · Reservation state diagram |
| **D** | UC8 Notifications, UC10 Reviews, UC14 Analytics, UC15 AI feature | notifications, reviews, review_responses, (analytics views) | **REST API design** owner: conventions, error format, consolidated API table · **Non-functional requirements** |

B has the most use cases, but UC2, UC3, UC6 and UC7 overlap heavily. C's domain holds the hardest design problem: concurrency.

## Steps & schedule

### Step 0: Kickoff (Thu Oct 1 – Sun Oct 4) · _everyone_
- [ ] Download the M1 template and confirm the official due date.
- [ ] Decide the stack (backend, frontend, DB, broker for M3, cloud) and write down **why**, because the doc must justify it.
- [ ] Decide auth: server-side session vs. JWT. Either works, but you must justify the choice.
- [ ] Pick the tools: where the doc lives (Google Doc vs. Markdown in `docs/`) and how diagrams get drawn. Recommendation: **PlantUML or Mermaid** files in `docs/diagrams/`, so diagrams are versioned and reviewable in PRs.
- [ ] Agree on the use-case template (below) and naming conventions (snake_case tables, plural resource URLs).
- [ ] Confirm ownership table above. Everyone joins the GitHub repo and Linear.

### Step 1: Use cases + functional requirements (Mon Oct 5 – Wed Oct 7) · _each owner_
- [ ] Write every owned use case in the agreed template.
- [ ] Derive numbered functional requirements from them (`FR-BOOK-01`, …).
- [ ] **A:** system description, scope, assumptions (simulated machines and payments), glossary, actors.
- [ ] **D:** draft the NFRs. Make them *measurable*, e.g. "availability API p95 < 300 ms at 100 concurrent users", not "fast".
- [ ] **Wed Oct 7 sync:** round-robin review (A→B→C→D→A). Look for missing alternate and error flows.

**Use-case template:** ID & name · primary actor · goal · preconditions · trigger · main success flow (numbered) · alternate flows · exception/error flows · postconditions · business rules · related FRs

### Step 2: Data + API + architecture (Thu Oct 8 – Sun Oct 11)
- [ ] **Each owner:** list the entities and attributes their use cases need, plus their endpoints (method, URL, params, body, response, status codes, required role).
- [ ] **C:** merge everyone's entities into one **ERD**. Define PKs and FKs, NOT NULL/UNIQUE/CHECK constraints and indexes, with justification. Write the section on how double-booking is prevented, plus other transactional operations (reserve + pay, loyalty redemption).
- [ ] **D:** publish API conventions **on Thu**, before others write endpoints: base path, pagination, error JSON shape, auth header, status-code rules. Then consolidate everyone's endpoints into one API table.
- [ ] **B:** layered architecture diagram, module/service decomposition (M2 modular monolith → M3 services), how modules talk to each other (REST vs. events), business-events table (event, producer, consumers, payload).
- [ ] **A:** threat table covering at least: plaintext passwords, injection, broken access control, XSS, CSRF (if using sessions), sensitive data in logs, payment data, brute-force login. For each, give the mitigation.

### Step 3: UML (Mon Oct 12 – Tue Oct 13)
- [ ] Use-case diagram (all actors, all 15 UCs): **A**
- [ ] Domain/class diagram: **C** (matches the ERD)
- [ ] Component/deployment diagram: **B**
- [ ] Sequence diagrams, one per owner: A: login + protected request · B: machine start → complete (simulated) · C: reserve + pay, incl. conflict/failure · D: review submitted → notification / AI analysis
- [ ] State-machine diagrams: Machine (B) and Reservation (C)

### Step 4: Integrate & review (Wed Oct 14 – Fri Oct 16)
- [ ] **A:** assemble everything into the template. Write the project plan (link `TIMELINE.md`) and the responsibilities table.
- [ ] **Everyone, Wed Oct 14:** full read-through with a **traceability check**. Every UC maps to FRs, every FR maps to endpoint(s) and table(s), and every endpoint has a role.
- [ ] Thu: fix gaps, unify terminology and diagram style.
- [ ] Fri: final proofread and **submit**. Add an entry to `docs/AI_USE.md`.

## Quality bar (what graders look for)

- Every use case has **error/alternate flows**, e.g. machine already reserved, payment declined, machine fails mid-cycle, session expired.
- NFRs are **measurable and testable**.
- Every design choice says **why**, including the alternatives you rejected.
- The ERD has **constraints and indexes**, not just boxes and lines.
- The API uses **resource nouns, correct HTTP methods and status codes**, plus a standard error body.
- The same names appear everywhere: use case ↔ FR ↔ endpoint ↔ table ↔ diagram.
