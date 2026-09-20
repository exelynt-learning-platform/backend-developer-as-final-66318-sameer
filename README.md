# Resource Booking System

A RESTful API for booking shared resources (rooms, vehicles, equipment) built with
Spring Boot 3, Java 17, Spring Security + JWT, and JPA/Hibernate on PostgreSQL or MySQL.

## Features

- JWT-based stateless authentication (`POST /auth/login`)
- Role-based access control — `ADMIN` and `USER`, enforced at both the filter-chain
  level (`SecurityConfig`) and the service level (ownership checks)
- Full CRUD on **resources** (ADMIN) and read-only access (USER)
- Reservation creation, viewing, status transitions, full update, and deletion
- Reservation owner is **always** taken from the JWT principal — never trusted from
  the request body
- Reservation statuses: `PENDING`, `CONFIRMED`, `CANCELLED`
- Filtering reservations by `status`, `minPrice`, `maxPrice`
- Pagination (`page`, `size`) and optional sorting (`sort=field,dir`) on both
  resources and reservations
- Centralized validation + error handling (`GlobalExceptionHandler`)
- Swagger / OpenAPI UI
- Seed `ADMIN` and `USER` accounts + sample resources on startup
- Integration tests covering authentication, RBAC, ownership, filtering, and pagination

## Tech Stack

| Layer          | Choice                                       |
|----------------|-----------------------------------------------|
| Language       | Java 17                                       |
| Framework      | Spring Boot 3.3.4                             |
| Security       | Spring Security 6, JWT (jjwt 0.12.x), BCrypt  |
| Persistence    | Spring Data JPA / Hibernate, PostgreSQL or MySQL |
| Docs           | springdoc-openapi (Swagger UI)                |
| Build          | Maven                                         |
| Test           | JUnit 5, MockMvc, H2 (in-memory)              |

## Project Structure

```
src/main/java/com/sameer/booking/
├── config/          # Security, OpenAPI, and DataSeeder config
├── controller/       # REST controllers (Auth, Resource, Reservation)
├── dto/               # Request/response DTOs (auth, resource, reservation, common)
├── entity/            # JPA entities (User, Resource, Reservation)
├── enums/             # Role, ReservationStatus
├── exception/         # Custom exceptions + GlobalExceptionHandler
├── repository/        # Spring Data JPA repositories
├── security/           # JwtService, JwtAuthFilter, CustomUserDetailsService, SecurityUtils
├── service/            # AuthService, ResourceService, ReservationService
└── specification/      # JPA Specifications for dynamic reservation filtering
```

## Prerequisites

- Java 17+
- Maven 3.8+
- PostgreSQL 13+ **or** MySQL 8+ (or just run against H2 for a quick local test — see below)

## Setup

### 1. Clone and configure environment

