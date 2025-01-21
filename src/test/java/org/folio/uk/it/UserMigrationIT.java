package org.folio.uk.it;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.folio.test.TestUtils.parseResponse;
import static org.folio.uk.support.TestConstants.TENANT_NAME;
import static org.folio.uk.support.TestValues.readValue;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.util.UUID;
import org.folio.test.extensions.WireMockStub;
import org.folio.test.types.IntegrationTest;
import org.folio.uk.base.BaseIntegrationTest;
import org.folio.uk.domain.dto.UserMigrationJob;
import org.folio.uk.domain.dto.UserMigrationJobStatus;
import org.folio.uk.domain.dto.Users;
import org.folio.uk.integration.keycloak.KeycloakService;
import org.folio.uk.migration.properties.UserMigrationProperties;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.jdbc.Sql;

@IntegrationTest
class UserMigrationIT extends BaseIntegrationTest {

  private static final String JOB_ID = "9971c946-c449-46b6-968b-77b66280b044";

  @SpyBean private KeycloakService keycloakService;
  @SpyBean private UserMigrationProperties userMigrationProperties;

  @BeforeAll
  static void beforeAll() {
    enableTenant(TENANT_NAME);
  }

  @AfterAll
  static void afterAll() {
    removeTenant(TENANT_NAME);
  }

  @Test
  @Sql("classpath:/sql/truncate-migration.sql")
  @WireMockStub(scripts = {
    "/wiremock/stubs/perms/search-users-migration.json",
    "/wiremock/stubs/users/search-users-migration.json",
    "/wiremock/stubs/users/search-users-tenants-migration.json",
  })
  void migrateUsers_positive() throws Exception {
    var mvcResult = doPost("/users-keycloak/migrations", null).andReturn();
    var resp = parseResponse(mvcResult, UserMigrationJob.class);

    assertThat(resp.getId()).isNotNull();
    assertThat(resp.getStartedAt()).isNotNull();
    assertThat(resp.getStatus()).isEqualTo(UserMigrationJobStatus.IN_PROGRESS);
    assertThat(resp.getTotalRecords()).isEqualTo(20);

    var status = await().atMost(Duration.ofSeconds(10))
      .until(() -> getJobStatusById(resp.getId()), equalTo(UserMigrationJobStatus.FINISHED));
    assertThat(status).isEqualTo(UserMigrationJobStatus.FINISHED);

    var users = readValue("json/user/search-users-migration.json", Users.class);
    var migratedUser = users.getUsers().get(0);

    var passwordCaptor = ArgumentCaptor.forClass(String.class);
    verify(keycloakService).createUserForMigration(any(), passwordCaptor.capture(), any());
    assertThat(passwordCaptor.getValue()).isNull();

    verifyKeyCloakUser(migratedUser);
  }

  @Test
  @Sql("classpath:/sql/truncate-migration.sql")
  @WireMockStub(scripts = {
    "/wiremock/stubs/perms/search-users-migration.json",
    "/wiremock/stubs/users/search-users-migration.json",
    "/wiremock/stubs/users/search-users-tenants-migration.json",
  })
  void migrateUsers_positive_if_default_passwords_on_migration_equals_true() throws Exception {
    when(userMigrationProperties.isDefaultPasswordsOnMigration()).thenReturn(true);
    var mvcResult = doPost("/users-keycloak/migrations", null).andReturn();
    var resp = parseResponse(mvcResult, UserMigrationJob.class);

    assertThat(resp.getId()).isNotNull();
    assertThat(resp.getStartedAt()).isNotNull();
    assertThat(resp.getStatus()).isEqualTo(UserMigrationJobStatus.IN_PROGRESS);
    assertThat(resp.getTotalRecords()).isEqualTo(20);

    var status = await().atMost(Duration.ofSeconds(10))
      .until(() -> getJobStatusById(resp.getId()), equalTo(UserMigrationJobStatus.FINISHED));
    assertThat(status).isEqualTo(UserMigrationJobStatus.FINISHED);

    var users = readValue("json/user/search-users-migration.json", Users.class);
    var migratedUser = users.getUsers().get(0);

    var passwordCaptor = ArgumentCaptor.forClass(String.class);
    verify(keycloakService).createUserForMigration(any(), passwordCaptor.capture(), any());
    assertThat(passwordCaptor.getValue()).isEqualTo(migratedUser.getUsername());

    verifyKeyCloakUser(migratedUser);
  }

