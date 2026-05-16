package org.folio.uk.it;

import static org.awaitility.Awaitility.await;
import static org.awaitility.Durations.FIVE_SECONDS;
import static org.folio.uk.support.TestConstants.TENANT_NAME;
import static org.folio.uk.support.TestConstants.USER_ID;
import static org.mockito.Mockito.verify;

import org.folio.test.types.IntegrationTest;
import org.folio.uk.base.BaseIntegrationTest;
import org.folio.uk.base.model.UserDomainEvent;
import org.folio.uk.integration.keycloak.KeycloakClient;
import org.folio.uk.integration.keycloak.KeycloakService;
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
  void updateOnEvent() {
    var msg = TestValues.readValue("json/kafka/user-update-event-diactivated.json", UserDomainEvent.class);
    kafkaTemplate.send(FOLIO_USER_TOPIC, msg);

    await().atMost(FIVE_SECONDS).untilAsserted(() -> {
      verify(keycloakService).disableUser(USER_ID);
    });

    /*var authToken = tokenService.issueToken();
    var systemUsersList = keycloakClient.findUsersByUsername(TENANT_NAME, "mod-foo", true, authToken);*/
  }
}
