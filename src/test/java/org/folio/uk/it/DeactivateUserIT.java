package org.folio.uk.it;

import static org.folio.uk.support.TestConstants.TENANT_NAME;

import java.util.UUID;
import org.folio.test.TestUtils;
import org.folio.test.types.IntegrationTest;
import org.folio.uk.base.BaseIntegrationTest;
import org.folio.uk.integration.keycloak.KeycloakClient;
import org.folio.uk.integration.users.UsersClient;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

@IntegrationTest
class DeactivateUserIT extends BaseIntegrationTest {

  private static final UUID USER_ID = UUID.fromString("de5bb75d-e696-4d43-9df8-289f39367079");

  @Autowired private KeycloakClient keycloakClient;
  @Autowired private KafkaTemplate<String, Object> kafkaTemplate;

  @MockitoSpyBean private UsersClient usersClient;

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
    var msg = TestUtils.readString("json/kafka/system-user-event.json");
    kafkaTemplate.send(FOLIO_USER_TOPIC, msg);
  }
}
