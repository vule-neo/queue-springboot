# CLAUDE.md

Ovaj fajl daje Claude-u (Claude Code ili chat) kontekst o projektu kad god ga pitam za pomoć.

## Projekat

**Queue Management System** — sistem za digitalizaciju fizičkog čekanja (npr. dom zdravlja, šalteri). Portfolio/learning projekat, cilj je backend znanje za AI/backend intervjue, ne startup.

Lifecycle: `organizacija → lokacija → usluga → šalter → red → ticket → pozivanje → obrada → statistika`

## Arhitektura

- **Modular monolith** u Spring Bootu — NE microservices. Moduli kao paketi: `auth`, `organizations`, `queues`, `tickets`, `counters`, `notifications`, `analytics`.
- Frontend: Angular, tri odvojena interfejsa (customer / employee / public display).
- Baza: PostgreSQL + Flyway migracije (nema Hibernate auto-ddl u produkciji).
- Redis: caching (public display) + rate limiting.
- RabbitMQ: event-driven komunikacija (`TicketCreated`, `TicketCalled`, `TicketCompleted`, `TicketCancelled`).
- WebSocket (STOMP): real-time update za customer i public display ekrane.

## Ticket state machine

```
WAITING → CALLED → SERVING → COMPLETED
WAITING → CANCELLED
CALLED → NO_SHOW
CALLED → SKIPPED → WAITING
```

Nema preskakanja stanja. Svaki prelaz mora biti validiran u servisu, ne samo u kontroleru.

## Uloge

- `CUSTOMER` — uzima ticket, gleda svoj status, otkazuje
- `EMPLOYEE` — otvara counter, poziva sljedećeg, završava/skipuje
- `MANAGER` — kreira queue/counter, upravlja zaposlenima, gleda statistiku
- `ADMIN` — sve

JWT + `@PreAuthorize` po ulozi.

## Plan rada — detaljno po verzijama

Radim inkrementalno, svaka verzija je funkcionalna demo aplikacija. **Ne preskačem verzije** — tehnologija ulazi tek kad postoji konkretan problem koji rješava.

### V1 — CRUD Skeleton

Redoslijed:
1. Setup projekta (Spring Initializr: Web, JPA, PostgreSQL, Validation, Flyway, Lombok) + Angular CLI
2. Baza + Flyway — prva migracija (`V1__init.sql`): `organization`, `location`, `service`, `queue`, `ticket`. Bez `ddl-auto: update`.
3. Entiteti + relacije, redom: `Organization → Location → Service → Queue → Ticket`
4. Repository sloj (Spring Data JPA)
5. Service sloj — kreiranje ticketa, status = `WAITING` (bez state machine logike još)
6. Controller sloj — `POST /organizations`, `POST /locations`, `POST /services`, `POST /queues/{id}/tickets`, `GET /queues/{id}/tickets`
7. DTO-ovi + mapping (ne vraćati entitete direktno)
8. Exception handling — `@ControllerAdvice`
9. Angular — forma "uzmi broj" + lista reda, bez stila, fokus na funkcionalnost

Znanje: Spring Boot osnove (`@RestController`, `@Service`, `@Repository`, DI), JPA/Hibernate (`@Entity`, relacije, lazy/eager, N+1 osnovno), Flyway konvencije, REST/DTO pattern, Angular osnove (`HttpClient`, forme, routing).

### V2 — Security + Ticket Lifecycle

Redoslijed:
1. Spring Security setup (`SecurityFilterChain`, `UserDetailsService`, password encoder)
2. User entitet + `POST /auth/register`, `POST /auth/login`
3. JWT generisanje/validacija (filter čita token, postavlja `SecurityContext`)
4. Role-based authorization (`@PreAuthorize`)
5. Ticket state machine — enum `TicketStatus` + validacija prelaza u servisu
6. `POST /queues/{id}/next` (bez lockinga — to je V4)
7. Angular employee dashboard — login, route guards, dugme "pozovi sljedećeg"

Znanje: Spring Security arhitektura (filter chain, autentikacija vs autorizacija), JWT struktura i potpisivanje (`jjwt`), state machine pattern (čista logika, bez biblioteke), Angular HTTP interceptor + route guards.

### V3 — WebSocket / Real-time

Redoslijed:
1. WebSocket config (`@EnableWebSocketMessageBroker`, STOMP endpoint `/ws`)
2. Topic struktura (`/topic/queue/{queueId}`)
3. Broadcast na promjenu statusa iz servisa
4. Angular WebSocket klijent (`@stomp/stompjs` / `sockjs-client`)
5. Customer ekran real-time update
6. Public display ekran (poseban route, isti topic, drugi UI)

Znanje: STOMP protokol osnove, Spring `SimpMessagingTemplate`, RxJS (`Observable`, `Subject`).

### V4 — Concurrency + Redis

Redoslijed:
1. Reprodukuj problem prvo — test sa 20 paralelnih zahtjeva na "pozovi sljedećeg", dokaži duplikat prije fixa
2. Pesimistic locking (`@Lock(LockModeType.PESSIMISTIC_WRITE)`)
3. Pravilne `@Transactional` granice
4. Ponovi test, dokaži fix
5. Redis cache za public display (`@Cacheable`)
6. Rate limiting na `POST /tickets` (Bucket4j + Redis)

