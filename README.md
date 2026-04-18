# Sporty Test

> Test task for **Sporty Group** by **Dmitrii Makeev**

A Spring Boot service that tracks live sport events and publishes score updates to Kafka in real time.

When an event goes live, the service starts polling an external score source at a fixed interval and publishes each score as an Avro-serialized message to the `score-updates` Kafka topic. Polling stops when the event is marked offline.

## Architecture

```
Client
  │
  ▼
EventController  POST /events/status  {eventId, live: true/false}
  │
  ▼
EventService     schedules / cancels polling task per eventId
  │
  ▼
ScorePublisherService  fetches score via Feign, builds message
  │
  ├──► SportEventClient (Feign)  GET /events/{eventId}/score  ──► event-source (Node.js)
  │
  └──► KafkaScoreProducer  publishes Avro ScoreEvent ──► Kafka topic: score-updates
```

**Services**

| Service | Description |
|---|---|
| `testapp` | Spring Boot application (Java 25) |
| `event-source` | Node.js mock score provider — stores scores per event, increments them over time |
| `kafka` | Confluent Kafka 7.6 in KRaft mode |
| `schema-registry` | Confluent Schema Registry 7.7 for Avro serialization |

## Prerequisites

| Tool | Version |
|---|---|
| Docker | 24+ |
| Docker Compose | v2 (bundled with Docker Desktop) |
| Java JDK | 25 (only needed for local development) |
| Make | any |

## Running

### Full stack (everything in Docker)

```bash
make up
```

Starts Kafka, Schema Registry, event-source and the Spring Boot app.
The app is available at `http://localhost:8080`.

### Infrastructure only + app locally

```bash
make infra       # starts Kafka, Schema Registry, event-source
make app-local   # runs Spring Boot with the dev profile
```

The app connects to infrastructure on `localhost` ports.

### Stop everything

```bash
make down
```

## API

### Mark event as live (start polling)

```bash
curl -X POST http://localhost:8080/events/status \
  -H "Content-Type: application/json" \
  -d '{"eventId": "match-1", "live": true}'
```

### Mark event as offline (stop polling)

```bash
curl -X POST http://localhost:8080/events/status \
  -H "Content-Type: application/json" \
  -d '{"eventId": "match-1", "live": false}'
```

## Watching Kafka messages

```bash
docker exec -it schema-registry kafka-avro-console-consumer \
  --bootstrap-server kafka:29092 \
  --topic score-updates \
  --from-beginning \
  --property schema.registry.url=http://schema-registry:8081
```

## Configuration

The app is configured via environment variables (see `application.yaml`):

| Variable | Description | Default (dev) |
|---|---|---|
| `EVENT_SOURCE_SERVICE_URL` | Base URL of the score provider | `http://localhost:8081` |
| `KAFKA_BOOTSTRAP_SERVERS` | Kafka bootstrap servers | `localhost:9092` |
| `SCHEMA_REGISTRY_URL` | Schema Registry URL | `http://localhost:8094` |
| `KAFKA_SECURITY_PROTOCOL` | Kafka security protocol | `PLAINTEXT` |

Poll interval defaults to `10s` and can be overridden via:
```yaml
spring.application.domain-properties.poll-interval: 5s
```

## Running tests

```bash
cd testapp && ./gradlew test
```

Tests use Testcontainers — Docker must be running. The end-to-end test (`EventE2ETest`) spins up real Kafka and Schema Registry containers and mocks the Feign client with `@MockitoBean`.
