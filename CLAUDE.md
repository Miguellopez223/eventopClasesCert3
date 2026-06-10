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
- PostgreSQL database `eventop_db` running on `localhost:5432` (user: `postgres`, password: `password`)
- Hibernate `ddl-auto=update` — schema is auto-created/updated from entities
- On first run, a root user is auto-created via `DataInitializer` (username: `root`, password: `Abc123**`, role `ROLE_ROOT`)
- **Sistema 1 must be reachable** at `sistema1.url-base` (default `http://172.16.76.144:8081`). `EventopApplication` is itself a `CommandLineRunner` that authenticates against Sistema 1 on startup; if it's unreachable the startup runner throws.

## Architecture

Three-module layered architecture under `edu.upb.eventop`:

- **`data`** — JPA entities, repositories, DTOs (request + response), and enums. No business logic. Entities use UUID string IDs (`@UuidGenerator`) and extend `AuditableEntity` (auto-populated `createdDate`, `modifiedDate`, `createdBy`, `modifiedBy`, `@Version`). Note: **all DTOs live here**, not in `core`.
- **`core`** — Service layer + external-system integration (`integracion`). Depends on `data`. web/jpa/security deps are `scope=provided` here.
- **`eventop-api`** — Spring Boot application, REST controllers, security config, `DataInitializer`. Depends on `core`. This is the bootable module.

Dependency flow: `eventop-api` → `core` → `data`. Built on Spring Boot 4 starter names (`spring-boot-starter-webmvc`, not `-web`).

## External System Integration (Sistema 1)

`SistemaA` in `core` consumes a peer system ("Sistema 1") via Spring `RestClient`, built with explicit connect/read timeouts (`sistema1.connect-timeout`, `sistema1.read-timeout`) through a `SimpleClientHttpRequestFactory`. `auth()` posts `Sistema1AuthRequest` to `${sistema1.url-base}/api/v1/auth` and returns `Sistema1AuthResponse` (snake_case JSON fields). See `EventopApplication.run()` for the startup auth call.

## API Endpoints

All REST endpoints are under `/api/v1/`:

- `POST /api/v1/auth` — JWT authentication (**only public endpoint** besides swagger/`/error`)
- `GET/POST /api/v1/empresas`, `PUT /api/v1/empresas/{id}` — `EmpresaController` is `@Secured({"ROLE_ADMIN", "ROLE_ROOT"})` (requires JWT + role)
- `GET /api/v1/eventos`, `POST /api/v1/eventos/{empresaId}` — authenticated (any valid JWT)

## Security

- Stateless JWT auth via `JwtTokenFilter` + `JwtTokenProvider`. On validation, `JwtTokenProvider` reloads the `User` from the DB by the `id` claim and sets it as the auth principal. Login returns `OKAuthDto` (snake_case JSON; all three token fields currently hold the same JWT).
- JWT config in `application.properties`: `security.jwt.token.secret-key` and `security.jwt.token.expire-length` (minutes)
- Public endpoints are configured in `WebSecurityConfiguration`; all others require a valid Bearer token
- **Method-level security**: `@EnableMethodSecurity(securedEnabled = true, jsr250Enabled = true)` on the app class; methods/controllers guarded with `@Secured`. Authorities are the raw role enum name (e.g. `ROLE_ROOT`) from `User.getAuthorities()`.
- Password encoding uses Spring's `DelegatingPasswordEncoder` (`{bcrypt}` by default), bean in `InjectConfiguration`
- JPA auditing is enabled (`@EnableJpaAuditing` + `AuditorAware<String>` bean that falls back to `"ADMIN"` when unauthenticated)

## Key Conventions

- Language: Spanish for domain names (entities, fields, error messages), English for technical names (packages, Spring annotations)
- Lombok is used throughout (`@AllArgsConstructor`, `@Builder`, `@Getter/@Setter`, `@Slf4j`)
- DTOs are split into `dto/request/` and `dto/response/` packages in the data module
- Entities map to PostgreSQL tables; `User` maps to `_user` (reserved word avoidance)
- `Eventos` has a `@ManyToOne` relationship to `Empresa` (field is named `materia` but accessors use `empresa`)
- Controllers are `@Controller` returning `ResponseEntity` (except `AuthController`, which is `@RestController`); errors are caught/logged and returned as `internalServerError()`. The authenticated `User` is read from `SecurityContextHolder...getPrincipal()`.
- Input validation is done manually in services (e.g. `StringUtil.isNullOrEmpty`), not via Bean Validation annotations.
- Repositories mix derived queries, `@Query` JPQL with constructor-expression DTO projections, native queries, and `@Procedure` stored-proc calls — see `EventosRepository` / `EmpresaRepository`.