Znanje: transaction isolation levels, optimistic vs pesimistic locking, Redis osnove (`RedisTemplate` / Spring Cache), concurrent testing (`ExecutorService`, `CountDownLatch`). Najteži konceptualni dio — ne žuriti.

### V5 — RabbitMQ + Notifications

Redoslijed:
1. RabbitMQ setup (exchange, queue, binding — Spring AMQP)
2. Event objekti (`TicketCreated`, `TicketCalled`, itd. kao DTO)
3. Producer u `TicketService` nakon promjene statusa
4. Consumer u `notifications` paketu (`@RabbitListener`) — log ili email
5. Analytics consumer — drugi listener, bilježi u `analytics` tabelu

Znanje: message broker osnove (exchange types, binding), Spring AMQP (`RabbitTemplate`, `@RabbitListener`), idempotency (šta ako se event obradi dvaput).

### V6 — Analytics + Estimation

Redoslijed:
1. `QueueEvent` audit tabela (who/what/when), ako već ne postoji
2. Wait time V1 — jednostavna formula, on-the-fly
3. Manager dashboard endpoints — agregacije (`GET /analytics/today`)
4. Wait time V2 — istorija po service-u/danu, prosjek umjesto konstante
5. Angular grafikoni (Chart.js / ngx-charts)

Znanje: SQL agregacije (`GROUP BY`, `AVG`, `COUNT`) ili JPQL/Criteria ekvivalent, osnovna statistika (prosjek, rolling average), chart biblioteka po izboru.

### V7 — Docker + Testing + CI/CD

Redoslijed:
1. Dockerfile za backend (multi-stage) i frontend
2. `docker-compose.yml` — backend, frontend, postgres, redis, rabbitmq + env varijable
3. Unit testovi — `QueueService`, `TicketService` (Mockito)
4. Integration testovi — Testcontainers (pravi Postgres/Redis/RabbitMQ)
5. Concurrency test iz V4 formalizovan kao pravi JUnit test
6. GitHub Actions — build + test na svaki push/PR
7. Deploy (Railway/Render ili VPS)

Znanje: Docker osnove (image vs container, multi-stage build, compose networking), JUnit 5 + Mockito, Testcontainers koncept (zašto bolje od H2), GitHub Actions YAML osnove.

## Kako mi Claude treba da pomaže

- Kad pitam za kod, drži se trenutne verzije (V1–V7) na kojoj radim — ne predlaži prečice iz kasnijih verzija ako nisam tamo stigao.
- Kod komentariši minimalno, objasni *zašto* (npr. zašto pesimistic lock a ne optimistic za ovaj slučaj), ne samo *šta*.
- Kad je u pitanju concurrency/locking/transactions — budi eksplicitan oko trade-offova, to mi je najbitniji dio za intervjue.
- Ne piši mi cijele module odjednom bez da pitaš — radije jedan entitet/servis/endpoint po put, da mogu pratiti i sam kucati dio koda.
- Ako nešto može da se uradi na više načina (npr. DTO mapping, exception handling stil), predloži jedan pristup i kratko objasni zašto, umjesto tri opcije bez preporuke.
- Testove tretiraj kao dio zadatka, ne kao naknadnu misao — pogotovo concurrency test iz V4.

## Stack (za referencu)

Spring Boot, Spring Security (JWT), Spring Data JPA, PostgreSQL, Flyway, Redis, RabbitMQ, WebSocket (STOMP), Angular, Docker/Docker Compose, JUnit + Mockito + Testcontainers.
---

# Trenutno stanje (15.09.2026.)

Ovaj dio se dopisuje kako projekat napreduje — čitaj ga prvo.

## Dokle smo stigli

```
✅ V1  CRUD skeleton              ✅ V3  WebSocket real-time
✅ V2  Security + JWT + state machine    ✅ V4  Concurrency + Redis (locking, keš, rate limit)
✅ V5  RabbitMQ    ⬜ V6  Analytics    ⬜ V7  Docker + testovi + CI/CD
```

Git: `main` — `4e4f845` V1–V3, `f870362` V4 locking, `dffe100` V4 Redis keš,
`db6923c` V4 rate limiting, `387ca87` V5 RabbitMQ.

## Sljedeći korak

**V6 Analytics.** Sirovina već postoji: `ticket_event_log` (V5 analytics
consumer puni jedan red po događaju, sa `occurred_at` i `status`). Korak 1 iz
plana (`QueueEvent` audit tabela) je time gotov. Ide: wait time V1 (formula
on-the-fly) → `GET /analytics/today` (JPQL agregacije nad `ticket_event_log`)
→ wait time V2 (prosjek iz istorije) → Angular grafikoni.

## Docker — šta mora biti upaljeno

Docker Desktop je instaliran **po korisniku**
(`%LOCALAPPDATA%\Programs\DockerDesktop\Docker Desktop.exe`), ne u Program
Files. Ako `docker ps` javi "cannot find the file specified" → Desktop nije
pokrenut, ne fali instalacija. Kontejneri:

