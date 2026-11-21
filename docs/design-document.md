# SpinCycle: Smart Laundromat Service Platform

**Milestone 1: Requirements and Design** (design document v3)

EECS 4413, Fall 2026. Submitted November 5, 2026.

Team: Ringo (A, accounts, security and administration views), Abdelrahman (B, machines and simulator), Gurnoor (C, reservations and payments), Joshua (D, notifications, reviews, analytics and the AI feature).

# 1. Overview

## 1.1 System description

SpinCycle is a Smart Laundromat Service Platform: a web application that automates what a traditional laundromat does by hand. Customers register, sign in, search washers and dryers, reserve a machine for a time slot, pay for the reservation with a simulated payment, receive notifications, and review machines they have used. Administrators manage machines and their status, view reservations and payments, reply to reviews and see a usage summary. The AI feature is either a chat assistant that finds machines from a plain-language request, or automatic AI analysis of reviews for administrators.

Machines are simulated by software, and payments use Stripe test mode with test data only. Real card information is never collected or stored.

## 1.2 Scope

| **In scope**                                                                 | **Out of scope**                                                                                                   |
|------------------------------------------------------------------------------|--------------------------------------------------------------------------------------------------------------------|
| UC1 to UC6 and UC8 to UC10 from the project description, plus reviews (UC11) | Loyalty points and offers (optional in the project description)                                                    |
| Customer and administrator roles, enforced on the server                     | Machine monitoring, machine maintenance, facility and financial information (removed from the project description) |
| REST APIs with JSON, a relational database, simulated machines and payments  | Physical machines or sensors, real card data, real email or SMS                                                    |
| One event-driven interaction and separate deployable services in Milestone 3 | A separate microservice for every business function                                                                |

## 1.3 Assumptions

- The system manages one laundromat. Machines can be booked at any time of day, up to 7 days ahead.

- Each machine type has one cycle length and one price. A reservation blocks the machine for the cycle plus a 10-minute unloading buffer.

- An unpaid reservation is held for 10 minutes, then expires.

- Payments use Stripe test mode through a payment adapter, with a mock adapter for automated tests.

- The simulator starts a machine at the reservation start time and finishes it when the cycle ends. It can also simulate a fault.

- Notifications are shown in the application. There is no real email or SMS.

- The first administrator is created by a seed script. Public registration creates customers only.

- The AI assistant uses an external language model through an API key kept on the server.

## 1.4 Technology

Backend: Java 17 with Spring Boot, Spring Data JPA and Spring Security. Database: PostgreSQL with Flyway migrations. Client: React single-page application over HTTP, REST and JSON. Authentication: server-side sessions in an HTTP-only cookie. Payments: Stripe test mode. Events: in-process in Milestone 2, Kafka in Milestone 3. Containers: Docker in Milestone 3. AI: an external language model behind an interface.

# 2. Use Cases

## 2.1 Use-case overview

Use-case numbers follow the project description. UC7 (loyalty) is optional and not included. UC11 (reviews) is added because the project description lists reviews in the Milestone 3 system, in the example endpoints (/api/reviews) and in the authorization examples (customers submit reviews, administrators respond).

| **Use case**     | **Name**                                                      | **Actors**                       | **Owner** |
|------------------|---------------------------------------------------------------|----------------------------------|-----------|
| UC1.1 to UC1.3   | Register, sign in, sign out                                   | Visitor, customer, administrator | A         |
| UC2.1            | Search and select a machine                                   | Visitor, customer                | B         |
| UC3.1 to UC3.3   | Reserve a machine, view my reservations, cancel a reservation | Customer                         | C         |
| UC4.1, UC4.2     | Pay for a reservation, view payment status                    | Customer                         | C         |
| UC5.1            | Simulate a machine cycle                                      | Simulator (system)               | B         |
| UC6.1, UC6.2     | Receive a notification, view my notifications                 | Customer, system                 | D         |
| UC8.1, UC8.2     | Manage machines, change machine status                        | Administrator                    | B         |
| UC8.3            | View reservations and payments                                | Administrator                    | A         |
| UC9.1            | View the admin summary                                        | Administrator                    | D         |
| UC10.1 or UC10.2 | AI feature: assistant, or AI review analysis                  | Customer or administrator        | D         |
| UC11.1 to UC11.3 | Submit a review, browse reviews, reply to a review            | Visitor, customer, administrator | D         |

*Figure 1. Use-case diagram. Mermaid source: [diagrams.md, figure 1](diagrams/diagrams.md).*

## 2.2 Accounts (A)

### UC1.1 Register an account

**Actors:** Visitor.

**Preconditions:** The visitor is not signed in.

**Main steps**

1.  The visitor opens the registration page and enters name, email and password.

2.  The system validates the input.

3.  The system saves the account with the CUSTOMER role and a hashed password.

4.  The system signs the customer in.

**Alternate and error steps**

- Input is missing or invalid: an error is shown and nothing is created (400).

- The email is already registered: an error is shown and nothing is created (409).

**Postconditions:** A customer account exists and the customer is signed in.

### UC1.2 Sign in

**Actors:** Customer, administrator.

**Preconditions:** The user has an account.

**Main steps**

1.  The user enters email and password.

2.  The system checks them against the stored hash.

3.  The system starts a session and shows the customer or administrator home page.

**Alternate and error steps**

- Wrong email or password: one generic error is shown and no session starts (401).

- Too many failed attempts: further attempts are refused for a while (429).

**Postconditions:** The user has an active session.

### UC1.3 Sign out

**Actors:** Customer, administrator.

**Preconditions:** The user is signed in.

**Main steps**

1.  The user chooses Sign out.

2.  The system deletes the session on the server and shows the public home page.

**Alternate and error steps**

- The session already expired: the user is treated as signed out.

