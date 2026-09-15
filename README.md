# Queue Management System

Digital take-a-number system for places with physical queues (clinics, public offices, bank counters).
Customers take a ticket from their phone, staff call the next person from a counter screen, a public
display in the waiting room updates in real time, and managers get daily statistics.

Built as a **learning / portfolio project** with a deliberate focus on the backend problems that show up in
real systems: concurrency, caching, rate limiting, event-driven messaging, idempotency, and testing against
real infrastructure.

```
organization → location → service → queue → ticket → call → serve → statistics
```

## Stack

| Layer | Technology |
|---|---|
| Backend | Java 21, Spring Boot 3.5 (Web, Data JPA, Security, WebSocket, AMQP, Cache) |
| Database | PostgreSQL 18, Flyway migrations (no Hibernate auto-DDL) |
| Cache / rate limiting | Redis 7, Spring Cache, Bucket4j |
| Messaging | RabbitMQ 3 (topic exchange, dead-letter queues) |
| Real-time | WebSocket + STOMP |
| Frontend | Angular 20 (signals, standalone components), Chart.js |
| Testing | JUnit 5, Mockito, Testcontainers, Awaitility |
| Ops | Docker multi-stage builds, Docker Compose, GitHub Actions |

## Architecture

Modular monolith — one deployable, modules as packages (`organizations`, `locations`, `servicetypes`,
`queues`, `tickets`, `users`, `notifications`, `analytics`, `common`). Modules talk through Spring beans
inside the process and through RabbitMQ events where decoupling matters.

```
 Angular (customer / employee / public display)
      │ REST + JWT                 │ STOMP over WebSocket
      ▼                            ▼
 ┌─────────────────────────── Spring Boot ───────────────────────────┐
 │  TicketService ── state machine ── @Transactional + row lock       │
 │       │ after commit                                               │
 │       ├──► WebSocket  /topic/queue/{id}                            │
 │       └──► RabbitMQ   exchange ticket.events (topic)               │
 │                 ├── ticket.notifications ──► NotificationListener  │
 │                 └── ticket.analytics     ──► AnalyticsListener     │
 │  Redis: cache for the public display, token buckets for rate limit │
 └────────────────────────────────────────────────────────────────────┘
      │
 PostgreSQL (Flyway)
```

### Ticket state machine

```
WAITING → CALLED → SERVING → COMPLETED
WAITING → CANCELLED
CALLED  → NO_SHOW
CALLED  → SKIPPED → WAITING
```

Every transition is validated in the service layer (`TicketStatus.moze(...)`), never only in the controller —
so the rule holds whether the call comes from HTTP, a WebSocket handler, or a message listener.

### Roles

`CUSTOMER` takes / cancels own tickets · `EMPLOYEE` calls, serves, skips · `MANAGER` creates queues,
views analytics · `ADMIN` everything. JWT in the `Authorization` header, `@PreAuthorize` per endpoint.

## Engineering decisions worth reading

These are the parts of the codebase where the *why* matters more than the *what*. Each one was first
reproduced as a failing test or a live experiment, then fixed.

**Issuing the next number is a race.** Two customers reading `lastNumber` at the same time got the same
ticket. Reproduced with 20 parallel threads (`TicketConcurrencyTest`, deliberately *not* `@Transactional`),
fixed with a pessimistic row lock on the queue (`SELECT … FOR UPDATE`) and `Propagation.MANDATORY` so the
lock can't accidentally live outside the caller's transaction. Both "take a number" and "call next" lock the
same row in the same order, so deadlock is impossible. See `DOKAZ-race-condition.md`.

**Cache eviction happens after commit, not with `@CacheEvict`.** `@CacheEvict` runs while the transaction
is still open; a concurrent reader would re-populate the cache with pre-commit data. Eviction is registered
as a `TransactionSynchronization.afterCommit`. A 30 s TTL is the safety net.

**Redis is not the source of truth.** A `CacheErrorHandler` degrades to the database if Redis is down —
and Lettuce is configured with a 1 s timeout and `REJECT_COMMANDS`, because the default (60 s command
timeout, queue commands while disconnected) turned "graceful degradation" into a one-minute hang.

**Rate limiting is per user, not per IP, and fails open.** A whole waiting room shares one Wi-Fi IP.
Token bucket (5 tickets / minute, greedy refill) stored in Redis so it holds across backend instances.
If Redis is unavailable the limiter lets requests through: it protects against abuse, it is not a security
boundary — for login or payments the decision would be the opposite.

