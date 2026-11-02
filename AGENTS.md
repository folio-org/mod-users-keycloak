# mod-users-keycloak

FOLIO module combining mod-users + mod-users-bl interfaces with Keycloak integration — bridges FOLIO user management and Keycloak IAM. Java, Spring Boot, PostgreSQL/Liquibase, Kafka, Keycloak, MapStruct, Lombok.

## Build & Test

```bash
mvn clean install              # full build (compile, codegen, checkstyle, unit and integration tests)
mvn clean install -DskipTests  # skip tests
mvn test                       # unit tests (@UnitTest)
mvn verify                     # integration tests (@IntegrationTest; needs containers)
mvn test -Dtest=UserServiceTest#createUserSafe_positive   # single unit test
mvn verify -Dit.test=UserIT#get_positive -Dtest=SkipSurefire -Dsurefire.failIfNoSpecifiedTests=false  # single IT
mvn clean verify -P coverage   # JaCoCo report; the 80% instruction check does not fail the build
mvn checkstyle:check           # also runs at process-classes
mvn clean generate-sources     # regenerate API sources from OpenAPI
```

**Style**: max method length 21 lines (suppressed in tests); 2-space indent, 120-char lines, LF, UTF-8 (`.editorconfig`); `folio-java-checkstyle` rules, suppressions in `checkstyle/checkstyle-suppressions.xml`.

## Architecture (`org.folio.uk`)

**Key patterns**:
1. **Dual storage**: users live in both mod-users (Postgres) and Keycloak; `UserService` creates in mod-users first, then upserts in Keycloak.
2. **Multi-tenant**: each tenant → a Keycloak realm; `UsersTenantService` handles realm setup, system user, Kafka listener restart.
3. **Event-driven system users**: `KafkaMessageListener` on topic pattern `(${application.environment}\.)(.*\.)mgr-tenant-entitlements.system-user` → `SystemUserService` (CREATE/UPDATE/DELETE).
4. **HTTP clients**: external modules + Keycloak Admin API (`KeycloakClient`) are `@HttpExchange` interfaces; admin tokens Caffeine-cached.
5. **Password reset**: a password-reset action with its expiration time is stored in mod-login-keycloak (`LoginClient`, `/authn/password-reset-action`); links via mod-notify; config from mod-configuration.

## Codegen & Descriptor

- OpenAPI `src/main/resources/swagger.api/users-keycloak.yaml` (schemas in `schemas/`) → `org.folio.uk.rest.resource` + `.domain.dto` in `target/generated-sources/`. Add endpoint: edit spec/schemas → `mvn generate-sources` → implement generated interface.
- Module Descriptor: `descriptors/ModuleDescriptor-template.json` → `target/ModuleDescriptor.json` at build.

## Testing

- Unit (`*Test.java`, `@UnitTest`): Mockito/MockMvc, surefire.
- Integration (`*IT.java`, `@IntegrationTest`): failsafe; extend `BaseIntegrationTest` (`src/test/java/org/folio/uk/base/`) providing `@EnablePostgres/@EnableKeycloakTlsMode/@EnableKafka/@EnableWireMock`, `@ActiveProfiles("it")`, request helpers, `@WireMockStub`. Stubs in `src/test/resources/wiremock/stubs/`; fixtures in `json/`; realm `json/keycloak/testtenant-realm.json`.