**Postconditions:** Protected pages need a new sign-in.

## 2.3 Machines (B)

Machine status is one of AVAILABLE, IN_USE or OUT_OF_SERVICE. "Reserved" is not stored on the machine, because a machine has many reservations at different times. The API shows the label RESERVED when an AVAILABLE machine has a reservation starting within the next 10 minutes. A deactivated machine is hidden and cannot be booked.

### UC2.1 Search and select a machine

**Actors:** Visitor, customer.

**Preconditions:** None to search. Signed in to reserve.

**Main steps**

1.  The user opens the Machines page.

2.  The system shows active machines with name, type (washer or dryer), capacity, price, cycle length and status.

3.  The user filters by type or by a desired start time.

4.  The system shows only machines that are free for the whole blocked time at that start.

5.  The user selects a machine and a start time and continues to UC3.1. A visitor is asked to sign in first.

**Alternate and error steps**

- No machine matches: the system shows an empty list and the next free times.

- Invalid filter (a past time, more than 7 days ahead): 400 with field errors.

**Postconditions:** A machine and start time are selected. Nothing is held yet.

### UC5.1 Simulate a machine cycle

**Actors:** Simulator (system). Secondary: booking and notification modules.

**Preconditions:** A CONFIRMED reservation reaches its start time and the machine is AVAILABLE.

**Main steps**

1.  At the start time, the simulator sets the machine to IN_USE, the reservation becomes IN_PROGRESS, and MachineStarted is published.

2.  When the cycle length has passed, the simulator sets the machine to AVAILABLE and publishes MachineCycleCompleted.

3.  The booking module marks the reservation COMPLETED, and the customer is notified (UC6.1).

**Alternate and error steps**

- The simulator injects a fault (a small configured rate, or an administrator demo button): the machine becomes OUT_OF_SERVICE and MachineFailure is published. The current and future reservations on that machine are cancelled and refunded in full, and the customers are notified.

- The machine is OUT_OF_SERVICE at the start time: the reservation is cancelled and refunded in full.

**Postconditions:** The status change is recorded in the status history.

### UC8.1 Manage machines

**Actors:** Administrator.

**Preconditions:** The administrator is signed in.

**Main steps**

1.  The administrator opens machine management.

2.  To add a machine, the administrator enters a unique name and chooses a machine type. The system creates it as AVAILABLE.

3.  To update a machine, the administrator changes its name or type.

4.  To deactivate a machine, the administrator chooses Deactivate. The system hides it from customers, and its future reservations are cancelled and refunded in full.

**Alternate and error steps**

- A customer tries this: 403. A visitor: 401.

- The name is already used: 409. Invalid input: 400.

- The machine is IN_USE: it cannot be deactivated until the cycle ends (409).

**Postconditions:** The machine list shows the change.

### UC8.2 Change machine status

**Actors:** Administrator.

**Preconditions:** The administrator is signed in.

**Main steps**

1.  The administrator selects a machine and sets it to OUT_OF_SERVICE (with a reason) or back to AVAILABLE.

2.  The system checks that the change is allowed, saves it and records it in the status history.

3.  Setting OUT_OF_SERVICE cancels and refunds the machine’s future reservations, as for a fault.

**Alternate and error steps**

- The machine is IN_USE: the change is refused (409).

- A customer tries this: 403.

**Postconditions:** The new status shows on the Machines page.

## 2.4 Reservations and payments (C)

### UC3.1 Reserve a machine

**Actors:** Customer.

**Preconditions:** The customer is signed in and has selected a machine and start time (UC2.1).

**Main steps**

1.  The customer confirms the machine and start time.

2.  The system validates the start time (in the future, within 7 days).

3.  The system locks the machine row and checks that no active reservation overlaps the blocked time (cycle plus 10 minutes).

4.  The system creates a PENDING_PAYMENT reservation with the price of the machine type and a 10-minute hold, and publishes ReservationCreated.

5.  The customer continues to payment (UC4.1).

**Alternate and error steps**

- Another reservation overlaps: 409 and nothing is created.

- The machine is deactivated or OUT_OF_SERVICE: 409.

- Invalid input: 400. Not signed in: 401.

- The hold passes 10 minutes without payment: the reservation becomes EXPIRED and the slot is free again.

**Postconditions:** One PENDING_PAYMENT reservation exists, or none on failure.

### UC3.2 View my reservations

**Actors:** Customer.

**Preconditions:** The customer is signed in.

**Main steps**

1.  The customer opens My Reservations.

2.  The system lists the customer’s own reservations with machine, time, price and status.

**Alternate and error steps**

- No reservations: an empty list.

- A reservation of another customer is requested: 404.

**Postconditions:** Nothing changes.

### UC3.3 Cancel a reservation

**Actors:** Customer.

**Preconditions:** The customer is signed in and owns the reservation.

**Main steps**

1.  The customer chooses Cancel on a reservation that has not started.

2.  The system sets it to CANCELLED and frees the slot.

3.  If it was paid, the system refunds it in full through the payment adapter.

**Alternate and error steps**

- The reservation has started, finished or is already cancelled: 409.

- Not the owner: 404.

**Postconditions:** The reservation is CANCELLED and any payment is refunded.

### UC4.1 Pay for a reservation

**Actors:** Customer.

**Preconditions:** The customer owns a PENDING_PAYMENT reservation whose hold has not expired.

**Main steps**

1.  The customer enters a Stripe test card in Stripe’s payment form.

2.  The client sends the reservation id, a Stripe test payment method id and an idempotency key.

3.  The server checks the owner, status, hold and amount, then creates and confirms the payment in Stripe test mode.

4.  On success, one transaction records the payment as SUCCEEDED and sets the reservation to CONFIRMED.

