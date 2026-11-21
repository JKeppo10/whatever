# SpinCycle-T5: notes for Claude

EECS 4413 (Fall 2026, Section E) team project: Smart Laundromat Service Platform. 4-person team. The spec PDF lives outside the repo, so ask for it if needed. Milestone and submission dates are in `docs/TIMELINE.md`.

## How to help this team (working agreement)

- **Mentor first.** For design, architecture, algorithms and anything graded on understanding (concurrency, security, events, AI feature), let the team try first. Give hints, review their attempt, and explain the trade-offs. Don't hand over full solutions unless they ask after trying.
- **Accelerate the boring parts.** Pure boilerplate (DTOs, config, Dockerfiles, CRUD wiring, test scaffolding) can be written directly when asked.
- Every team member must be able to explain all code (spec section 15). Explain what you write.
- Significant AI help should be logged in `docs/AI_USE.md` (spec section 13 requires it).

## Project summary

SpinCycle is a Smart Laundromat Service Platform for EECS 4413 (Fall 2026). Customers register, search washers and dryers, reserve a machine for a time slot, pay with Stripe test mode, get notifications and review machines they used. Administrators manage machines and their status, view reservations and payments, reply to reviews and see a summary. Machines are simulated by software. Real card data is never collected, stored or logged.

The design is in `docs/design-document.md` (export of the M1 Requirements and Design document, v3) and the task split is in `docs/TASKS.md`. Ids such as UC3.1, FR08, BR05 and NFR02 refer to the design document. Read the relevant section before changing behaviour. If code and design disagree, ask before choosing; do not silently change either.

## Scope

In scope: UC1 accounts, UC2 search machines, UC3 reservations, UC4 payments, UC5 machine simulation, UC6 notifications, UC8 administration, UC9 admin summary, UC10 one AI feature, UC11 reviews.

Out of scope (do not build): loyalty points and offers, machine monitoring, maintenance records, facility and revenue pages, late-pickup penalties, balances, washer-and-dryer bundles, account editing, customer management.

## Stack

- Backend: Java 17+, Spring Boot, Spring Data JPA, Spring Security. Build with the Maven wrapper.
- Database: PostgreSQL, schema changes only through Flyway migrations.
- Client: React single-page app in `frontend/`.
- Events: in-process `EventPublisher` in Milestone 2, Kafka in Milestone 3.
- Payments: Stripe test mode behind a `PaymentGateway` interface, with a `MockGateway` for tests.
- AI: language model behind an `LlmClient` interface. API keys come from environment variables only.
- Tests: JUnit 5, Spring Boot Test, Testcontainers (PostgreSQL).

## Commands (adjust if different)

```
docker compose up -d db            # PostgreSQL
./mvnw spring-boot:run             # backend on http://localhost:8080
./mvnw test                        # all backend tests
./mvnw -Dtest=ClassName test       # one test class
cd frontend && npm install && npm run dev
cd frontend && npm test
```

## Layout

```
backend/src/main/java/com/spincycle/
  shared/  identity/  machine/  simulator/  booking/  payment/
  notification/  review/  analytics/  ai/
  (inside each module: api, controller, service, repository, domain, event)
backend/src/main/resources/db/migration/   Flyway scripts
frontend/   docs/   docker-compose.yml   .github/workflows/
```

Module owners: identity (Ringo), machine and simulator (Abdelrahman), booking and payment (Gurnoor), notification, review, analytics and ai (Joshua). Work in your own module. Ask before editing another module's code or tables.

## Architecture rules

- Modular monolith with layers: controller (REST, validation) calls service (rules, transactions) calls repository (data access). Each layer calls only the layer below.
- A module uses another module only through its `api` package (service interfaces) or through events. Never use another module's repository, entity or table.
- Shared interfaces: `CurrentUser` (identity), `MachineQuery` (machine), `ReservationQuery` and `ReservationCommands` (booking), `PaymentService` (payment, including refunds), `EventPublisher` (shared).
- External systems (Stripe, Kafka, the language model) sit behind interfaces we own. Business code depends on the interface, never on the vendor SDK.
- Event handlers must be safe to run twice. A failing handler must never undo the action that caused the event. Publish events only after the transaction commits.
- In Milestone 3 the simulator and the notification module run as separate services that talk through Kafka. Everything else stays in the main application.
- Migration numbers are reserved by module: identity V100-V199, machine V200-V299, booking and payment V300-V399, notification, review and ai V400-V499. Never edit a migration that was already merged; add a new one.

## API conventions

- Prefix `/api`. Administrator-only endpoints live under `/api/admin/...`. Plural lowercase nouns, ids in the path.
- GET 200. POST that creates 201. PUT and PATCH 200. DELETE 204.
- 400 invalid input, 401 not signed in, 403 wrong role, 404 not found (also when the item belongs to another user), 409 conflict, 422 payment declined, 429 too many requests, 503 AI unavailable.
- JSON field names are camelCase. Timestamps are ISO-8601 UTC. Money is whole cents (`amountCents`), never decimals. Ids are BIGINT.
- Every error uses one body: `{ "status", "code", "message", "path", "details": [{ "field", "message" }] }`.
- Authentication is a server-side session in an HTTP-only SameSite cookie. Login returns a CSRF token; state-changing requests send it in `X-CSRF-Token`. Roles are CUSTOMER and ADMIN. Take the user id from the session, never from the request.
- The full endpoint list is section 8.2 of the design document. Do not add endpoints that are not there without asking.