  @Test
  @Sql("classpath:/sql/truncate-migration.sql")
  @WireMockStub(scripts = {
    "/wiremock/stubs/perms/search-users-migration.json",
    "/wiremock/stubs/users/search-users-migration-fail.json"
  })
  void migrateUsers_negative_failed() throws Exception {
    var mvcResult = doPost("/users-keycloak/migrations", null).andReturn();
    var resp = parseResponse(mvcResult, UserMigrationJob.class);

    assertThat(resp.getId()).isNotNull();
    assertThat(resp.getStartedAt()).isNotNull();
    assertThat(resp.getStatus()).isEqualTo(UserMigrationJobStatus.IN_PROGRESS);
    assertThat(resp.getTotalRecords()).isEqualTo(20);

    var status = await().atMost(Duration.ofSeconds(10))
      .until(() -> getJobStatusById(resp.getId()), equalTo(UserMigrationJobStatus.FAILED));
    assertThat(status).isEqualTo(UserMigrationJobStatus.FAILED);
  }

  @Test
  @Sql("classpath:/sql/truncate-migration.sql")
  @WireMockStub(scripts = {
    "/wiremock/stubs/perms/search-users-migration-empty.json",
    "/wiremock/stubs/users/search-users-migration.json"
  })
  void migrateUsers_negative() throws Exception {
    attemptPost("/users-keycloak/migrations", null)
      .andExpect(status().isBadRequest());
  }

  @Test
  @Sql("classpath:/sql/populate-migration-schema.sql")
  @WireMockStub(scripts = {
    "/wiremock/stubs/perms/search-users-migration.json",
    "/wiremock/stubs/users/search-users-migration.json"
  })
  void migrateUsers_negative_exists() throws Exception {
    attemptPost("/users-keycloak/migrations", null)
      .andExpect(status().isBadRequest())
      .andExpect(jsonPath("$.total_records", is(1)))
      .andExpect(jsonPath("$.errors[0].message", is("There is already exists active migration job")))
      .andExpect(jsonPath("$.errors[0].type", is("RequestValidationException")))
      .andExpect(jsonPath("$.errors[0].code", is("validation_error")));
  }

  @Test
  @Sql("classpath:/sql/populate-migration-schema.sql")
  void delete_migration_exists() throws Exception {
    doDelete("/users-keycloak/migrations/" + JOB_ID);
  }

  @Test
  @Sql("classpath:/sql/truncate-migration.sql")
  void delete_migration() throws Exception {
    doDelete("/users-keycloak/migrations/" + JOB_ID);
  }

  @Test
  @Sql("classpath:/sql/populate-migration-schema.sql")
  void get_migration_by_id() throws Exception {
    doGet("/users-keycloak/migrations/" + JOB_ID)
      .andExpect(jsonPath("$.id", is(JOB_ID)));
  }

  @Test
  @Sql("classpath:/sql/truncate-migration.sql")
  void get_migration_by_id_not_found() throws Exception {
    attemptGet("/users-keycloak/migrations/" + JOB_ID)
      .andExpect(status().isNotFound());
  }

  @Test
  @Sql("classpath:/sql/populate-migration-schema.sql")
  void get_migration_by_cql() throws Exception {
    doGet("/users-keycloak/migrations?query=id==" + JOB_ID)
      .andExpect(jsonPath("$.migrations[0].id", is(JOB_ID)))
      .andExpect(jsonPath("$.totalRecords", is(1)));
  }

  private UserMigrationJobStatus getJobStatusById(UUID id) throws Exception {
    return parseResponse(doGet("/users-keycloak/migrations/" + id).andReturn(), UserMigrationJob.class)
      .getStatus();
  }
}