5.  The system publishes PaymentCompleted, and the customer is notified (UC6.1).

**Alternate and error steps**

- The test card is declined: the attempt is recorded as FAILED, the reservation stays PENDING_PAYMENT and the customer can retry until the hold expires (422).

- The same request is sent twice: the idempotency key returns the first result and nothing is charged twice.

- The hold has expired: 409 and no payment is made.

- Stripe does not answer: the reservation stays PENDING_PAYMENT and the payment status is checked with Stripe before any retry.

**Postconditions:** The reservation is CONFIRMED with one successful payment, or stays unpaid.

### UC4.2 View payment status

**Actors:** Customer.

**Preconditions:** The customer is signed in.

**Main steps**

1.  The customer opens a reservation.

2.  The system shows its payment amount, status, date and Stripe reference.

**Alternate and error steps**

- No payment yet: shown as unpaid.

- Not the owner: 404.

**Postconditions:** Nothing changes.

### UC8.3 View reservations and payments (owner A)

**Actors:** Administrator.

**Preconditions:** The administrator is signed in.

**Main steps**

1.  The administrator opens the reservations list and filters it by status, machine or date.

2.  The administrator opens the payments list and filters it by status or date.

**Alternate and error steps**

- A customer tries this: 403.

- Invalid filter: 400. Nothing matches: an empty list.

**Postconditions:** Nothing changes. Card details are never shown.

## 2.5 Notifications, reviews, analytics and AI (D)

### UC6.1 Receive a notification

**Actors:** Customer (the system creates it).

**Preconditions:** An event happens for the customer.

**Main steps**

1.  The notification module receives PaymentCompleted, MachineCycleCompleted or MachineFailure.

2.  It stores one unread notification for the affected customer: "Reservation confirmed", "Your laundry is done" or "Machine fault: your reservation was cancelled and refunded".

3.  The customer sees the unread count the next time the page refreshes.

**Alternate and error steps**

- The same event arrives twice: only one notification is created.

- Creating the notification fails: it is retried and logged. The payment or cycle is not undone.

**Postconditions:** One unread notification exists for the customer.

### UC6.2 View my notifications

**Actors:** Customer.

**Preconditions:** The customer is signed in.

**Main steps**

1.  The customer opens the notification list, newest first.

2.  The customer opens one, and the system marks it read.

**Alternate and error steps**

- A notification of another user: 404.

**Postconditions:** The opened notification is marked read.

### UC9.1 View the admin summary

**Actors:** Administrator.

**Preconditions:** The administrator is signed in.

**Main steps**

1.  The administrator opens the dashboard and chooses a date range (default: the last 7 days).

2.  The system shows the number of machines by status, reservations by status, completed cycles, and successful payments with their total amount.

**Alternate and error steps**

- A customer tries this: 403.

- Invalid range (end before start): 400.

- No data: zeros, not an error.

**Postconditions:** Nothing changes.

UC10 requires one AI feature. The team will implement one of the two options below and confirm the choice at the start of Milestone 3. Option A is the default; option B is chosen if the assistant proves too slow or unreliable in testing. Both reuse existing data and leave the rest of the system working if the AI service is down.

### UC10.1 Option A: find a machine with the AI assistant

**Actors:** Customer.

**Preconditions:** The customer is signed in.

**Main steps**

1.  The customer types a request such as "a washer free in the next hour".

2.  The system sends the message and two read-only tools (machine search and availability) to the language model.

3.  The model calls the tools. The system runs them with the customer’s permissions and returns the results.

4.  The assistant answers in plain language with a short list of matching machines and a Reserve button for each.

5.  Reserve opens the normal reservation flow (UC3.1). The assistant never books or pays.

**Alternate and error steps**

- Empty message or more than 500 characters: 400.

- Nothing matches: the assistant says so and suggests a later time.

- The language model is down or takes more than 8 seconds: "The assistant is unavailable" (503). Search and booking keep working.

- More than 20 messages a minute: 429.

**Postconditions:** Nothing in the system changes. Only usage statistics are logged.

### UC10.2 Option B: AI review analysis

**Actors:** System (started by a new review). Administrator sees the result.

**Preconditions:** A review was saved (UC11.1).

**Main steps**

1.  The review module publishes ReviewSubmitted.

2.  The AI handler sends only the rating and comment text to the language model and asks for a sentiment (positive, neutral or negative), up to 3 topics (for example "dryer heat") and an urgent flag.

3.  The system checks the answer against the allowed values and saves it with the review.

4.  Administrators see the sentiment, topics and urgent flag in the review list, and can filter by them.

**Alternate and error steps**

- The model is down, slow or answers in a wrong format: the analysis is marked FAILED and retried up to 3 times. The review itself is already saved and shown.

- The review has no comment: the model is not called and the sentiment comes from the rating.

- The same event arrives twice: only one analysis is stored.

**Postconditions:** The review has one analysis with status DONE or FAILED. Results are advice only, labelled AI-generated, and shown to administrators only.

### UC11.1 Submit a review

**Actors:** Customer.

**Preconditions:** The customer is signed in and has a COMPLETED reservation on the machine that has no review yet.

**Main steps**

1.  The customer opens the completed reservation and chooses Write a review.

2.  The customer gives a rating from 1 to 5 and an optional comment (up to 1000 characters).

3.  The system validates the input and checks the reservation.

4.  The system saves the review and publishes ReviewSubmitted.

**Alternate and error steps**

- Rating missing or not 1 to 5, or comment too long: 400.

- No completed reservation for this machine: 403.

- That reservation already has a review: 409.

**Postconditions:** One review exists for the reservation.

### UC11.2 Browse reviews

**Actors:** Visitor, customer, administrator.

**Preconditions:** None.

**Main steps**

1.  The user opens a machine page.