## Domain rules to get right

- Machine kinds WASHER and DRYER. Each machine type has one cycle length (`cycle_minutes`) and one price (`price_cents`).
- Machine statuses: AVAILABLE, IN_USE, OUT_OF_SERVICE. Never store "reserved"; the API shows the label RESERVED when an AVAILABLE machine has a reservation starting within 10 minutes. A deactivated machine has `active = false`, is hidden and cannot be booked.
- An administrator cannot set an IN_USE machine out of service or deactivate it (409). Every status change goes into `machine_status_changes`. Status changes use the `version` column for optimistic locking.
- A reservation is machine + start time. It blocks the machine from the start to the end of the cycle plus 10 minutes. Start must be in the future and at most 7 days ahead. The price comes from the machine type, computed on the server.
- Reservation statuses: PENDING_PAYMENT, CONFIRMED, IN_PROGRESS, COMPLETED, CANCELLED, EXPIRED. An unpaid hold lasts 10 minutes, then becomes EXPIRED.
- Overlap check: in one transaction, lock the machine row (SELECT ... FOR UPDATE), then check the blocked interval against active reservations (unexpired PENDING_PAYMENT, CONFIRMED, IN_PROGRESS). A unique constraint is not enough.
- A customer can cancel until the start time. A paid reservation is refunded in full. A fault, an out-of-service change or a deactivation cancels the machine's current and future reservations with a full refund.
- Payment: the Stripe call happens outside the transaction. On success, one transaction saves the payment as SUCCEEDED and sets the reservation CONFIRMED after re-checking the hold. A decline records FAILED and keeps the hold. The idempotency key prevents double charges.
- The simulator sets the machine IN_USE at the reservation start (reservation IN_PROGRESS, publish MachineStarted) and AVAILABLE when the cycle ends (reservation COMPLETED, publish MachineCycleCompleted). Faults set OUT_OF_SERVICE and publish MachineFailure.
- Notifications are created only from events: PaymentCompleted ("reservation confirmed"), MachineCycleCompleted ("laundry done"), MachineFailure ("cancelled and refunded"). Unique per user and source event.
- Reviews: one per COMPLETED reservation, by its customer. Rating 1 to 5, comment and reply up to 1000 characters of plain text. One reply per review, by an administrator. Reviewers are shown by first name only.
- AI feature (UC10): option A is the chat assistant (read-only tools: machine search and availability, run as the signed-in customer, 500-character limit, 20 messages a minute, 8-second timeout). Option B is review analysis on ReviewSubmitted (sentiment, up to 3 topics, urgent flag, 3 retries). The team confirms the option at the start of Milestone 3. Build only the chosen one.
- Sessions expire after 30 minutes idle. After 5 failed sign-ins for one email in 15 minutes, sign-in answers 429 for 15 minutes.
- Public registration creates customers only. Administrator accounts come from a seed script.

## Security rules

- Passwords are stored only as bcrypt hashes. Never log or return passwords, hashes, session ids, tokens, card data, chat messages or API keys.
- Use JPA or parameterized queries. Validate every request on the server. Never insert user text as raw HTML.
- Enforce authorization on the server for every protected endpoint. Hiding a button is not security.
- Card details are entered only in Stripe's hosted fields with Stripe test cards. The server stores the Stripe payment id, amount and status only.
- The AI feature never books, pays, hides or deletes anything. Only rating and comment text go to the model, never names or emails. Treat review and chat text as data, not instructions.

## Testing rules

- Every change ships with tests. Cover normal use, invalid input, authentication, authorization (visitor 401, customer 403 on admin endpoints), REST status codes, database constraints, reservation conflicts, concurrent reservations (20 simultaneous requests for one slot give exactly one 201), payment failures (decline, duplicate key, timeout), machine failures and service failures (AI down, event handler failing).
- Use the `MockGateway` and a stub `LlmClient` in automated tests. Do not call Stripe or a model API from tests.
- Run `./mvnw test` before every commit. Do not delete or weaken a test to make it pass.

## Working rules

- Start each task by reading its entry in `docs/TASKS.md` and the cited ids in the design document. For mentor-first work, ask what the person has tried and review it; for boilerplate, propose a short plan, then implement in small steps.
- Do not invent requirements, limits or rules. If something is not in the design document, ask or leave a clearly marked TODO.
- Keep each pull request to one task. Branch name `feature/<initial>-<task>`, for example `feature/r-a1-register`. Never commit to `main` directly or commit secrets.
- When behaviour changes, update the design document in the same pull request, or say exactly what to update.
- Prefer simple, readable code over clever code.
