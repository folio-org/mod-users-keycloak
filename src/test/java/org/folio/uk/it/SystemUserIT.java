package org.folio.uk.it;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.awaitility.Durations.FIVE_SECONDS;
import static org.folio.test.extensions.impl.KeycloakContainerExtension.getKeycloakAdminClient;
import static org.folio.uk.support.TestConstants.TENANT_NAME;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.UUID;
import org.folio.test.TestUtils;
import org.folio.test.extensions.WireMockStub;
import org.folio.test.types.IntegrationTest;
import org.folio.uk.base.BaseIntegrationTest;
import org.folio.uk.integration.keycloak.KeycloakClient;
import org.folio.uk.integration.keycloak.TokenService;
import org.folio.uk.integration.roles.UserRolesClient;
import org.folio.uk.integration.roles.dafaultrole.DefaultRolesClient;
import org.folio.uk.integration.roles.model.LoadablePermission;
import org.folio.uk.integration.roles.model.LoadableRole;
import org.folio.uk.integration.roles.model.UserRoles;
import org.folio.uk.integration.users.UsersClient;
import org.folio.uk.support.TestConstants;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

@IntegrationTest
class SystemUserIT extends BaseIntegrationTest {

  private static final UUID USER_ID = UUID.fromString("de5bb75d-e696-4d43-9df8-289f39367079");

  @Autowired private TokenService tokenService;
  @Autowired private KeycloakClient keycloakClient;
  @Autowired private KafkaTemplate<String, Object> kafkaTemplate;

  @MockitoSpyBean private UsersClient usersClient;
  @MockitoSpyBean private DefaultRolesClient defaultRolesClient;
  @MockitoSpyBean private UserRolesClient userRolesClient;

  @BeforeAll
  static void beforeAll() {
    enableTenant(TENANT_NAME);
  }

  @AfterAll
  static void afterAll(@Autowired KeycloakClient client, @Autowired TokenService tokenService) {
    removeTenant(TENANT_NAME, false);

    var authToken = tokenService.issueToken();
    var usersByUsername = client.findUsersByUsername(TENANT_NAME, TENANT_NAME + "-system-user", true, authToken);
    assertThat(usersByUsername).isEmpty();

    getKeycloakAdminClient().realm(TENANT_NAME).remove();
  }

  @Test
  @WireMockStub(scripts = {
    "/wiremock/stubs/users/find-module-system-user-by-query.json",
    "/wiremock/stubs/users/delete-module-system-user.json",
    "/wiremock/stubs/users/get-module-system-user-capability-set.json",
    "/wiremock/stubs/users/get-module-system-user-capability.json",
    "/wiremock/stubs/users/get-module-system-user-roles.json",
    "/wiremock/stubs/policy/find-policy-by-module-system-username.json"
  })
  void deleteOnEvent() {
    kafkaTemplate.send(FOLIO_SYSTEM_USER_TOPIC, TestConstants.systemUserResourceDeleteEvent());

    await().atMost(FIVE_SECONDS).untilAsserted(() -> verify(usersClient).deleteUser(USER_ID));

    wmAdminClient.addStubMapping(TestUtils.readString("wiremock/stubs/users/create-system-user.json"));
  }

  @Test
  @WireMockStub(scripts = {
    "/wiremock/stubs/users/find-module-system-user-by-query.json",
    "/wiremock/stubs/loadable-roles/upsert-loadable-default-role-on-update.json",
    "/wiremock/stubs/roles/assign-role-to-user.json"
  })
  void updateOnEvent() {
    kafkaTemplate.send(FOLIO_SYSTEM_USER_TOPIC, TestConstants.systemUserResourceUpdateEvent());

    var expectedLoadableRole = new LoadableRole();
    expectedLoadableRole.setName("default-system-role-mod-foo");
    expectedLoadableRole.setDescription("Default system role for system user mod-foo");
    expectedLoadableRole.setPermissions(java.util.List.of(
      LoadablePermission.of("foo.bar"),
      LoadablePermission.of("foo.bar1")
    ));

    var expectedUserRoles = new UserRoles();
    expectedUserRoles.setUserId(USER_ID);
    expectedUserRoles.setRoleIds(List.of(UUID.fromString("00000000-1000-0000-0000-000000000000")));

    await().atMost(FIVE_SECONDS).untilAsserted(() -> {
      verify(defaultRolesClient).createDefaultLoadableRole(eq(expectedLoadableRole));
      verify(userRolesClient).assignRoleToUser(eq(expectedUserRoles));
    });
  }

  @Test
  @WireMockStub(scripts = {
    "/wiremock/stubs/users/create-module-system-user.json",
    "/wiremock/stubs/loadable-roles/upsert-loadable-default-role-on-create.json",
    "/wiremock/stubs/roles/assign-role-to-user.json"
  })
  void createOnEvent() {
    kafkaTemplate.send(FOLIO_SYSTEM_USER_TOPIC, TestConstants.systemUserResourceEvent());

    var expectedLoadableRole = new LoadableRole();
    expectedLoadableRole.setName("default-system-role-mod-foo");
    expectedLoadableRole.setDescription("Default system role for system user mod-foo");
    expectedLoadableRole.setPermissions(java.util.List.of(
      LoadablePermission.of("foo.bar")
    ));

    var expectedUserRoles = new UserRoles();
    expectedUserRoles.setUserId(USER_ID);
    expectedUserRoles.setRoleIds(List.of(UUID.fromString("00000000-1000-0000-0000-000000000000")));

    await().atMost(FIVE_SECONDS).untilAsserted(() -> {
      verify(defaultRolesClient).createDefaultLoadableRole(eq(expectedLoadableRole));
      verify(userRolesClient).assignRoleToUser(eq(expectedUserRoles));
    });

    var authToken = tokenService.issueToken();
    var systemUsersList = keycloakClient.findUsersByUsername(TENANT_NAME, "mod-foo", true, authToken);

    assertThat(systemUsersList).hasSize(1);
    var systemUser = systemUsersList.getFirst();
    assertThat(systemUser.getFirstName()).isEqualTo("System User - mod-foo");
    assertThat(systemUser.getLastName()).isEqualTo("System");
    assertThat(systemUser.getUserName()).isEqualTo("mod-foo");
  }
}