2.  The system shows the machine’s reviews (rating, comment, reviewer’s first name, date, reply) newest first, and the average rating.

**Alternate and error steps**

- No reviews: an empty list and no average.

- The machine does not exist: 404.

**Postconditions:** Nothing changes.

### UC11.3 Reply to a review

**Actors:** Administrator.

**Preconditions:** The administrator is signed in.

**Main steps**

1.  The administrator opens a review and writes a reply (up to 1000 characters).

2.  The system saves the reply, which is shown under the review.

**Alternate and error steps**

- A customer tries this: 403.

- The review already has a reply: the new text replaces it.

- Empty or too long: 400. The review does not exist: 404.

**Postconditions:** The review shows one reply.

# 3. Functional Requirements

## 3.1 Requirements

| **ID** | **The system must...**                                                                                                                                                                                                  | **Use case**             |
|--------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|--------------------------|
| FR01   | Let a visitor register with name, email and password, rejecting invalid input and a duplicate email.                                                                                                                    | UC1.1                    |
| FR02   | Let a user sign in with email and password and start a session, with one generic error on failure.                                                                                                                      | UC1.2                    |
| FR03   | End the session on the server at sign-out, and treat an expired session as signed out.                                                                                                                                  | UC1.3                    |
| FR04   | Let a customer see only their own reservations, payments and notifications.                                                                                                                                             | UC1, UC3.2, UC4.2, UC6.2 |
| FR05   | Show active machines with type, capacity, price, cycle length and status, and filter by type or start time.                                                                                                             | UC2.1                    |
| FR06   | Show only machines that are free for the whole blocked time when a start time is given.                                                                                                                                 | UC2.1                    |
| FR07   | Create a reservation for a machine and start time, held for 10 minutes until paid.                                                                                                                                      | UC3.1                    |
| FR08   | Reject a reservation that overlaps another active reservation on the same machine, including under concurrent requests.                                                                                                 | UC3.1                    |
| FR09   | Expire unpaid reservations after 10 minutes and free the slot.                                                                                                                                                          | UC3.1                    |
| FR10   | Let a customer cancel a reservation that has not started, with a full refund if it was paid.                                                                                                                            | UC3.3                    |
| FR11   | Accept a simulated payment through Stripe test mode, record amount, status and reservation, and confirm the reservation on success.                                                                                     | UC4.1                    |
| FR12   | Record a declined payment without confirming the reservation, and let the customer retry while the hold lasts.                                                                                                          | UC4.1                    |
| FR13   | Never charge twice for the same request (idempotency key).                                                                                                                                                              | UC4.1                    |
| FR14   | Show the payment status of a reservation to its owner.                                                                                                                                                                  | UC4.2                    |
| FR15   | Simulate machine cycles: IN_USE at the reservation start, AVAILABLE at the end, and OUT_OF_SERVICE on a simulated fault.                                                                                                | UC5.1                    |
| FR16   | Cancel and refund in full the reservations affected by a fault, an out-of-service change or a deactivation.                                                                                                             | UC5.1, UC8.1, UC8.2      |
| FR17   | Record every machine status change with time, reason and who made it.                                                                                                                                                   | UC5.1, UC8.2             |
| FR18   | Create a notification when a reservation is confirmed, a cycle finishes or a fault cancels a reservation.                                                                                                               | UC6.1                    |
| FR19   | Deliver the "reservation confirmed" notification through an event (asynchronous in Milestone 3).                                                                                                                        | UC6.1                    |
| FR20   | Let a customer list notifications and mark them read.                                                                                                                                                                   | UC6.2                    |
| FR21   | Let an administrator add, update and deactivate machines.                                                                                                                                                               | UC8.1                    |
| FR22   | Let an administrator change machine status between AVAILABLE and OUT_OF_SERVICE.                                                                                                                                        | UC8.2                    |
| FR23   | Let an administrator view and filter all reservations and payments, without card details.                                                                                                                               | UC8.3                    |
| FR24   | Show administrators a summary of machines, reservations, completed cycles and payments for a date range.                                                                                                                | UC9.1                    |
| FR25   | Provide one AI feature: either an assistant that finds machines using only live data through read-only tools (option A), or automatic analysis of each new review into sentiment, topics and an urgent flag (option B). | UC10.1, UC10.2           |
| FR26   | Keep every non-AI function working when the AI service is unavailable.                                                                                                                                                  | UC10                     |
| FR27   | Check the role on the server for every administrator function.                                                                                                                                                          | UC8, UC9, UC11.3         |
| FR28   | Let a customer review a machine (rating 1 to 5, optional comment) once per completed reservation.                                                                                                                       | UC11.1                   |
| FR29   | Show a machine’s reviews and average rating to anyone.                                                                                                                                                                  | UC11.2                   |
| FR30   | Let an administrator reply to a review.                                                                                                                                                                                 | UC11.3                   |

## 3.2 Business rules

