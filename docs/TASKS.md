# SpinCycle: tasks by person

Source: `docs/design-document.md` (M1 v3). Ids like UC3.1, FR08 and BR05 refer to it. Each task is one branch (`feature/<initial>-<task>`) and one pull request, reviewed by one other member: Ringo reviews Abdelrahman, Abdelrahman reviews Gurnoor, Gurnoor reviews Joshua, Joshua reviews Ringo.

Dates: M2 Nov 21, M3 Nov 28, final submission Dec 5, demos Dec 8.

## Shared foundation (Nov 6 to 8, do first)

| Task | Owner | Done when |
| --- | --- | --- |
| F1 Project skeleton | Abdelrahman | Spring Boot app, one package per module, dev and test profiles, health endpoint, Maven wrapper |
| F2 Database baseline | Gurnoor | Docker Compose PostgreSQL, Flyway with the migration ranges, Testcontainers base test class |
| F3 Shared API code | Joshua | Global error handler with the shared error body, validation, request id in logs, OpenAPI page |
| F4 Security foundation | Ringo | Session auth, roles, CSRF header, CORS, `CurrentUser` |
| F5 Events | Abdelrahman | `EventPublisher` interface and in-process version, event classes |
| F6 CI | Ringo | GitHub Actions builds and runs tests on every pull request |
| F7 Client foundation | Ringo | React app, routing, API client with cookie and CSRF token, error display |

Agree on these interfaces in the first two days, each with a stub: `CurrentUser` (Ringo), `MachineQuery` (Abdelrahman), `ReservationQuery` and `ReservationCommands` (Gurnoor), `PaymentService` (Gurnoor), `EventPublisher` (Abdelrahman).

---

## Ringo: accounts, security, administration views (`identity`, UC1, UC8.3)

### A1 Registration (M2, Nov 6 to 13) UC1.1, FR01, BR01, BR02
- [ ] Migration V100 for `users` and `sessions`
- [ ] bcrypt password hashing
- [ ] `POST /api/auth/register` creates customers only
- [ ] 400 for invalid input, 409 for a duplicate email
- [ ] Tests: valid, invalid, duplicate

### A2 Sign-in and sign-out (M2, Nov 6 to 13) UC1.2, UC1.3, FR02, FR03, BR03
- [ ] `POST /api/auth/login` with one generic error, new session id each time, returns csrfToken
- [ ] `POST /api/auth/logout` deletes the session on the server
- [ ] `GET /api/users/me`
- [ ] 30-minute idle expiry (401 afterwards)
- [ ] 5 failed sign-ins per email in 15 minutes gives 429
- [ ] Tests, including NFR03 (old cookie refused after sign-out)

### A3 Roles and admin seed (M2, Nov 6 to 13) FR27, BR02
- [ ] Role rules for `/api/admin/**`: 401 without a session, 403 for a customer
- [ ] Seed script for the first administrator, README note on signing in as admin
- [ ] Automated test that calls every admin endpoint as a visitor and as a customer (NFR06), extended as others add endpoints

### A4 Admin reservations and payments (M2, Nov 14 to 21) UC8.3, FR23
- [ ] `GET /api/admin/reservations` with status, machineId, from, to filters
- [ ] `GET /api/admin/payments` with status, from, to filters
- [ ] Uses Gurnoor's service interfaces, never his tables
- [ ] Tests for 400, 401, 403 and empty results

### A5 Client screens (M2 and M3)
- [ ] Register, sign-in, sign-out (M2)
- [ ] Role-based navigation (hiding only; the server enforces)
- [ ] Admin reservation and payment lists (M3)

### A6 Security hardening (M3, Nov 22 to 28)
- [ ] Check CSRF on every state-changing endpoint (missing token gives 403)
- [ ] Search logs for passwords, session ids, card numbers, API keys (NFR07)
- [ ] SQL injection strings in inputs, dependency scan

