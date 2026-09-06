# identity-service

## Local Development

### Prerequisites

- Java 21+
- Maven (or use `./mvnw`)
- External PostgreSQL on `localhost:15432`
- External Redis on `localhost:16379`

### Configuration

Copy and edit the environment file:

```bash
cp .env.example .env
```

### Run

```bash
./mvnw spring-boot:run
```

The application automatically loads `.env` via Spring Boot's config import.

### Verify

```bash
curl http://localhost:8080/health
curl http://localhost:8080/health/db
```

## Testing

### Prerequisites

* Docker must be running. Testcontainers starts isolated PostgreSQL 18 and Redis 8 containers automatically.

### Commands

```bash
./mvnw clean compile
./mvnw test
./mvnw verify
./mvnw verify -Dit.test=*IT
```

* `./mvnw clean compile` — Cleans build artifacts and compiles source code.
* `./mvnw test` — Runs unit tests (`*Test.java`) through Surefire.
* `./mvnw verify` — Runs unit and integration tests.
* `./mvnw verify -Dit.test=*IT` — Runs integration tests (`*IT.java`) through Failsafe.

Integration tests use Testcontainers to start isolated PostgreSQL 18 and Redis 8 containers and run Flyway migrations against a real PostgreSQL database. They do not depend on the external PostgreSQL or Redis instances used by local development.

### Test Conventions

* Unit tests: `*Test.java`
* Integration tests: `*IT.java`
* Integration tests use `AbstractIntegrationTest` as the shared Spring Boot test foundation.
* Integration tests use `RANDOM_PORT`.

## Docker

### First Run / After Code Changes

Rebuild and restart (detects code changes automatically):

```bash
docker compose up --build -d
```

### View Logs

```bash
docker compose logs -f
```

### Stop

```bash
docker compose down
```

### Clean Rebuild (This Project Only)

Remove this project's containers and locally-built images, then rebuild:

```bash
docker compose down --rmi local
docker compose up --build -d
```

### Check Status

```bash
docker compose ps
```

### Verify

```bash
curl http://localhost:8080/health
curl http://localhost:8080/health/db
```
