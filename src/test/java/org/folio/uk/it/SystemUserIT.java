package org.folio.uk.it;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.folio.test.TestUtils.readString;
import static org.mockito.Mockito.only;
import static org.mockito.Mockito.verify;

import java.util.UUID;
import org.awaitility.Durations;
import org.folio.test.extensions.WireMockStub;
import org.folio.test.types.IntegrationTest;
import org.folio.uk.base.BaseIntegrationTest;
import org.folio.uk.base.KeycloakTestClient;
import org.folio.uk.domain.dto.UserCapabilitiesRequest;
import org.folio.uk.integration.keycloak.KeycloakClient;
import org.folio.uk.integration.keycloak.TokenService;
import org.folio.uk.integration.roles.UserCapabilitiesClient;
import org.folio.uk.integration.users.UsersClient;
import org.folio.uk.support.TestConstants;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.kafka.core.KafkaTemplate;

@IntegrationTest
class SystemUserIT extends BaseIntegrationTest {

  private static final UUID USER_ID = UUID.fromString("de5bb75d-e696-4d43-9df8-289f39367079");
  private static final UUID CAPABILITY_ID = UUID.fromString("b190defd-8e0e-4a04-ba14-448091f76b3f");
  private static final UUID CAPABILITY_ID1 = UUID.fromString("b190defd-8e0e-4a04-ba14-448091f76b5f");

  @Autowired private TokenService tokenService;
  @Autowired private KeycloakClient keycloakClient;
  @Autowired private KafkaTemplate<String, Object> kafkaTemplate;

  @SpyBean private UserCapabilitiesClient userCapabilitiesClient;
  @SpyBean private UsersClient usersClient;

  @BeforeAll
  static void beforeAll(@Autowired KeycloakTestClient client, @Autowired TokenService tokenService) {
    enableTenant(TestConstants.TENANT_NAME, tokenService, client);
  }

  @AfterAll
  static void afterAll(@Autowired KeycloakClient client, @Autowired TokenService tokenService) {
    removeTenant(TestConstants.TENANT_NAME);

    var authToken = tokenService.issueToken();
    var usersByUsername = client.findUsersByUsername(TestConstants.TENANT_NAME, "master-system-user", true, authToken);
    assertThat(usersByUsername).isEmpty();
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

    await().atMost(Durations.FIVE_SECONDS).untilAsserted(() -> verify(usersClient).deleteUser(USER_ID));

    wmAdminClient.addStubMapping(readString("wiremock/stubs/users/create-system-user.json"));
  }

  @Test
  @WireMockStub(scripts = {
    "/wiremock/stubs/users/find-module-system-user-by-query.json",
    "/wiremock/stubs/capabilities/query-capabilities-by-permissions.json",
    "/wiremock/stubs/capabilities/assign-module-system-user-capabilities.json"
  })
  void updateOnEvent() {
    kafkaTemplate.send(FOLIO_SYSTEM_USER_TOPIC, TestConstants.systemUserResourceUpdateEvent());

    var expectedRequest = new UserCapabilitiesRequest().userId(USER_ID).addCapabilityIdsItem(CAPABILITY_ID)
      .addCapabilityIdsItem(CAPABILITY_ID1);

    await().atMost(Durations.FIVE_SECONDS).untilAsserted(() ->
      verify(userCapabilitiesClient, only()).assignUserCapabilities(USER_ID, expectedRequest));
  }

  @Test
  @WireMockStub(scripts = {
    "/wiremock/stubs/users/create-module-system-user.json",
    "/wiremock/stubs/capabilities/query-capabilities-by-permission.json",
    "/wiremock/stubs/capabilities/assign-module-system-user-capability.json"
  })
  void createOnEvent() {
    kafkaTemplate.send(FOLIO_SYSTEM_USER_TOPIC, TestConstants.systemUserResourceEvent());

    var expectedRequest = new UserCapabilitiesRequest().userId(USER_ID).addCapabilityIdsItem(CAPABILITY_ID);

    await().atMost(Durations.FIVE_SECONDS).untilAsserted(() ->
      verify(userCapabilitiesClient, only()).assignUserCapabilities(USER_ID, expectedRequest)
    );

    var authToken = tokenService.issueToken();
    var systemUsersList = keycloakClient.findUsersByUsername(TestConstants.TENANT_NAME, "mod-foo", true, authToken);

    assertThat(systemUsersList).hasSize(1);
    var systemUser = systemUsersList.get(0);
    assertThat(systemUser.getFirstName()).isEqualTo("System User - mod-foo");
    assertThat(systemUser.getLastName()).isEqualTo("System");
    assertThat(systemUser.getUserName()).isEqualTo("mod-foo");
  }
}
