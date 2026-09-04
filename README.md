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