### A7 Document and submission (final week)
- [ ] Keep the design document in step with the code
- [ ] Collect `docs/AI_USE.md` from everyone for the final document
- [ ] Submit

---

## Abdelrahman: machines and simulator (`machine`, `simulator`, UC2, UC5, UC8.1, UC8.2)

### B1 Machine data (M2, Nov 6 to 13) BR07, BR10
- [ ] Migrations V200 for `machine_types`, `machines`, `machine_status_changes`, `machine_cycles`
- [ ] Seed: a few washer and dryer types (cycle length, price) and about 10 machines

### B2 Search machines (M2, Nov 6 to 13) UC2.1, FR05, FR06
- [ ] `GET /api/machines` with kind and availableAt filters, computed RESERVED label
- [ ] `GET /api/machines/{id}`
- [ ] `GET /api/machines/{id}/availability?date=` (blocked time = cycle + 10 minutes, uses `ReservationQuery`)
- [ ] 400 for a past time or more than 7 days ahead

### B3 Status model (M2, Nov 6 to 13) FR17, BR10
- [ ] `MachineStatusService`: AVAILABLE, IN_USE, OUT_OF_SERVICE, with the allowed changes only (409 otherwise)
- [ ] `version` column for optimistic locking
- [ ] History row in the same transaction

### B4 Admin machine management (M2, Nov 14 to 21) UC8.1, UC8.2, FR21, FR22
- [ ] `POST`, `PUT`, `DELETE` (deactivate) on `/api/admin/machines`
- [ ] `PATCH /api/admin/machines/{id}/status`
- [ ] Out of service or deactivation asks booking to cancel and refund future reservations (`ReservationCommands`)
- [ ] 409 for an IN_USE machine or a duplicate name

### B5 Simulator in the app (M2, Nov 14 to 21) UC5.1, FR15, FR16
- [ ] Scheduler that starts machines at the reservation start and finishes them after the cycle
- [ ] `machine_cycles` row per run
- [ ] Publish MachineStarted, MachineCycleCompleted, MachineFailure
- [ ] Fault injection: configured rate plus `POST /api/admin/machines/{id}/simulate-fault` for the demo

### B6 Separate simulator and Kafka (M3, Nov 22 to 28)
- [ ] Kafka in Docker Compose, Kafka version of `EventPublisher`
- [ ] Simulator runs as its own service and publishes machine events to Kafka

### B7 Containers and deployment (M3, Nov 22 to 28, everyone helps)
- [ ] Dockerfiles and Compose for the app, simulator, notification service, database and Kafka
- [ ] Cloud deployment if the milestone instructions require it

### B8 Client screens (M2 and M3)
- [ ] Machines page with type filter, status colors, 30-second refresh (NFR03)
- [ ] Admin machine management screen

### B9 Tests (with every task)
- [ ] Allowed and forbidden status changes, history rows
- [ ] Availability edge cases, simulator fault path

---

## Gurnoor: reservations and payments (`booking`, `payment`, UC3, UC4)

### C1 Create a reservation (M2, Nov 6 to 13) UC3.1, FR07, FR08, BR04 to BR08
- [ ] Concurrency test first: 20 simultaneous requests for one slot give exactly one 201 (NFR02)
- [ ] Migration V300 for `reservations`
- [ ] `POST /api/reservations`: lock the machine row, check overlap, price from the machine type, PENDING_PAYMENT with a 10-minute hold
- [ ] Publish ReservationCreated

### C2 Hold expiry, view and cancel (M2, Nov 6 to 13) UC3.2, UC3.3, FR09, FR10, BR09
- [ ] Scheduled job that expires unpaid holds
- [ ] `GET /api/reservations`, `GET /api/reservations/{id}` (404 for other customers)
- [ ] `DELETE /api/reservations/{id}` until the start; full refund if paid

