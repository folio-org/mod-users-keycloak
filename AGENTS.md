# mod-users-keycloak

Provides the `users-keycloak` user and composite-user APIs. User records live in mod-users (called over HTTP) and login users in Keycloak; the module's own database holds only user-migration jobs and the tenant module-version marker.

## Commands

```bash
mvn clean install -DskipTests  # build
mvn test                       # unit tests (@UnitTest, *Test.java)
mvn verify                     # unit and integration tests (@IntegrationTest, *IT.java); needs Docker
mvn test -Dtest=UserServiceTest#createUserSafe_positive  # one unit test
mvn verify -Dit.test=UserIT#get_positive -Dtest=SkipSurefire -Dsurefire.failIfNoSpecifiedTests=false  # one IT, unit tests skipped
mvn clean verify -P coverage   # JaCoCo report; the 80% instruction check does not fail the build
mvn checkstyle:check           # style only
```

Checkstyle also runs at `process-classes`, so every build above fails on a violation, test sources included. The base rules come from `folio-java-checkstyle`; this repository limits methods to 21 lines (`checkstyle/checkstyle-checker.properties`) and turns that limit off for `src/test/java`.

Surefire runs only the `unit` group and failsafe only the `integration` group, so a test class without `@UnitTest` or `@IntegrationTest` is skipped without a warning. Integration tests extend `BaseIntegrationTest`, which starts Postgres, Kafka, Keycloak (TLS), and WireMock.

## Conventions

- The OpenAPI spec `src/main/resources/swagger.api/users-keycloak.yaml` (schemas in `schemas/`) generates `org.folio.uk.rest.resource` and `org.folio.uk.domain.dto`. Change the spec, not the generated sources.
- `schemas/user.json` is a copy of the mod-users user schema. A field missing from the copy is dropped when a user is saved through `PUT /users-keycloak/users/{id}`, so the stored value is lost in mod-users. When mod-users adds a field, add it here and raise the `users` version in `requires` and the `users-keycloak` version in `provides` of `descriptors/ModuleDescriptor-template.json`.
- A user is created in mod-users first and in Keycloak second, because the Keycloak user stores the mod-users id in its `user_id` attribute (`upsertUser` rejects a user without an id). Keycloak update, delete, enable, and disable find the Keycloak user by this attribute, not by username.
- Create a `KeycloakException` with a cause when it can reach a controller, because `ApiExceptionHandler` reads `getCause().getMessage()` and fails with a `NullPointerException` otherwise. `UserService.updateUser` already has such a path ("Too many keycloak users").

## Gotchas

- User create, update, and delete call mod-users first and Keycloak second, with no compensation. If the Keycloak call fails, the mod-users change stays, and the client usually gets a 400.
- On create, any 422 from mod-users counts as "user already exists": the module loads the user by username and continues with Keycloak. If no user has that username, the request ends with 404. This, plus the tolerated Keycloak 409 in `createUserSafe`, makes the `@Retryable` system-user creation safe to repeat (`SYSTEM_USER_RETRY_COUNT`, `SYSTEM_USER_RETRY_DELAY`).
- `KeycloakService.upsertUser` matches by username. If a Keycloak user with that username exists, it updates that user (including its `user_id` attribute) and ignores the password. `PUT` creates a missing Keycloak user without a password.
- `DELETE /users-keycloak/users/{id}` deletes the mod-users record and the user's roles, capabilities, capability sets, and policies only if the user exists in mod-users; the Keycloak delete always runs. A retry after a partial failure therefore deletes only the Keycloak user and leaves the role cleanup undone.
- `ApiExceptionHandler` maps every `KeycloakException` to 400 `service_error`, Keycloak outages included. Downstream 400 and 404 responses pass through as 400 and 404; other downstream errors become 500.
- `KafkaMessageListener` has two listeners: system-user events from mgr-tenant-entitlements, and `users.users` events, where only `UPDATE` is handled and only the `active` flag is synced to Keycloak. The `KAFKA_SYS_USER_TOPIC_RETRY_*` and `KAFKA_USER_TOPIC_RETRY_*` settings apply only while the tenant schema does not exist yet ("relation ... does not exist"). Any other failure is not retried: the event is logged and skipped, so a failed `active` sync leaves Keycloak out of date. For system-user events, a failure result is also published when `EVENT_CONFIRMATION_ENABLED=true`.
- `POST /users-keycloak/migrations` returns 201 at once and creates Keycloak users asynchronously. A user that fails (for example, already in Keycloak) is logged and skipped, and the job still ends as `FINISHED`; `FAILED` means a whole batch threw. `/users-keycloak/idp-migrations` is also asynchronous, keeps no job record, and returns 204 without doing anything when `SINGLE_TENANT_UX` is false.
