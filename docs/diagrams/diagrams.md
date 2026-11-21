# SpinCycle M1 diagrams (Mermaid sources)

Paste each block (without the backticks) into https://mermaid.live and export a PNG. Figure numbers match the M1 document.

## Figure 1. Use-case diagram

```mermaid
flowchart LR
  Visitor(["Visitor"])
  Customer(["Customer"])
  Admin(["Administrator"])
  Sim(["Simulator"])
  subgraph SYS["SpinCycle"]
    uc11("UC1.1 Register")
    uc12("UC1.2 Sign in")
    uc13("UC1.3 Sign out")
    uc21("UC2.1 Search and select a machine")
    uc31("UC3.1 Reserve a machine")
    uc32("UC3.2 View my reservations")
    uc33("UC3.3 Cancel a reservation")
    uc41("UC4.1 Pay for a reservation")
    uc42("UC4.2 View payment status")
    uc51("UC5.1 Simulate a machine cycle")
    uc61("UC6.1 Receive a notification")
    uc62("UC6.2 View my notifications")
    uc81("UC8.1 Manage machines")
    uc82("UC8.2 Change machine status")
    uc83("UC8.3 View reservations and payments")
    uc91("UC9.1 View admin summary")
    uc101("UC10.1 Option A: AI assistant")
    uc102("UC10.2 Option B: AI review analysis")
    uc111("UC11.1 Submit a review")
    uc112("UC11.2 Browse reviews")
    uc113("UC11.3 Reply to a review")
  end
  Visitor --> uc11
  Visitor --> uc21
  Customer --> uc12
  Customer --> uc13
  Customer --> uc21
  Customer --> uc31
  Customer --> uc32
  Customer --> uc33
  Customer --> uc41
  Customer --> uc42
  Customer --> uc62
  Customer --> uc101
  Customer --> uc111
  Customer --> uc112
  Visitor --> uc112
  Admin --> uc12
  Admin --> uc13
  Admin --> uc81
  Admin --> uc82
  Admin --> uc83
  Admin --> uc91
  Admin --> uc113
  Admin --> uc102
  Sim --> uc51
  uc51 -.->|triggers| uc61
  uc31 -.->|include| uc41
  uc101 -.->|extends to| uc31
  uc111 -.->|triggers| uc102
```

## Figure 2. Architecture overview

```mermaid
flowchart TB
  Client["Client tier: React single-page application"]
  subgraph APP["Application tier: Spring Boot modular monolith (controller, service, repository)"]
    direction LR
    identity["Identity (A)"]
    machine["Machine (B)"]
    booking["Booking (C)"]
    payment["Payment (C)"]
    analytics["Analytics (D)"]
    review["Review (D)"]
    assistant["AI feature (D): assistant or review analysis"]
  end
  DB[("PostgreSQL")]
  Kafka{{"Kafka (Milestone 3)"}}
  Sim["Simulator service (B)"]
  Notif["Notification service (D)"]
  Stripe["Stripe test mode"]
  LLM["Language model API"]
  Client -->|"HTTP, REST, JSON"| APP
  APP -->|"JPA, parameterized SQL"| DB
  APP -->|"publish events"| Kafka
  Sim -->|"machine events"| Kafka
  Kafka -->|"subscribe"| Notif
  Kafka -->|"subscribe"| APP
  Notif --> DB
  payment -->|"payment adapter"| Stripe
  assistant -->|"LLM adapter"| LLM
```

## Figure 3. Entity-relationship diagram