### C3 Payments with Stripe test mode (M2, Nov 14 to 21) UC4.1, UC4.2, FR11 to FR14, BR11
- [ ] Migration for `payments`
- [ ] `PaymentGateway`, `StripeTestGateway`, `MockGateway`
- [ ] `POST /api/payments` with idempotency key; on success one transaction saves SUCCEEDED and sets CONFIRMED
- [ ] Decline records FAILED and keeps the hold (422); timeout leaves it pending and checks Stripe before retry
- [ ] `GET /api/payments/{id}`
- [ ] Publish PaymentCompleted after commit; refunds through `PaymentService`

### C4 React to the machine (M2 end and M3) FR15, FR16
- [ ] On MachineStarted set IN_PROGRESS; on MachineCycleCompleted set COMPLETED
- [ ] On MachineFailure, out of service or deactivation: cancel current and future reservations and refund in full
- [ ] `ReservationQuery` for machine availability and review eligibility

### C5 Client screens (M2 and M3)
- [ ] Booking flow from a chosen machine and time
- [ ] Stripe hosted payment form with test cards
- [ ] My Reservations with cancel

### C6 Tests (with every task)
- [ ] Overlap boundaries, expired hold, cancel after start (409)
- [ ] Declined payment, duplicate key, timeout, rollback when confirmation fails (NFR04)
- [ ] No card data in the database or logs

---

## Joshua: notifications, reviews, analytics, AI (`notification`, `review`, `analytics`, `ai`, UC6, UC9, UC10, UC11)

### D1 Notifications in the app (M2, Nov 14 to 21) UC6.1, UC6.2, FR18 to FR20, BR12
- [ ] Migration V400 for `notifications`
- [ ] Handlers for PaymentCompleted, MachineCycleCompleted, MachineFailure, unique per user and event
- [ ] `GET /api/notifications`, `PATCH /api/notifications/{id}/read`
- [ ] M2 demo: "reservation confirmed" appears after payment

### D2 Notification service over Kafka (M3, Nov 22 to 28) FR19
- [ ] Notification runs as its own service and consumes events from Kafka
- [ ] Retry and log on failure; the payment is never undone

### D3 Reviews (M3, Nov 22 to 28) UC11.1 to UC11.3, FR28 to FR30, BR14, BR15
- [ ] Migration for `reviews`
- [ ] `POST /api/reviews` (needs a COMPLETED reservation via `ReservationQuery`, one per reservation)
- [ ] `GET /api/machines/{id}/reviews` with average, first names only
- [ ] `PUT /api/admin/reviews/{id}/reply`, `GET /api/admin/reviews`
- [ ] Publish ReviewSubmitted

### D4 Admin summary (M3, Nov 22 to 28) UC9.1, FR24
- [ ] `GET /api/admin/summary?from=&to=`: machines by status, reservations by status, completed cycles, payment count and total
- [ ] Uses other modules' query interfaces; zeros when empty

### D5 AI feature (M3, Nov 22 to 28) UC10, FR25, FR26, BR13, BR16
- [ ] Nov 22: team confirms option A or B
- [ ] `LlmClient` interface, one adapter, a stub for tests
- [ ] Option A: `POST /api/assistant/chat` with two read-only tools run as the customer, 500-character limit, 20 per minute, 8-second timeout, 503 when down
- [ ] Option B: handler on ReviewSubmitted, sentiment, topics, urgent flag, `review_ai_analyses`, 3 retries, shown in `GET /api/admin/reviews`

### D6 Logging and health (M2 end and M3) NFR08
- [ ] Request id in every log line, no sensitive data
- [ ] Health endpoint with database status

### D7 Client screens (M3)
- [ ] Notification bell and list
- [ ] Review form and machine reviews, admin reply
- [ ] Admin summary page
- [ ] Assistant chat panel (option A) or review sentiment filters (option B)

### D8 Tests and quality checks (M3 and final week)
- [ ] Duplicate events, review eligibility, script-injection strings shown as text (NFR10)
- [ ] AI down (NFR05), AI timing (NFR09)
- [ ] Load tests for NFR01, keep the results for the final submission
