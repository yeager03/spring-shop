# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

Maven wrapper is `./mvnw` (`mvnw.cmd` on Windows). Java 21, Spring Boot 4.1.0.

- Build: `./mvnw clean package`
- Run the app: `./mvnw spring-boot:run` (serves on `http://localhost:8080/api/v1`)
- All tests: `./mvnw test`
- Single test class: `./mvnw test -Dtest=ProductServiceTest`
- Single test method: `./mvnw test -Dtest=ProductControllerTest#getProduct_shouldReturn404_whenProductDoesNotExist`
- Local infra (Postgres, Redis + UI, MinIO + bucket init): `docker compose -f docker-compose.development.yml up -d`

Both the app and docker-compose read `.env` (copy from `.env.example`). The app loads it via
`spring.config.import: optional:file:./.env`. There is no lint step configured beyond the compiler.

## Runtime dependencies

The app will not start without:

- **PostgreSQL** — Flyway runs migrations on boot; JPA is `ddl-auto: validate`, so the schema must match entities
  exactly.
- **MinIO / S3-compatible storage** — `StorageService` (AWS SDK v2) uploads product images and user avatars to bucket
  `shop`. `docker-compose` provisions the bucket with public-read via `minio-init`.
- `JWT_SECRET` must be **Base64-encoded**; `JwtConfig` base64-decodes it into an HS256 `SecretKey`.

Redis is in `docker-compose` but not yet wired into the application.

## Architecture

### Feature-package layout

Code is organized by feature under `com.yeager.shop.<feature>`, each with `entity/`, `dto/`, `service/`, `repository/`,
`controller/`. Features: `catalog` (products, categories, images), `cart`, `order`, `authentication`, `user`.
Cross-cutting code lives in `common/` (`config/`, `exception/`, `security/`, `storage/`, `dto/`). Some features add
`converter/` (query-param enum binding), `security/`, `repository/projection/`, `repository/specification/`.

### Authentication & authorization

Stateless JWT via Spring Security OAuth2 **Resource Server** (HS256), configured in
`authentication/security/JwtConfig.java` and `common/config/SecurityConfig.java`.

- **Access token** (15m): carries `sub` (userId), `role`, `authentication_version`. `JwtConfig`'s custom converter maps
  the `role` claim to a `ROLE_`-prefixed authority and builds an `AuthenticatedUserPrincipal`. Controllers get the
  current user via `@AuthenticationPrincipal AuthenticatedUserPrincipal principal` → `principal.getUserId()`.
- **Refresh token** (30d): opaque token, hashed in the `sessions` table, delivered as an HttpOnly cookie scoped to
  `/api/v1/authentication`. `AuthenticationService.refreshTokens` does **rotation with reuse detection** — a
  replayed/revoked token cascades revocation of the whole session chain and raises `RefreshTokenReuseException`.
- **Global invalidation**: `AuthenticationVersionValidator` (a custom `OAuth2TokenValidator<Jwt>`) rejects any access
  token whose `authentication_version` claim ≠ the user's current DB value. Password change bumps
  `authentication_version` and revokes active sessions — this logs the user out everywhere.
- Roles: `CUSTOMER` (default), `MANAGER`, `ADMIN`. Route rules in `SecurityConfig`: catalog reads are public; catalog
  writes require `MANAGER`/`ADMIN`; everything else needs authentication.

### Error handling

`common/exception/GlobalExceptionHandler` (`@RestControllerAdvice`) maps domain exceptions (`ResourceNotFoundException`,
`ResourceAlreadyExistsException`, `InvalidOperationException`, `InvalidCredentialsException`, `StorageException`,
`RefreshTokenReuseException`) and framework exceptions to RFC 7807 `ProblemDetail` responses. Validation failures return
a `ProblemDetail` with an `errors` array of `{field, message}`.

### Validation messages

All Bean Validation messages are externalized in `src/main/resources/messages.properties` and referenced by key in
annotations, e.g. `@Positive(message = "{product.common.id.positive}")`. The exception handler resolves them through
`MessageSource` + `LocaleContextHolder`, so add new messages there rather than inlining strings. `typeMismatch.*` keys
handle query-param binding errors.

### Catalog specifics

Public product/category endpoints look up by **slug**; management endpoints use **numeric id**. Slugs are normalized (
trim + lowercase) and format-validated with regex `^[a-z0-9]+(?:-[a-z0-9]+)*$`. Product listing/filtering is built with
JPA `Specification` (`catalog/repository/specification/ProductSpecifications`) plus projections (
`repository/projection/`) to fetch main images in one query. Paged results use `common/dto/PagedResponse` + `PageMeta`;
request page numbers are 1-based and converted to 0-based `PageRequest`.

### Storage transactionality

When persisting an entity that references an uploaded file, the pattern (see `ProductService.addImage`) is: upload to
storage first, then on DB failure delete the uploaded object in a `catch` block so storage and DB don't diverge.

### Database

Flyway migrations in `src/main/resources/db/migration` (`V<n>__description.sql`, naming validated). Tables use
`snake_case`, explicit `pk_/fk_/uq_/ck_/idx_` constraint names, `BIGSERIAL` ids, `TIMESTAMPTZ` timestamps, and DB-level
`CHECK` constraints for enums and invariants. Entities map enums as `VARCHAR` with matching `CHECK` constraints.

## Testing conventions

- Controller tests: `@WebMvcTest(XController.class)` + `@AutoConfigureMockMvc(addFilters = false)` (security filters
  off) + `@MockitoBean` for the service. Assert on `ProblemDetail` JSON shape.
- Service tests: plain `@ExtendWith(MockitoExtension.class)` with `@Mock`/`@InjectMocks`, no Spring context.
- `spring-boot-starter-data-jpa-test`, `-flyway-test`, and `-security-test` are available for integration-style tests.

## Conventions

- Lombok is used (`@RequiredArgsConstructor` for constructor injection, `@Getter`/`@Setter` on entities). The compiler
  plugin declares the Lombok annotation processor explicitly.
- Services own transactions (`@Transactional`, `readOnly = true` for queries); controllers stay thin and only do
  binding + validation.

## Coding preferences

### Spring Data JPA

- Prefer explicitly named repository methods with `@Query` over long Spring Data derived query method names.
- Use Spring Data derived query methods when the resulting method name is short and immediately readable, such as
  `findByEmail` or `existsBySlug`.
- Prefer JPQL for custom entity-based queries.
- Use native SQL only when JPQL is insufficient or database-specific SQL is justified.
- Do not create excessively long method names just to avoid writing an explicit query.

### Java

- Do not use Java `record`.
- Prefer regular classes for DTOs and other data carriers.
- Follow the existing Lombok conventions of the project where applicable.