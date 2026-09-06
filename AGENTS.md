# AGENTS.md

## Commands

```bash
./mvnw clean compile          # compile only (fast check)
./mvnw test                   # unit tests (*Test.java) via Surefire
./mvnw verify                 # unit + integration tests
./mvnw verify -Dit.test=*IT   # integration tests only via Failsafe
```

**No lint/typecheck step exists.** `compile` is the fastest verification.

Docker is **required** for integration tests — Testcontainers spins up PostgreSQL 18 and Redis 8 automatically.

## Architecture

- **Spring Boot 4.0.7** on **Java 21** (`com.identity.service`)
- **Persistence**: jOOQ codegen + Spring JDBC. Generated code lives in `persistence/generated/` — do not edit manually.
- **Migrations**: Flyway in `src/main/resources/db/migration/`. New tables must be added as numbered migration files.
- **jOOQ codegen** runs against the **local external** PostgreSQL (`localhost:15432`) at `generate-sources` phase. It regenerates `persistence/generated/` classes.
- **Schema currently has 5 tables**: `users`, `password_credentials`, `token_families`, `refresh_tokens`, `confidential_clients`.
- **jOOQ currently only generates** for `users` and `password_credentials` (see `pom.xml` `includes` filter).

## Package Layout

```
com.identity.service
├── domain/              # domain objects
├── health/              # health endpoints
├── persistence/
│   ├── generated/       # jOOQ generated code — DO NOT EDIT
│   └── repository/      # repository interfaces + jOOQ impls
├── security/            # Spring Security config
├── service/             # business logic
└── testcontainers/      # shared Testcontainer definitions
```

## Testing

- Unit tests: `*Test.java` (Surefire)
- Integration tests: `*IT.java` (Failsafe)
- All IT classes extend `AbstractIntegrationTest` which provides `@SpringBootTest(RANDOM_PORT)` + Testcontainers
- Test profile: `@ActiveProfiles("test")`
- External PostgreSQL/Redis are **not** used by tests — Testcontainers provides isolated containers.

## Local Dev

- Copy `.env.example` to `.env` and fill in credentials.
- External services expected: PostgreSQL `localhost:15432`, Redis `localhost:16379`
- `.env` is auto-loaded via `spring.config.import=optional:file:.env[.properties]`
- Health check: `curl http://localhost:8080/health`

## Gotchas

- jOOQ codegen requires the external PostgreSQL to be running. `generate-sources` phase will fail if it's down.
- The `token_families`, `refresh_tokens`, and `confidential_clients` tables exist in the migration but have **no jOOQ generated classes yet** (not in the `includes` filter).
- Redis is a dependency but has no repository layer yet — only the connection config exists.