| **ID** | **Rule**                                                                                                                                                          |
|--------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| BR01   | Email is unique. Passwords are stored only as bcrypt hashes.                                                                                                      |
| BR02   | Public registration creates customers only. Administrators come from a seed script.                                                                               |
| BR03   | A session expires after 30 minutes without activity. After 5 failed sign-ins for one email in 15 minutes, sign-in is refused for 15 minutes (429).                |
| BR04   | A reservation blocks its machine from the start time to the end of the cycle plus 10 minutes for unloading.                                                       |
| BR05   | Two active reservations (PENDING_PAYMENT with an unexpired hold, CONFIRMED or IN_PROGRESS) on the same machine never overlap.                                     |
| BR06   | Reservations start in the future and at most 7 days ahead.                                                                                                        |
| BR07   | The price comes from the machine type and is calculated on the server, in cents.                                                                                  |
| BR08   | An unpaid hold lasts 10 minutes.                                                                                                                                  |
| BR09   | A reservation can be cancelled until it starts, with a full refund if paid. A reservation cancelled because of the machine is always refunded in full.            |
| BR10   | Machine status is AVAILABLE, IN_USE or OUT_OF_SERVICE. An IN_USE machine cannot be set out of service or deactivated by an administrator.                         |
| BR11   | Only test payment data is used. Card numbers never reach the server, the database or the logs.                                                                    |
| BR12   | One event creates at most one notification per user. A failed notification never undoes the payment or cycle.                                                     |
| BR13   | The AI feature only reads data. The assistant never creates, changes or pays for a reservation, and review analysis never hides or deletes a review.              |
| BR14   | A review needs a COMPLETED reservation by the same customer on that machine, and each reservation has at most one review. Reviewers are shown by first name only. |
| BR15   | A review has at most one reply, written by an administrator. Review and reply text is plain text of up to 1000 characters.                                        |
| BR16   | Only the rating and comment text are sent to the language model, never names, emails or payment data.                                                             |

# 4. Non-functional Requirements

Percentiles are the 95th. Each target has a test that checks it.

| **ID**                 | **Requirement**                                                                                                                                                               | **How we check it**                                                     |
|------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|-------------------------------------------------------------------------|
| NFR01 Performance      | Machine search and reservation creation answer in under 1 second with 50 concurrent users.                                                                                    | Load test of 5 minutes with 50 virtual users.                           |
| NFR02 Concurrency      | 20 simultaneous requests for the same machine and time give exactly one 201 and 19 answers of 409.                                                                            | Automated test with 20 threads.                                         |
| NFR03 Status updates   | A machine status change shows on the Machines page within 30 seconds.                                                                                                         | Change a status and time how long the page takes to show it.            |
| NFR04 Reliability      | A declined or unanswered payment never leaves a reservation CONFIRMED without a successful payment, and a retry never charges twice.                                          | Automated tests with the mock adapter: decline, timeout, duplicate key. |
| NFR05 Reliability      | With the AI service stopped, every other endpoint still works.                                                                                                                | Run the API test suite with the AI stub turned off.                     |
| NFR06 Security         | Every administrator endpoint answers 401 to a visitor and 403 to a customer.                                                                                                  | Automated test over all administrator endpoints.                        |
| NFR07 Security         | No password, session id, card number or API key appears in the logs.                                                                                                          | Search the logs after the test run (0 matches).                         |
| NFR08 Observability    | A health endpoint reports the application and database status, every error is logged with a request id, and errors use one JSON error body.                                   | Call /actuator/health; trigger an error and check the log and response. |
| NFR09 AI response time | Option A: the assistant answers within 8 seconds, or shows the unavailable message. Option B: a review is saved in under 500 ms, and its analysis finishes within 30 seconds. | Run 20 test prompts or 20 reviews; stub a 10-second model delay.        |
| NFR10 Security         | 10 out of 10 script-injection test strings in review comments and replies are shown as plain text.                                                                            | Automated test that submits the strings and checks the page.            |

Bottlenecks and how the design handles them: simultaneous reservations for one machine are serialised by a lock on that machine row only, so other machines are not blocked. Status refresh uses polling every 30 seconds, which is cheap at laundromat scale. Search uses indexes on machine and reservation times.

# 5. Architecture

## 5.1 Chosen style

A 3-tier client-server system with a layered application tier, built as a modular monolith.

- Client tier: a React single-page application using HTTP, REST and JSON.

- Application tier: one Spring Boot application. Each module has three layers: controller (REST and validation), service (business rules and transactions) and repository (data access). Each layer calls only the layer below.

- Data tier: PostgreSQL. Each module owns its own tables.

Modules call each other only through service interfaces or events, never through another module’s tables. External systems (Stripe, Kafka, the language model) sit behind adapter interfaces we own, so they can be replaced by mocks in tests.

In Milestone 3, the simulator and the notification module become separate deployable services. The main application publishes events to Kafka, and they subscribe.

*Figure 2. Architecture overview. Mermaid source: [diagrams.md, figure 2](diagrams/diagrams.md).*

## 5.2 Reasons

- Layered and modular: matches the project’s step-by-step plan (monolith in M2, selected services in M3) and keeps presentation, business logic, data access and external communication separate, as section 5.1 of the project description asks.

- Client-server with a single-page client: the REST API is required anyway, and the client reuses it. Machine status refreshes without reloading the page.

- Adapter pattern for Stripe, Kafka and the AI model: business code does not depend on a vendor and tests run without network calls.

- Server-side sessions instead of JWT: sign-out takes effect immediately, and they are simpler to secure for one application.

- Kafka in M3 for events: the project requires at least one event-driven interaction. In M2 the same events go through an in-process publisher, so the switch does not change business code.

# 6. Component and Service Decomposition

## 6.1 Modules

| **Module**   | **Owner** | **Responsibility**                                                                            | **Tables**                                                      | **Milestone 3**  |
|--------------|-----------|-----------------------------------------------------------------------------------------------|-----------------------------------------------------------------|------------------|
| Identity     | A         | Registration, sign-in, sessions, roles, security configuration                                | users, sessions                                                 | Main application |
| Machine      | B         | Machine types, machines, status, availability, admin machine management                       | machine_types, machines, machine_status_changes, machine_cycles | Main application |
| Simulator    | B         | Starts and finishes cycles, injects faults                                                    | none                                                            | Separate service |
| Booking      | C         | Reservations, overlap check, hold expiry, cancellation                                        | reservations                                                    | Main application |
| Payment      | C         | Stripe test-mode payments and refunds                                                         | payments                                                        | Main application |
| Notification | D         | Creates notifications from events                                                             | notifications                                                   | Separate service |
| Analytics    | D         | Admin summary (read-only queries)                                                             | none                                                            | Main application |
| Review       | D         | Reviews and replies                                                                           | reviews                                                         | Main application |
| AI feature   | D         | Option A: chat assistant with read-only tools. Option B: review analysis from ReviewSubmitted | review_ai_analyses (option B)                                   | Main application |