Copy `.env.example` to `.env` and adjust values (or export the variables directly,
or pass `-D` system properties — anything Spring's standard property resolution picks up).

```bash
cp .env.example .env
```

Key variables (all have working defaults baked into `application.yml`, but you
should at minimum set your own `JWT_SECRET` and DB credentials):

| Variable               | Description                                   | Default                                  |
|-------------------------|------------------------------------------------|-------------------------------------------|
| `SERVER_PORT`           | HTTP port                                       | `8080`                                     |
| `DB_URL`                | JDBC URL                                        | `jdbc:postgresql://localhost:5432/booking_db` |
| `DB_USERNAME`           | DB username                                     | `postgres`                                  |
| `DB_PASSWORD`           | DB password                                     | `postgres`                                  |
| `DB_DRIVER`             | JDBC driver class                               | `org.postgresql.Driver`                     |
| `DB_DIALECT`            | Hibernate dialect                               | `org.hibernate.dialect.PostgreSQLDialect`   |
| `DDL_AUTO`              | Hibernate schema strategy                       | `update`                                    |
| `SHOW_SQL`              | Log SQL statements                              | `false`                                     |
| `JWT_SECRET`            | Base64 HS256 signing key (256-bit+)             | *(dev default — override in real deployments)* |
| `JWT_EXPIRATION_MS`     | Token lifetime in ms                            | `86400000` (24h)                            |
| `SEED_ENABLED`          | Seed ADMIN/USER + sample resources on boot      | `true`                                      |
| `SEED_ADMIN_USERNAME`   | Seeded admin username                           | `admin`                                     |
| `SEED_ADMIN_PASSWORD`   | Seeded admin password                           | `Admin@123`                                 |
| `SEED_USER_USERNAME`    | Seeded user username                            | `user`                                      |
| `SEED_USER_PASSWORD`    | Seeded user password                            | `User@123`                                  |

### 2. Database setup

**PostgreSQL (default):**

```sql
CREATE DATABASE booking_db;
```

The app will create/update tables automatically on startup (`DDL_AUTO=update`).

**MySQL:**

Run with the `mysql` Spring profile, which points at `application-mysql.yml`:

```bash
export SPRING_PROFILES_ACTIVE=mysql
export DB_USERNAME=root
export DB_PASSWORD=your_password
```

The MySQL connection string uses `createDatabaseIfNotExist=true`, so no manual
`CREATE DATABASE` step is required.

### 3. Run

```bash
mvn spring-boot:run
```

Or build a jar and run it:

```bash
mvn clean package
java -jar target/booking-system-1.0.0.jar
```

The app starts on `http://localhost:8080` (or `SERVER_PORT`).

### 4. Explore the API

- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **OpenAPI JSON**: http://localhost:8080/v3/api-docs
- **Postman**: import `postman_collection.json` — it includes a login request for
  each seeded role that automatically captures the JWT into a collection variable
  for subsequent requests.

## Seed Users

On first startup (with `SEED_ENABLED=true`, the default), two accounts and three
sample resources are created:

| Username | Password    | Role  |
|----------|-------------|-------|
| `admin`  | `Admin@123` | ADMIN |
| `user`   | `User@123`  | USER  |

## Authentication

```
POST /auth/login
Content-Type: application/json

{
  "username": "admin",
  "password": "Admin@123"
}
```

Response:

```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "tokenType": "Bearer",
  "username": "admin",
  "role": "ADMIN",
  "expiresInMs": 86400000
}
```

Send the token on every subsequent request:

```
Authorization: Bearer <token>
```

## API Overview

### Resources — `/api/resources`

| Method | Path                  | ADMIN | USER | Notes                          |
|--------|------------------------|-------|------|----------------------------------|
| GET    | `/api/resources`       | ✅    | ✅   | Paginated list                   |
| GET    | `/api/resources/{id}`  | ✅    | ✅   | Single resource                  |
| POST   | `/api/resources`       | ✅    | ❌   | Create                            |
| PUT    | `/api/resources/{id}`  | ✅    | ❌   | Update                            |
| DELETE | `/api/resources/{id}`  | ✅    | ❌   | Delete                            |

### Reservations — `/api/reservations`

| Method | Path                          | ADMIN         | USER                      | Notes                                                        |
|--------|---------------------------------|----------------|-----------------------------|------------------------------------------------------------------|
| POST   | `/api/reservations`             | ✅             | ✅                          | Owner is taken from the JWT, not the request body                |
| GET    | `/api/reservations`              | ✅ (all)       | ✅ (own only)               | Supports `status`, `minPrice`, `maxPrice`, `page`, `size`, `sort` |
| GET    | `/api/reservations/{id}`         | ✅             | ✅ (own only, else 403)    |                                                                    |
| PUT    | `/api/reservations/{id}`         | ✅             | ❌                          | Full update (resource, times, status, price)                     |
| PATCH  | `/api/reservations/{id}/status`  | ✅ (any status)| ✅ (own reservation, `CANCELLED` only) |                                                       |
| DELETE | `/api/reservations/{id}`         | ✅             | ❌                          |                                                                    |

**Filtering & pagination example:**

```
GET /api/reservations?status=CONFIRMED&minPrice=20&maxPrice=200&page=0&size=10&sort=price,desc
```

## Validation & Error Responses

All errors return a consistent shape:

```json
{
  "timestamp": "2026-08-28T10:15:30",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "path": "/api/reservations",
  "details": ["price: Price must not be negative"]
}
```

| Status | Scenario                                                        |
|--------|-------------------------------------------------------------------|
| 400    | Validation failure, end time before start time, bad price range   |
| 401    | Missing/invalid/expired JWT, bad login credentials                |
| 403    | Authenticated but not authorized (e.g. USER hitting another user's reservation, or a write on `/api/resources`) |
| 404    | Resource/reservation not found                                     |
| 409    | Data integrity violation (e.g. duplicate username)                 |

## Testing

Integration tests run against an in-memory H2 database (`test` Spring profile,
`application-test.yml`) — no external database needed.

```bash
mvn test
```

Coverage includes:
- `JwtServiceTest` — token generation, validation, expiry
- `AuthControllerTest` — login success/failure, validation
- `ResourceControllerTest` — RBAC on all CRUD operations
- `ReservationControllerTest` — ownership enforcement (USER sees only their own
  data, cannot view/delete others'), ADMIN full access, status-transition rules,
  filtering by status/price range, pagination

## Security Notes

- Passwords are hashed with BCrypt (`PasswordEncoder` bean in `SecurityConfig`) —
  never stored or logged in plaintext.
- JWTs are signed with HS256 using a server-side secret (`JWT_SECRET`) — rotate
  this and keep it out of source control in any real deployment (`.env` is
  git-ignored).
- Sessions are stateless (`SessionCreationPolicy.STATELESS`) — no server-side
  session state, the JWT is the only credential.
- Ownership checks for reservations happen in the **service layer**, not just the
  filter chain, so a USER token can never read or modify another user's data
  regardless of the ID passed in the URL.