```bash
docker start redis        # 6379 — keš + rate limit; app radi i bez njega (sporije, bez limita)
docker start rabbitmq     # 5672 AMQP, 15672 web UI (guest/guest); app radi i bez njega (eventi se gube)
```

RabbitMQ kontejner ima named volume `rabbitmq_data`. Prvi `docker run` je pao
sa `.erlang.cookie: eacces` (fajl root-ov, proces `rabbitmq`) — popravljeno
jednokratno sa `chown rabbitmq:rabbitmq` u volume-u. Ako se ikad pravi novi
kontejner, isto ponoviti.

## Kako se pokreće

```powershell
# backend (port 8080)
cd queue-backend\queue-backend ; .\mvnw.cmd spring-boot:run

# frontend (port 4200, proxy na 8080)
cd frontend\frontend ; npx ng serve
```

- Baza: PostgreSQL 18, `queue_db`, korisnik `postgres`. `psql` nije na PATH-u:
  `C:\Program Files\PostgreSQL\18\bin\psql.exe`
- Tajne (lozinka baze, JWT ključ) su u `application-local.properties` —
  **nije u gitu**, šablon je `application-local.properties.example`
- Test korisnik: `marko@test.com` / `tajnalozinka`, uloga `MANAGER`
- Uloge se mijenjaju ručno: `UPDATE app_user SET role='EMPLOYEE' WHERE email=...`
  (registracija uvijek daje `CUSTOMER`)

## Naučeno na teži način — ne ponavljati

- **`mvnw clean compile`** kad se ponašanje ne poklapa sa kodom. Inkrementalni
  build zna vratiti `BUILD SUCCESS` nad starim `.class` fajlovima.
- **`/error` mora biti `permitAll`.** `sendError()` okida ERROR dispatch koji
  ponovo prolazi kroz security lanac; bez toga 403 bude pregažen sa 401.
- **Concurrency test ne smije imati `@Transactional`** — sve bi bilo u jednoj
  transakciji i trka se ne bi reprodukovala.
- **Lock van transakcije ne vrijedi ništa** — otud `propagation = MANDATORY`
  na `QueueService.getEntityForUpdate`.
- Port 8080 / 4200 ostaju zauzeti ako se stara instanca ne ugasi.
- **Lettuce podrazumijevano čeka 60s** i dok je veza pukla *stavlja komande u
  red*. "Graciozno degradiranje" preko `CacheErrorHandler`-a je bilo 60s
  visenja po zahtjevu. Treba `spring.data.redis.timeout=1s` +
  `DisconnectedBehavior.REJECT_COMMANDS`.
- **`CacheErrorHandler` štiti samo anotacije.** Direktan `cache.evict()` iz
  `afterCommit` ide mimo njega → pad Redisa je vraćao 500 za ticket koji je
  *već commitovan*. Svaki ručni poziv na `Cache` treba svoj try/catch.
- **Keš se briše poslije commita**, ne `@CacheEvict`-om: inače konkurentni
  čitač u prozoru prije commita vrati staro stanje u keš.
- Gasi Redis (`docker stop redis`) i probaj **i POST, ne samo GET** — bug
  iznad se vidio tek na POST-u.
- **Rabbit šalje poslije commita**, ne iz servisa direktno:
  `objavi()` → Spring event → `@TransactionalEventListener(AFTER_COMMIT)` →
  WebSocket + Rabbit. Inače ekran/potrošač vide ticket koji je rollbackovan.
- **`IdempotencyGuard` marker mora biti u istoj transakciji kao obrada**
  (`MANDATORY`). Sa `REQUIRES_NEW` bi marker ostao poslije neuspjele obrade i
  event bi bio izgubljen.
- **`event_id` je `VARCHAR(36)`** — tačno UUID. Test sa prefiksom `"test-"`
  je pao na dužini, poruka otišla u DLQ. Dobar dokaz da DLQ radi, loš test.
- Management API (`:15672/api`) brojače osvježava sa ~1s zakašnjenja — ne
  zaključivati "poruka nije stigla" odmah poslije slanja.

## Dug koji stoji

1. **`ticket.user_id` ne postoji** → `cancel` dozvoljava otkazivanje tuđeg
   ticketa. Treba migracija `V3__ticket_user.sql` (nullable FK). Sigurnosna rupa.
2. **Nema README-a** — za portfolio je to prvo što se otvori.
3. `pom.xml.boot4.bak` je ostatak, može se obrisati.
4. Testni podaci u `queue_db` pomiješani sa pravim.
5. `skip` i `requeue` su dvije operacije (`CALLED→SKIPPED`, `SKIPPED→WAITING`) —
   odluka je li ih spojiti u jednu nije donesena.

## Stil rada koji je funkcionisao

- Prvo dokaži problem testom, pa ga rješavaj (V4 `DOKAZ-race-condition.md`).
- Kod se piše modul po modul, vertikalno (entitet → repo → servis → DTO →
  kontroler → curl test), ne sloj po sloj kroz sve entitete.
- Provjera svake promjene stvarnim pokretanjem i curl-om, ne "trebalo bi da radi".