## 6.2 Business events

| **Event**             | **Published by** | **Used by**                                                             |
|-----------------------|------------------|-------------------------------------------------------------------------|
| ReservationCreated    | Booking          | Analytics (counts)                                                      |
| PaymentCompleted      | Payment          | Booking (confirm), Notification ("reservation confirmed")               |
| PaymentFailed         | Payment          | Booking (keep the hold until it expires)                                |
| ReservationCancelled  | Booking          | Payment (refund)                                                        |
| MachineStarted        | Simulator        | Machine (IN_USE), Booking (IN_PROGRESS)                                 |
| MachineCycleCompleted | Simulator        | Machine (AVAILABLE), Booking (COMPLETED), Notification ("laundry done") |
| MachineFailure        | Simulator        | Machine (OUT_OF_SERVICE), Booking (cancel and refund), Notification     |
| ReviewSubmitted       | Review           | AI review analysis (option B)                                           |

The required event-driven interaction is PaymentCompleted to Notification. Handlers are safe to run twice, and the event is published only after the payment transaction commits.

# 7. Database Design

PostgreSQL. Ids are BIGINT, money is stored in cents, and times are stored in UTC.

| **Table**              | **Columns**                                                                                                 | **Keys and constraints**                                                                                                                                                                                      |
|------------------------|-------------------------------------------------------------------------------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| users                  | id, name, email, password_hash, role, created_at                                                            | Unique email. role is CUSTOMER or ADMIN.                                                                                                                                                                      |
| sessions               | id, user_id, created_at, expires_at                                                                         | Foreign key to users.                                                                                                                                                                                         |
| machine_types          | id, name, kind, capacity_kg, cycle_minutes, price_cents                                                     | kind is WASHER or DRYER. cycle_minutes \> 0, price_cents \>= 0.                                                                                                                                               |
| machines               | id, name, machine_type_id, status, active, version                                                          | Unique name. Foreign key to machine_types. status is AVAILABLE, IN_USE or OUT_OF_SERVICE. version for optimistic locking.                                                                                     |
| machine_status_changes | id, machine_id, from_status, to_status, reason, changed_by, changed_at                                      | Foreign key to machines. changed_by is null for the simulator.                                                                                                                                                |
| machine_cycles         | id, machine_id, reservation_id, started_at, ended_at, result                                                | Usage records. Unique reservation_id. result is RUNNING, COMPLETED or FAILED.                                                                                                                                 |
| reservations           | id, user_id, machine_id, start_at, end_at, blocked_until, status, amount_cents, hold_expires_at, created_at | Foreign keys to users and machines. start_at \< end_at \<= blocked_until. status is PENDING_PAYMENT, CONFIRMED, IN_PROGRESS, COMPLETED, CANCELLED or EXPIRED. Index on (machine_id, start_at, blocked_until). |
| payments               | id, reservation_id, amount_cents, status, stripe_payment_id, idempotency_key, created_at                    | Foreign key to reservations. Unique idempotency_key. status is SUCCEEDED, FAILED or REFUNDED. No card data.                                                                                                   |
| notifications          | id, user_id, type, message, source_event_id, is_read, created_at                                            | Unique (user_id, source_event_id), so a repeated event adds nothing.                                                                                                                                          |
| reviews                | id, user_id, machine_id, reservation_id, rating, comment, reply_text, replied_by, replied_at, created_at    | Unique reservation_id (one review per reservation). rating between 1 and 5. Foreign keys to users, machines and reservations.                                                                                 |
| review_ai_analyses     | id, review_id, status, sentiment, topics, urgent, attempts, analysed_at                                     | Option B only. Unique review_id. status is PENDING, DONE or FAILED. sentiment is POSITIVE, NEUTRAL or NEGATIVE.                                                                                               |

*Figure 3. Entity-relationship diagram. Mermaid source: [diagrams.md, figure 3](diagrams/diagrams.md).*

## 7.1 Transactions and concurrency

Creating a reservation (the overlap check):

1.  Begin a transaction and lock the machine row (SELECT ... FOR UPDATE).

2.  Look for an active reservation on that machine whose time overlaps the new blocked time (existing start \< new end and existing end \> new start).

3.  If one exists, roll back and answer 409. Otherwise insert the PENDING_PAYMENT reservation and commit.

Concurrent requests for the same machine wait for the lock, then see the new reservation and fail. A unique constraint alone cannot catch overlapping time ranges, which is why the lock is used.

Confirming a payment: the Stripe call happens first, outside the transaction. If it succeeds, one transaction saves the payment and sets the reservation to CONFIRMED, after checking again that the hold has not expired. If anything fails, nothing is saved, and the idempotency key makes the retry safe.

Machine status changes use the version column, so an administrator and the simulator cannot overwrite each other. The second writer gets 409.

# 8. REST API Design

## 8.1 Conventions

- All paths start with /api. Administrator-only paths start with /api/admin.

- JSON with camelCase fields. Money in cents (amountCents). Times in ISO-8601 UTC.

- Status codes: 200 OK, 201 created, 204 no content, 400 invalid input, 401 not signed in, 403 wrong role, 404 not found (also for another user’s data), 409 conflict, 422 payment declined, 429 too many requests, 503 AI unavailable.

- Authentication: a session cookie (HTTP-only). Changing requests also send the CSRF token in the X-CSRF-Token header.

Every error uses one body:

