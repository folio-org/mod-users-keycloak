# mod-users-keycloak

FOLIO module combining mod-users + mod-users-bl interfaces with Keycloak integration — bridges FOLIO user management and Keycloak IAM. Java 21, Spring Boot 3.5.7, PostgreSQL/Liquibase, Kafka, OpenFeign, MapStruct, Lombok.

## Build & Test

```bash
mvn clean install              # full build (compile, codegen, checkstyle, unit tests)
mvn clean install -DskipTests  # skip tests
mvn test                       # unit tests (@Tag("unit"))
mvn verify                     # integration tests (@Tag("integration"); needs containers)
mvn test -Dtest=UserServiceTest#shouldCreateUser   # single unit test
mvn verify -Dit.test=UserIT#shouldReturnUser        # single IT
mvn clean verify -P coverage   # JaCoCo 80% instruction min
mvn checkstyle:check           # also runs at process-classes
mvn clean generate-sources     # regenerate API sources from OpenAPI
```

**Style**: max method length 21 lines (suppressed in tests); 2-space indent, 120-char lines, LF, UTF-8 (`.editorconfig`); `folio-java-checkstyle` rules, suppressions in `checkstyle/checkstyle-suppressions.xml`.

## Architecture (`org.folio.uk`)

**Packages**: `controller/` (implement OpenAPI interfaces) · `service/` (`UserService`, `UsersTenantService`, `PasswordResetService`, `CapabilitiesService`) · `integration/` Feign clients (`keycloak/`, `users/`, `roles/`, `kafka/`, plus notify/password/login/inventory/permission/policy) · `domain/` (`UserMigrationJobEntity` + repos) · `mapper/` · `migration/` · `exception/`.

**Key patterns**:
1. **Dual storage**: users live in both mod-users (Postgres) and Keycloak; `UserService` creates in mod-users first, then upserts in Keycloak.
2. **Multi-tenant**: each tenant → a Keycloak realm; `UsersTenantService` handles realm setup, system user, Kafka listener restart.
3. **Event-driven system users**: `KafkaMessageListener` on topic pattern `(${environment}\.)(.*\.)mgr-tenant-entitlements.system-user` → `SystemUserService` (CREATE/UPDATE/DELETE).
4. **Feign**: external modules + Keycloak Admin API (`KeycloakClient`); admin tokens Caffeine-cached.
5. **Password reset**: time-limited tokens as Keycloak user attributes; links via mod-notify; config from mod-configuration.

## Codegen & Descriptor

- OpenAPI `src/main/resources/swagger.api/users-keycloak.yaml` (schemas in `schemas/`) → `org.folio.uk.rest.resource` + `.domain.dto` in `target/generated-sources/`. Add endpoint: edit spec/schemas → `mvn generate-sources` → implement generated interface.
- Module Descriptor: `descriptors/ModuleDescriptor-template.json` → `target/ModuleDescriptor.json` at build.

## Testing

- Unit (`*Test.java`, `@Tag("unit")`): Mockito/MockMvc, surefire.
- Integration (`*IT.java`, `@Tag("integration")`): failsafe; extend `BaseIntegrationTest` (`src/test/java/org/folio/uk/base/`) providing `@EnablePostgres/@EnableKeycloakTlsMode/@EnableKafka/@EnableWireMock`, `@ActiveProfiles("it")`, request helpers, `@WireMockStub`. Stubs in `src/test/resources/wiremock/stubs/`; fixtures in `json/`; realm `json/keycloak/testtenant-realm.json`.

## Notes

- **Spring Retry**: system user creation (`SYSTEM_USER_RETRY_*`) and role assignment (`systemUserRoleRetryTemplate`).
- **Caching**: Caffeine `keycloak-configuration`, `keycloak-client-configuration`, `token` in `configuration/CacheConfiguration.java`.
- **Secure store**: AWS-SSM/Vault/FSSP via `application.secret-store.*`.
- **Sonar exclusions**: `domain/`, `configuration/`, `rest/resource/`, `mapper/`.
- **Lombok**: `lombok.config` with `addLombokGeneratedAnnotation = true`.