```mermaid
erDiagram
  users ||--o{ sessions : has
  users ||--o{ reservations : books
  users ||--o{ notifications : receives
  machine_types ||--o{ machines : "type of"
  machines ||--o{ reservations : "booked for"
  machines ||--o{ machine_status_changes : history
  machines ||--o{ machine_cycles : runs
  reservations ||--o{ payments : "paid by"
  reservations ||--o| machine_cycles : "used in"
  users ||--o{ reviews : writes
  machines ||--o{ reviews : "reviewed in"
  reservations ||--o| reviews : "reviewed by"
  reviews ||--o| review_ai_analyses : "analysed as"
  users {
    bigint id PK
    string name
    string email UK
    string password_hash
    string role
  }
  sessions {
    string id PK
    bigint user_id FK
    timestamp expires_at
  }
  machine_types {
    bigint id PK
    string name
    string kind
    int capacity_kg
    int cycle_minutes
    int price_cents
  }
  machines {
    bigint id PK
    string name UK
    bigint machine_type_id FK
    string status
    boolean active
    int version
  }
  machine_status_changes {
    bigint id PK
    bigint machine_id FK
    string from_status
    string to_status
    string reason
    timestamp changed_at
  }
  machine_cycles {
    bigint id PK
    bigint machine_id FK
    bigint reservation_id UK
    timestamp started_at
    timestamp ended_at
    string result
  }
  reservations {
    bigint id PK
    bigint user_id FK
    bigint machine_id FK
    timestamp start_at
    timestamp end_at
    timestamp blocked_until
    string status
    int amount_cents
    timestamp hold_expires_at
  }
  payments {
    bigint id PK
    bigint reservation_id FK
    int amount_cents
    string status
    string stripe_payment_id
    string idempotency_key UK
  }
  reviews {
    bigint id PK
    bigint user_id FK
    bigint machine_id FK
    bigint reservation_id UK
    int rating
    string comment
    string reply_text
  }
  review_ai_analyses {
    bigint id PK
    bigint review_id UK
    string status
    string sentiment
    boolean urgent
  }
  notifications {
    bigint id PK
    bigint user_id FK
    string type
    string message
    string source_event_id
    boolean is_read
  }
```

## Figure 4. Sign-in and a protected request

```mermaid
sequenceDiagram
  participant C as Client
  participant API as Identity API
  participant DB as Database
  C->>API: POST /api/auth/login (email, password)
  API->>DB: find user by email
  DB-->>API: password_hash, role
  alt password matches
    API->>DB: create session
    API-->>C: 200, session cookie, csrfToken
  else wrong email or password
    API-->>C: 401 generic error
  end
  C->>API: GET /api/admin/summary (cookie)
  API->>DB: look up session and role
  alt no or expired session
    API-->>C: 401
  else role is CUSTOMER
    API-->>C: 403
  else role is ADMIN
    API-->>C: 200 summary
  end
```

## Figure 5. Machine status

```mermaid
stateDiagram-v2
  [*] --> AVAILABLE: machine added
  AVAILABLE --> IN_USE: reservation starts (MachineStarted)
  IN_USE --> AVAILABLE: cycle ends (MachineCycleCompleted)
  IN_USE --> OUT_OF_SERVICE: simulated fault (MachineFailure)
  AVAILABLE --> OUT_OF_SERVICE: fault or administrator
  OUT_OF_SERVICE --> AVAILABLE: administrator
```

## Figure 6. Reservation status

```mermaid
stateDiagram-v2
  [*] --> PENDING_PAYMENT: reservation created
  PENDING_PAYMENT --> CONFIRMED: payment succeeds
  PENDING_PAYMENT --> EXPIRED: 10 minutes without payment
  PENDING_PAYMENT --> CANCELLED: customer cancels
  CONFIRMED --> IN_PROGRESS: machine starts
  CONFIRMED --> CANCELLED: customer cancels or machine fault (refund)
  IN_PROGRESS --> COMPLETED: cycle ends
  IN_PROGRESS --> CANCELLED: machine fault (refund)
  EXPIRED --> [*]
  CANCELLED --> [*]
  COMPLETED --> [*]
```

## Figure 7. Reserve and pay

```mermaid
sequenceDiagram
  participant U as Customer
  participant B as Booking API
  participant DB as Database
  participant P as Payment API
  participant S as Stripe test mode
  U->>B: POST /api/reservations (machineId, startTime)
  B->>DB: lock machine row, check overlap
  alt overlap
    B-->>U: 409 conflict
  else free
    B->>DB: insert PENDING_PAYMENT, hold 10 min
    B-->>U: 201 reservation, amountCents
    U->>P: POST /api/payments (reservationId, test method, idempotency key)
    P->>DB: check owner, status, hold
    P->>S: create and confirm payment
    alt approved
      S-->>P: succeeded
      P->>DB: one transaction: payment SUCCEEDED, reservation CONFIRMED
      P-->>U: 200 confirmed
    else declined
      S-->>P: declined
      P->>DB: payment FAILED, reservation stays PENDING_PAYMENT
      P-->>U: 422 declined, retry
    end
  end
```

## Figure 8. PaymentCompleted to notification

```mermaid
sequenceDiagram
  participant P as Payment module
  participant DB as Database
  participant K as Kafka
  participant N as Notification service
  participant U as Customer
  P->>DB: commit payment and CONFIRMED reservation
  P->>K: publish PaymentCompleted
  K->>N: PaymentCompleted
  N->>DB: insert notification (unique per user and event)
  alt duplicate event
    N->>N: ignore
  end
  U->>N: GET /api/notifications
  N-->>U: "Reservation confirmed"
```
