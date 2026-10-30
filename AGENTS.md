# mod-users-keycloak

FOLIO module providing user and composite-user APIs with Keycloak integration; it bridges FOLIO user management and Keycloak IAM. Java 21, Spring Boot, PostgreSQL/Liquibase, Kafka, MapStruct, Lombok.

## Build & Test

```bash
mvn clean install              # full build (compile, codegen, checkstyle, unit + integration tests; needs containers)
mvn clean install -DskipTests  # skip tests
mvn test                       # unit tests (@UnitTest)
mvn verify                     # unit + integration tests (@IntegrationTest; needs containers)
mvn test -Dtest=UserServiceTest#createUserSafe_positive   # single unit test
mvn verify -Dit.test=UserIT#get_positive -Dtest=SkipSurefire -Dsurefire.failIfNoSpecifiedTests=false   # single IT, skips unit tests
mvn clean verify -P coverage   # coverage report (JaCoCo 80% instruction threshold; reported only, the build does not fail)
mvn checkstyle:check           # also runs at process-classes
mvn clean generate-sources     # regenerate API sources from OpenAPI
```

## Style

Match the repository's existing style: use maintained implementations and tests as templates for new code, and keep the structure, naming, and conventions of neighboring files. The checkstyle configuration (`checkstyle/`) is the authority for style limits.

## Pitfalls

- Dual storage: users live in both Postgres and Keycloak; `UserService` creates the user in mod-users first, then upserts it in Keycloak; keep that order.
- System users are event-driven: `KafkaMessageListener` on topic pattern `(${application.environment}\.)(.*\.)mgr-tenant-entitlements.system-user` → `SystemUserService` (CREATE/UPDATE/DELETE).
- Password reset: a time-limited reset action is issued through the `login` interface; links go via mod-notify, config comes from mod-configuration.
- HTTP clients: `@HttpExchange` interfaces for external modules and the Keycloak Admin API; admin tokens are Caffeine-cached.
- Integration tests extend `BaseIntegrationTest` (`src/test/java/org/folio/uk/base/`): containers, Keycloak realm, WireMock stubs, request helpers. `@IntegrationTest` sits on each IT class, not on the base class.

## Codegen & Descriptor

- OpenAPI `src/main/resources/swagger.api/users-keycloak.yaml` (schemas in `schemas/`) → `org.folio.uk.rest.resource` + `.domain.dto` in `target/generated-sources/`. Add endpoint: edit spec/schemas → `mvn generate-sources` → implement generated interface. Do not edit generated sources.
- Module Descriptor: `descriptors/ModuleDescriptor-template.json` → `target/ModuleDescriptor.json` at build.

## Notes

- Spring Retry: system user creation (`SYSTEM_USER_RETRY_*`) and role assignment (`systemUserRoleRetryTemplate`).
- Caching: Caffeine `keycloak-configuration`, `keycloak-client-configuration`, `token` in `configuration/CacheConfiguration.java`.
- Secure store: AWS-SSM/Vault/FSSP via `application.secret-store.*`.
- Sonar exclusions: `domain/`, `configuration/`, `rest/resource/`, `mapper/`.
- Lombok: `lombok.config` with `addLombokGeneratedAnnotation = true`.
