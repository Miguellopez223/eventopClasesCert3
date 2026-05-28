# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Run

This is a multi-module Maven project (Java 17, Spring Boot 4.0.6). Use the Maven wrapper:

```bash
# Build all modules
./mvnw clean install

# Run the API (starts on port 8081)
./mvnw -pl eventop-api spring-boot:run

# Run tests
./mvnw test

# Run tests for a single module
./mvnw -pl core test
./mvnw -pl eventop-api test
```

## Prerequisites

- Java 17
- PostgreSQL database `eventop_db` running on `localhost:5432` (user: `eventop_user`, password in `application.properties`)
- On first run, a root user is auto-created via `DataInitializer` (username: `root`, password: `Abc123**`)

## Architecture

Three-module layered architecture under `edu.upb.eventop`:

- **`data`** — JPA entities, repositories, DTOs, and enums. No business logic. Entities use UUID string IDs (`@UuidGenerator`) and extend `AuditableEntity` (auto-populated `createdDate`, `modifiedDate`, `createdBy`, `modifiedBy`, `version`).
- **`core`** — Service layer. Depends on `data`. Contains business logic and custom exceptions.
- **`eventop-api`** — Spring Boot application, REST controllers, and security config. Depends on `core`. This is the bootable module.

Dependency flow: `eventop-api` → `core` → `data`

## API Endpoints

All REST endpoints are under `/api/v1/`:

- `POST /api/v1/auth` — JWT authentication (public)
- `GET/POST /api/v1/empresas` — List/create empresas (public)
- `PUT /api/v1/empresas/{id}` — Update empresa (public)
- `GET /api/v1/eventos` — List eventos (authenticated)
- `POST /api/v1/eventos/{empresaId}` — Create evento under empresa (authenticated)

## Security

- Stateless JWT auth via `JwtTokenFilter` + `JwtTokenProvider`
- JWT config in `application.properties`: `security.jwt.token.secret-key` and `security.jwt.token.expire-length` (minutes)
- Public endpoints are configured in `WebSecurityConfiguration`; all others require a valid Bearer token
- Password encoding uses Spring's `DelegatingPasswordEncoder`

## Key Conventions

- Language: Spanish for domain names (entities, fields, error messages), English for technical names (packages, Spring annotations)
- Lombok is used throughout (`@AllArgsConstructor`, `@Builder`, `@Getter/@Setter`, `@Slf4j`)
- DTOs are split into `dto/request/` and `dto/response/` packages in the data module
- Entities map to PostgreSQL tables; `User` maps to `_user` (reserved word avoidance)
- `Eventos` has a `@ManyToOne` relationship to `Empresa` (field is named `materia` but accessors use `empresa`)