{ "status": 409, "code": "RESERVATION_CONFLICT",

"message": "The machine is already reserved for that time.",

"path": "/api/reservations", "details": \[\] }

## 8.2 Endpoints

| **Method and path**                          | **Auth**         | **Request**                                                  | **Response**                                                                               | **Status codes**             |
|----------------------------------------------|------------------|--------------------------------------------------------------|--------------------------------------------------------------------------------------------|------------------------------|
| POST /api/auth/register                      | None             | name, email, password                                        | id, name, email, role                                                                      | 201, 400, 409                |
| POST /api/auth/login                         | None             | email, password                                              | id, name, role, csrfToken; sets the session cookie                                         | 200, 400, 401, 429           |
| POST /api/auth/logout                        | Signed in        | None                                                         | None                                                                                       | 204, 401                     |
| GET /api/users/me                            | Signed in        | None                                                         | id, name, email, role                                                                      | 200, 401                     |
| GET /api/machines                            | None             | Query: kind, availableAt                                     | List of id, name, kind, capacityKg, priceCents, cycleMinutes, status                       | 200, 400                     |
| GET /api/machines/{id}                       | None             | None                                                         | One machine                                                                                | 200, 404                     |
| GET /api/machines/{id}/availability          | None             | Query: date                                                  | Free start times for that day                                                              | 200, 400, 404                |
| POST /api/reservations                       | Customer         | machineId, startTime                                         | id, status, startTime, endTime, amountCents, holdExpiresAt                                 | 201, 400, 401, 409           |
| GET /api/reservations                        | Customer         | None                                                         | The customer’s reservations                                                                | 200, 401                     |
| GET /api/reservations/{id}                   | Customer (owner) | None                                                         | One reservation with payment status                                                        | 200, 401, 404                |
| DELETE /api/reservations/{id}                | Customer (owner) | None                                                         | None (cancels and refunds)                                                                 | 204, 401, 404, 409           |
| POST /api/payments                           | Customer (owner) | reservationId, paymentMethodId (Stripe test), idempotencyKey | id, status, reservationStatus                                                              | 200, 400, 401, 404, 409, 422 |
| GET /api/payments/{id}                       | Customer (owner) | None                                                         | id, amountCents, status, stripePaymentId, createdAt                                        | 200, 401, 404                |
| GET /api/notifications                       | Signed in        | None                                                         | List of id, type, message, isRead, createdAt                                               | 200, 401                     |
| PATCH /api/notifications/{id}/read           | Signed in        | None                                                         | The notification                                                                           | 200, 401, 404                |
| POST /api/assistant/chat                     | Customer         | message                                                      | reply, machines (id, name, priceCents). Option A                                           | 200, 400, 401, 429, 503      |
| GET /api/machines/{id}/reviews               | None             | None                                                         | averageRating, list of rating, comment, firstName, createdAt, reply                        | 200, 404                     |
| POST /api/reviews                            | Customer         | reservationId, rating, comment                               | The review                                                                                 | 201, 400, 401, 403, 409      |
| PUT /api/admin/reviews/{id}/reply            | Admin            | text                                                         | The review with its reply                                                                  | 200, 400, 401, 403, 404      |
| GET /api/admin/reviews                       | Admin            | Query: machineId, sentiment, urgent                          | List of reviews, with sentiment, topics and urgent (option B)                              | 200, 400, 401, 403           |
| POST /api/admin/machines                     | Admin            | name, machineTypeId                                          | The new machine                                                                            | 201, 400, 401, 403, 409      |
| PUT /api/admin/machines/{id}                 | Admin            | name, machineTypeId                                          | The machine                                                                                | 200, 400, 401, 403, 404, 409 |
| DELETE /api/admin/machines/{id}              | Admin            | None                                                         | None (deactivates)                                                                         | 204, 401, 403, 404, 409      |
| PATCH /api/admin/machines/{id}/status        | Admin            | status, reason                                               | The machine                                                                                | 200, 400, 401, 403, 404, 409 |
| POST /api/admin/machines/{id}/simulate-fault | Admin            | None                                                         | The machine (demo)                                                                         | 200, 401, 403, 404, 409      |
| GET /api/admin/reservations                  | Admin            | Query: status, machineId, from, to                           | List of reservations                                                                       | 200, 400, 401, 403           |
| GET /api/admin/payments                      | Admin            | Query: status, from, to                                      | List of payments                                                                           | 200, 400, 401, 403           |
| GET /api/admin/summary                       | Admin            | Query: from, to                                              | machinesByStatus, reservationsByStatus, completedCycles, paymentsCount, paymentsTotalCents | 200, 400, 401, 403           |

# 9. UML Diagrams

Figure 1 (use cases) is in section 2.1, figure 2 (architecture) in section 5.1 and figure 3 (entity-relationship diagram) in section 7.

*Figure 4. Sequence diagram: sign-in and a protected request. Mermaid source: [diagrams.md, figure 4](diagrams/diagrams.md).*

*Figure 5. State diagram: machine status. Mermaid source: [diagrams.md, figure 5](diagrams/diagrams.md).*

*Figure 6. State diagram: reservation status. Mermaid source: [diagrams.md, figure 6](diagrams/diagrams.md).*

*Figure 7. Sequence diagram: reserve and pay, with conflict and declined payment. Mermaid source: [diagrams.md, figure 7](diagrams/diagrams.md).*

*Figure 8. Sequence diagram: PaymentCompleted event to notification. Mermaid source: [diagrams.md, figure 8](diagrams/diagrams.md).*

# 10. Security