**Events leave the service only after commit.** `TicketEventPublisher.objavi()` publishes an in-process
Spring event; a `@TransactionalEventListener(AFTER_COMMIT)` sends it to WebSocket and RabbitMQ. Neither the
display nor a consumer ever sees a ticket that was rolled back. The remaining gap (commit succeeds, broker
is down) is logged; the outbox pattern is the known next step and is deliberately not implemented.

**Consumers are idempotent.** RabbitMQ delivers at-least-once. Each consumer records `(consumer, eventId)` in
`processed_event` *in the same transaction* as its work (`Propagation.MANDATORY`), so a failed handler rolls
back the marker too and the redelivery is processed as if new. Failed messages retry 3× with backoff, then
land in a per-queue dead-letter queue.

**Analytics is real SQL.** Daily stats use `COUNT(*) FILTER (WHERE …)` and `AVG(EXTRACT(EPOCH FROM …))`
over a `LEFT JOIN` so queues with no tickets still appear. Wait-time estimates use the queue's own 7-day
average service time and fall back to a constant, and the response says which (`basis`).

**Tests run against real infrastructure.** Integration tests extend `AbstractIntegrationTest`, which starts
PostgreSQL, Redis and RabbitMQ with Testcontainers (singleton containers, `@ServiceConnection`). No H2:
`FILTER`, `EXTRACT`, `FOR UPDATE` and `timestamptz` don't behave the same there.

## Running it

### Everything in Docker

```bash
cp .env.example .env        # set POSTGRES_PASSWORD and APP_JWT_SECRET
docker compose up --build
```

- Frontend: http://localhost
- Backend API: http://localhost:8080
- RabbitMQ UI: http://localhost:15672 (guest / guest)

### Local development

```bash
docker start redis rabbitmq          # or docker compose up postgres redis rabbitmq

cd queue-backend/queue-backend
cp src/main/resources/application-local.properties.example src/main/resources/application-local.properties
./mvnw spring-boot:run               # :8080

cd frontend/frontend
npm ci && npx ng serve               # :4200, proxies /api and /ws to :8080
```

Registration always creates a `CUSTOMER`; promote with
`UPDATE app_user SET role = 'MANAGER' WHERE email = '…'`.

### Tests

```bash
cd queue-backend/queue-backend
./mvnw verify                        # needs Docker for Testcontainers
```

## API overview

| Method | Path | Role |
|---|---|---|
| POST | `/api/auth/register`, `/api/auth/login` | public |
| POST | `/api/organizations`, `/api/locations`, `/api/services`, `/api/queues` | MANAGER |
| GET | `/api/queues/{id}/tickets` | public (cached — the waiting-room display) |
| POST | `/api/queues/{id}/tickets` | any user (rate limited) |
| POST | `/api/tickets/{id}/cancel` | owner or staff |
| POST | `/api/queues/{id}/next`, `/api/tickets/{id}/{call,serve,complete,skip,requeue,no-show}` | EMPLOYEE+ |
| GET | `/api/tickets/{id}/estimate` | any user |
| GET | `/api/analytics/today`, `/api/analytics/queues/{id}/hourly` | MANAGER+ |
| WS | `/ws` → `/topic/queue/{id}` | public |

Errors are always `{ "message": "...", "details": [] }` with the appropriate status
(`404`, `409` invalid transition, `403`, `429` + `Retry-After`).

## Project layout

```
queue-backend/queue-backend/   Spring Boot app (src/main/java/com/queue/backend/<module>)
  src/main/resources/db/migration/   Flyway: V1 schema, V2 users, V3 events, V4 ticket owner
frontend/frontend/             Angular app
docker-compose.yml             postgres, redis, rabbitmq, backend, frontend
.github/workflows/ci.yml       build + tests + image build on every push
CLAUDE.md                      working notes: plan by version, lessons learned, open debt
```

## Status

V1 CRUD · V2 security + state machine · V3 WebSocket · V4 locking, Redis cache, rate limiting ·
V5 RabbitMQ + idempotent consumers · V6 analytics + estimation · V7 Docker, Testcontainers, CI — **done**.
Not deployed to a public host yet; `docker compose up` on any VPS is the intended path.
