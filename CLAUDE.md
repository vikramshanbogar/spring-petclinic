# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Run Commands

```bash
# Run the application (default H2 in-memory database)
./mvnw spring-boot:run

# Run with MySQL, PostgreSQL, or Oracle profile
./mvnw spring-boot:run -Dspring-boot.run.profiles=mysql
./mvnw spring-boot:run -Dspring-boot.run.profiles=postgres
./mvnw spring-boot:run -Dspring-boot.run.profiles=oracle

# Build
./mvnw package

# Run all tests
./mvnw test

# Run a single test class
./mvnw test -Dtest=OwnerControllerTests

# Compile SCSS to CSS (required after Bootstrap/style changes)
./mvnw package -P css

# Build Docker image
./mvnw spring-boot:build-image
```

The app runs at `http://localhost:8080/`. H2 console available at `http://localhost:8080/h2-console`.

## Code Style

- **Formatter**: `spring-javaformat-maven-plugin` enforces formatting at build time — run `./mvnw spring-javaformat:apply` to auto-format before committing.
- **Checkstyle**: No-HTTP rule enforced (no plain `http://` URLs allowed in code).
- **Indentation**: 4 spaces for Java/XML, 2 spaces for HTML/SQL/CSS.
- **Line endings**: LF (Unix-style).

## Architecture

**Domain packages** under `src/main/java/org/springframework/samples/petclinic/`:

- `model/` — Base JPA entity classes (`BaseEntity`, `NamedEntity`, `Person`)
- `owner/` — Owner, Pet, PetType, Visit entities + controllers + repositories. All visits are owned by the `owner` package (not a separate package).
- `vet/` — Vet and Specialty entities + controller + repository
- `system/` — Cache configuration (Caffeine/JCache), web config, crash demo controller

**Data layer**: Spring Data JPA repositories directly in domain packages (no separate service layer). Repositories extend `JpaRepository` or `CrudRepository`.

**View layer**: Thymeleaf templates in `src/main/resources/templates/`. A shared layout fragment at `templates/fragments/layout.html` wraps all pages.

**CSS**: Pre-compiled `petclinic.css` is committed to `src/main/resources/static/resources/css/`. Source SCSS lives under `src/main/less/` and is compiled via the `css` Maven profile using Bootstrap SCSS.

## Database Profiles

| Profile | Database | Default URL |
|---------|----------|-------------|
| (none)  | H2 in-memory | auto |
| `mysql` | MySQL | `jdbc:mysql://localhost/petclinic` |
| `postgres` | PostgreSQL | `jdbc:postgresql://localhost/petclinic` |
| `oracle` | Oracle DB | `jdbc:oracle:thin:@localhost:1521/petclinic` |

Docker Compose (`docker-compose.yml`) provides MySQL 9.2 (port 3306) and PostgreSQL 17.5 (port 5432) for local dev. Credentials are `petclinic/petclinic`.

SQL initialization scripts are in `src/main/resources/db/{h2,mysql,postgres,oracle,hsqldb}/`.

## Testing

- **Unit/slice tests**: `src/test/java/.../owner/`, `vet/`, `system/`, `model/` — use MockMvc and `@WebMvcTest`
- **H2 integration tests**: `PetClinicIntegrationTests` — full application context with H2
- **MySQL integration tests**: `MySqlIntegrationTests` — requires Docker (uses TestContainers)
- **PostgreSQL integration tests**: `PostgresIntegrationTests` — requires Docker Compose
- **Oracle integration tests**: `OracleIntegrationTests` — requires Docker (uses TestContainers with `gvenzl/oracle-free:slim`)

Integration tests with real databases are skipped unless Docker is available.

## Key Dependencies

- Spring Boot 3.5.0, Java 17+
- Thymeleaf (server-side rendering), Bootstrap 5.3.6 via WebJars
- Caffeine cache (exposed via JCache) — the `vets` endpoint is cached
- GraalVM native image support (`PetClinicRuntimeHints.java` for AOT hints)
