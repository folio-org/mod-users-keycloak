package org.folio.uk.it;

import static org.apache.commons.collections4.CollectionUtils.isEmpty;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.awaitility.Durations.FIVE_SECONDS;
import static org.awaitility.Durations.ONE_HUNDRED_MILLISECONDS;
import static org.folio.test.TestUtils.parseResponse;
import static org.folio.uk.integration.keycloak.model.KeycloakUser.USER_ID_ATTR;
import static org.folio.uk.support.TestConstants.TENANT_NAME;

import java.util.Optional;
import java.util.UUID;
import org.folio.test.extensions.WireMockStub;
import org.folio.test.types.IntegrationTest;
import org.folio.uk.base.BaseIntegrationTest;
import org.folio.uk.base.model.UserDomainEvent;
import org.folio.uk.domain.dto.User;
import org.folio.uk.integration.keycloak.KeycloakClient;
import org.folio.uk.integration.keycloak.KeycloakService;
import org.folio.uk.integration.keycloak.model.KeycloakUser;
import org.folio.uk.support.TestConstants;
import org.folio.uk.support.TestValues;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

@IntegrationTest
class DeactivateUserIT extends BaseIntegrationTest {

  @Autowired private KeycloakClient keycloakClient;
  @Autowired private KafkaTemplate<String, Object> kafkaTemplate;

  @MockitoSpyBean private KeycloakService keycloakService;

  @BeforeAll
  static void beforeAll() {
    enableTenant(TENANT_NAME);
  }

  @AfterAll
  static void afterAll() {
    removeTenant(TENANT_NAME);
  }

  @Test
  @WireMockStub("/wiremock/stubs/users/create-user.json")
  void deactivateUser_positive() throws Exception {
    var user = TestConstants.user();
    var mvcResult = doPost("/users-keycloak/users", user).andReturn();
    var resp = parseResponse(mvcResult, User.class);
    assertThat(resp.getId()).isEqualTo(user.getId());
    assertKeycloakUser(user, true);

    var msg = TestValues.readValue("json/kafka/user-update-event-diactivated.json", UserDomainEvent.class);
    kafkaTemplate.send(FOLIO_USER_TOPIC, msg);

    await().atMost(FIVE_SECONDS).pollDelay(ONE_HUNDRED_MILLISECONDS).untilAsserted(() -> {
      assertKeycloakUser(user, false);
    });
  }

  private void assertKeycloakUser(User user, boolean active) {
    var kcUser = findKeycloakUserByUserId(user.getId());
    assertThat(kcUser).isPresent();
    assertThat(kcUser.get().getEnabled()).isEqualTo(active);
  }

  private Optional<KeycloakUser> findKeycloakUserByUserId(UUID userId) {
    var query = USER_ID_ATTR + ":" + userId;
    var found = keycloakClient.getUsersWithAttrs(TENANT_NAME, query, true, tokenService.issueToken());

    if (isEmpty(found)) {
      return Optional.empty();
    } else {
      return Optional.of(found.getFirst());
    }
  }
}
