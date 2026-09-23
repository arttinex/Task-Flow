# TaskFlow

A personal daily task manager. Register, sign in, and manage only your own
tasks — full create/read/update/delete, status tracking, priorities, and due
dates — behind a session-based login.

**Stack:** Spring Boot 3.4 · Java 25 · Thymeleaf · Spring Security · MongoDB ·
Caffeine (in-memory cache) · Docker

---

## Features

- Register / login / logout with hashed passwords (BCrypt, strength 12)
- Every task is scoped to its owner at the query level — you cannot load,
  edit, or delete another user's task even by guessing an id
- Full CRUD: create, list (with status filter tabs), edit, delete, and a
  one-click status toggle (Pending → In progress → Done → Pending)
- Per-user in-memory caching of the task list, evicted automatically on any
  write for that user
- CSRF protection on every form, gzip response compression, MongoDB indexes
  on the fields every query actually filters by
- Custom, non-templated UI (warm paper theme, serif headings, restrained
  motion) — not the default Bootstrap/purple-gradient look
- `/actuator/health` for container healthchecks and orchestrators

## Project layout

```
src/main/java/com/taskflow/
  config/       SecurityConfig, CacheConfig
  controller/   AuthController, TaskController, error handling
  dto/          RegisterForm, TaskForm (validated form objects)
  model/        User, Task, Priority, TaskStatus
  repository/   UserRepository, TaskRepository (Spring Data MongoDB)
  security/     CustomUserDetails, CustomUserDetailsService
  service/      UserService, TaskService (caching + ownership checks live here)
src/main/resources/
  templates/    Thymeleaf views (auth/, tasks/, fragments/, error/)
  static/       css/style.css, js/app.js
```

## Run it locally (no Docker)

Requires JDK 25 and a MongoDB instance reachable at `mongodb://localhost:27017`.

```bash
export MONGODB_URI=mongodb://localhost:27017/taskflow
mvn spring-boot:run
```

Visit `http://localhost:8080`, register an account, and you're on your board.

## Run it with Docker (deploy-ready)

```bash
cp .env.example .env        # edit MONGO_PASSWORD before anything real
docker compose up --build -d
docker compose logs -f app  # watch it come up
```

This starts two containers on an isolated network:
- **app** — the Spring Boot jar, built on `eclipse-temurin:25-jre-alpine`
  (JRE-only runtime image, non-root user, healthcheck against
  `/actuator/health`)
- **mongo** — MongoDB 7 with a named volume (`mongo-data`) so data survives
  container restarts; **not** published to the host — only `app` can reach it

Stop everything with `docker compose down` (add `-v` to also drop the Mongo
volume).

### Just the image, no compose

```bash
docker build -t taskflow:latest .
docker run -p 8080:8080 -e MONGODB_URI=mongodb://<host>:27017/taskflow taskflow:latest
```

## Configuration (environment variables)

| Variable          | Default                                   | Purpose                          |
|-------------------|--------------------------------------------|-----------------------------------|
| `MONGODB_URI`     | `mongodb://localhost:27017/taskflow`       | Mongo connection string           |
| `PORT`            | `8080`                                     | HTTP port                         |
| `THYMELEAF_CACHE`  | `true`                                     | Set `false` only for local dev    |
| `JAVA_OPTS`       | *(empty)*                                  | e.g. `-Xms256m -Xmx512m`          |

## Security notes

- Passwords are never stored or logged in plain text (BCrypt only)
- Sessions: fixation-protected (new session id on login), capped at one
  active session per account
- Every task read/write goes through `findByIdAndUserId` /
  `deleteByIdAndUserId` — ownership is enforced in the query, not just in
  application logic, so there's no path that skips the check
- CSRF tokens are injected automatically into every Thymeleaf `th:action`
  form via Spring Security's `RequestDataValueProcessor`

## Caching strategy

`TaskService.getTasksForUser` is `@Cacheable` per user id. Any create,
update, delete, or status toggle for that user calls `@CacheEvict` on the
same key, so the cache can never serve stale data after a write — it's
cache-aside, not time-based staleness. A 10-minute TTL and a 10,000-entry
cap (`CacheConfig`) are a safety net, not the primary invalidation
mechanism.

---

## Performance checklist

Applied at build time, not audited after the fact — mapped to the standard
caching / database / frontend / network checklist:

**Already solid**
- Per-user task list caching (Caffeine, cache-aside with explicit eviction) —
  `TaskService`
- Indexes on `User.username`, `User.email` (unique) and `Task.userId` — every
  query path filters by one of these first, so none of them fall back to a
  collection scan
- No N+1 queries: the task list is one `find` per page load; status counts
  are computed off the already-cached list, not three extra queries
- Gzip compression on HTML/CSS/JS responses over 1KB (`application.yml`)
- MongoDB connection pooling is on by default via the Spring Data MongoDB
  starter (no manual connection handling anywhere in the code)
- CSS/JS are hand-written and small (no bundler, no unused framework weight)
- `/actuator/health` lets a load balancer or orchestrator stop routing
  traffic to an unhealthy instance before users hit it

**Worth doing as the task list grows**
- Add pagination to `TaskRepository`/`TaskService` once a single user's task
  count regularly exceeds a page or two — the query and cache key are
  already user-scoped, so this is additive, not a rework
- Add a CDN in front of `/css` and `/js` if this moves beyond a
  single-region deploy

**Not applicable yet**
- Image compression / lazy loading — the UI ships no images
- Code splitting — a single small CSS/JS pair doesn't need chunking