| **Threat or requirement**          | **How it is handled**                                                                                                                                                                                                                                                  |
|------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Stored passwords                   | Only bcrypt hashes are stored. Passwords are never logged or returned.                                                                                                                                                                                                 |
| SQL injection                      | Spring Data JPA and parameterized queries only.                                                                                                                                                                                                                        |
| Invalid input                      | Every request body and parameter is validated on the server (400 with field errors).                                                                                                                                                                                   |
| Unauthorised access                | Every protected endpoint checks the session (401) and role (403) on the server. The user id comes from the session, never from the request. Another user’s data answers 404.                                                                                           |
| Session theft and CSRF             | HTTP-only, SameSite session cookie, a new session id at every sign-in, and a CSRF token header on changing requests.                                                                                                                                                   |
| Password guessing                  | One generic sign-in error and a limit of 5 failed attempts per email in 15 minutes (429).                                                                                                                                                                              |
| Cross-site scripting               | React escapes text by default. Review comments, replies and chat messages are never inserted as raw HTML.                                                                                                                                                              |
| Fake reviews                       | Only a customer with a completed reservation can review, once per reservation.                                                                                                                                                                                         |
| Payment data                       | Card details are typed only into Stripe’s test form. The server stores only the Stripe payment id, amount and status.                                                                                                                                                  |
| Sensitive data in logs             | Logs hold user ids and actions, never passwords, session ids, card data or API keys. Error messages to users stay generic.                                                                                                                                             |
| AI misuse (prompt injection, cost) | The assistant has only read-only tools run as the signed-in customer, a 500-character limit and 20 messages a minute. Review text is passed to the model as data, not instructions, and its answer is checked against allowed values. The API key stays on the server. |

# 11. Plan and Responsibilities

## 11.1 Responsibilities

| **Member**      | **Area**                                      | **Use cases**          | **Also responsible for**                                                             |
|-----------------|-----------------------------------------------|------------------------|--------------------------------------------------------------------------------------|
| Ringo (A)       | Accounts, security, administration views      | UC1, UC8.3             | Security design, CI pipeline, client foundation, assembling and submitting documents |
| Abdelrahman (B) | Machines and simulator                        | UC2, UC5, UC8.1, UC8.2 | Project skeleton, event infrastructure, containers and deployment                    |
| Gurnoor (C)     | Reservations and payments                     | UC3, UC4               | Database migrations, concurrency tests, Stripe test mode                             |
| Joshua (D)      | Notifications, reviews, analytics, AI feature | UC6, UC9, UC10, UC11   | Shared error handling and API documentation, logging and health checks               |

Each pull request is reviewed by another member (A reviews B, B reviews C, C reviews D, D reviews A).

## 11.2 Milestones

| **Milestone**                         | **Date** | **Deliverable**                                                                                                                                                                 |
|---------------------------------------|----------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| M1: Requirements and Design           | Nov 5    | This document                                                                                                                                                                   |
| M2: Core Application                  | Nov 21   | Registration and sign-in, sessions, machine management and availability, reservations with the overlap check, Stripe test payments, REST APIs, database, validation and tests   |
| M3: Integration and Advanced Features | Nov 28   | Full client, simulator, notifications through Kafka, reviews, admin summary, the AI feature, simulator and notification as separate services, Docker, logging and health checks |
| Final submission                      | Dec 5    | Final document, source code, tests and results, API documentation, database scripts, deployment configuration, repository link, AI-use documentation. Demos on Dec 8            |

## 11.3 Development plan

| **Period**      | **Work**                                                                                                                                                                                                           |
|-----------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Nov 6 to 13     | A: sign-in, sessions, roles. B: skeleton, machines, availability. C: reservations and overlap check. D: error handling, API docs setup, notifications table.                                                       |
| Nov 14 to 21    | C: Stripe test payments. B: simulator in the application, admin machine management. A: admin reservation and payment views, role tests. D: "reservation confirmed" notification. All: M2 demo of the booking flow. |
| Nov 22 to 28    | Team confirms the AI option (A or B). B and D: Kafka, separate simulator and notification services, Docker. D: reviews, admin summary and the AI feature. All: client screens.                                     |
| Nov 29 to Dec 5 | Testing, logging, health checks, documentation, AI-use notes, demo rehearsal.                                                                                                                                      |

# 12. Glossary

| **Term**         | **Meaning**                                                                               |
|------------------|-------------------------------------------------------------------------------------------|
| Visitor          | A person who is not signed in.                                                            |
| Customer         | A registered user who reserves and pays for machines.                                     |
| Administrator    | A staff user who manages machines and views reservations, payments and the summary.       |
| Session          | The server’s record that a user is signed in, referenced by a cookie.                     |
| Machine type     | A kind of washer or dryer with a capacity, cycle length and price.                        |
| Blocked time     | The time a reservation holds a machine: the cycle plus 10 minutes for unloading.          |
| Hold             | A PENDING_PAYMENT reservation that keeps the slot for 10 minutes while the customer pays. |
| Overlap          | Two reservations on one machine where each starts before the other ends.                  |
| Simulator        | Software that runs machine cycles and faults in place of real machines.                   |
| Stripe test mode | Stripe’s free testing environment with test cards. No real money moves.                   |
| Idempotency key  | A unique value sent with a payment so a retry cannot charge twice.                        |
| Transaction      | A group of database changes that all succeed or all fail.                                 |
| Row lock         | A database lock that makes competing writes to one row wait their turn.                   |
| Event            | A message that something happened, such as PaymentCompleted, that other modules react to. |
| Modular monolith | One deployable application split into modules with clear boundaries.                      |
| Adapter          | A class that wraps an external system behind an interface we own.                         |
| Review           | A 1 to 5 rating with an optional comment by a customer about a machine they used.         |
| Sentiment        | Whether a review is positive, neutral or negative, as judged by the AI (option B).        |
| Tool call        | The AI model asking the system to run a read-only lookup, such as a machine search.       |
