# SpinCycle — Smart Laundromat Service Platform

**EECS 4413 · Building E-Commerce Systems · Fall 2026 · Section E · Team 5**

Customers can register, check washer and dryer availability, reserve and pay for machines (simulated with Stripe test mode), watch the machine status live, get notifications, earn loyalty points and leave reviews. Admins can manage machines, maintenance, customers, revenue and analytics. The final version adds an AI-powered feature.

## Team

| Member | GitHub | Primary ownership |
|---|---|---|
| Abdelrahman Hassan | @abdohassan15 | _TBD at kickoff_ |
| Ringo Chui | @RIngochui | |
| _Member 3_ | | |
| _Member 4_ | | |

## Milestones

| Milestone | Focus | Internal target |
|---|---|---|
| M1 | Requirements, architecture, DB and API design, UML | Fri Oct 16 |
| M2 | Core backend: auth, machines, reservations, payments, tests | Sun Nov 1 |
| M3 | Full distributed system: events, services, containers, cloud, AI | Sun Nov 15 (feature freeze) |
| Final | Hardening, test results, docs, demo | **Fri Nov 20** |

The week-by-week plan is in [docs/TIMELINE.md](docs/TIMELINE.md).

## Repository layout (planned)

```
backend/     # API (modular monolith in M2 → services in M3)
frontend/    # SPA client
infra/       # docker-compose, deployment config
docs/        # design document, API docs, UML, test results, AI-use log
```

## Workflow

- `main` is protected. Work on `feature/<linear-id>-short-name` branches and open PRs.
- Every PR gets at least **one teammate review** before it is merged.
- Put the Linear issue ID (e.g. `SPI-12`) in the branch name or PR title so Linear links it automatically.
- Never commit secrets. Use `.env` (it is gitignored) and keep `.env.example` up to date.
- Log meaningful AI-tool usage in `docs/AI_USE.md` as you go. The final report requires it (spec §13).
