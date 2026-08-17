package org.folio.uk.integration.kafka;

import static org.folio.integration.kafka.producer.KafkaUtils.getEnvTopicName;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import java.util.Set;
import org.folio.integration.kafka.model.ResourceEventType;
import org.folio.test.types.UnitTest;
import org.folio.uk.integration.kafka.model.EntitlementAcknowledgement;
import org.folio.uk.integration.kafka.model.SystemUser;
import org.folio.uk.integration.kafka.model.SystemUserEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

@UnitTest
@ExtendWith(MockitoExtension.class)
class EntitlementAcknowledgementPublisherTest {

  private static final String TENANT = "testtenant";
  private static final String MODULE_ID = "mod-foo-1.0.0";

  @Mock private KafkaTemplate<String, Object> kafkaTemplate;

  @AfterEach
  void tearDown() {
    verifyNoMoreInteractions(kafkaTemplate);
  }

  @Test
  void publishSuccess_positive_sendsSystemUserAcknowledgement() {
    var event = event(ResourceEventType.UPDATE, systemUser(MODULE_ID), systemUser("mod-foo-0.9.0"));
    var publisher = new EntitlementAcknowledgementPublisher(kafkaTemplate);

    publisher.publishSuccess(event);

    var expected = acknowledgement(MODULE_ID, EntitlementAcknowledgement.SUCCESS_STATUS, null);
    verify(kafkaTemplate).send(getEnvTopicName(EntitlementAcknowledgementPublisher.ENTITLEMENT_CONFIRMATION_TOPIC),
      TENANT, expected);
  }

  @Test
  void publishError_negative_sendsErrorDetails() {
    var event = event(ResourceEventType.DELETE, null, systemUser(MODULE_ID));
    var publisher = new EntitlementAcknowledgementPublisher(kafkaTemplate);
    var exception = new IllegalStateException("Keycloak unavailable");

    publisher.publishError(event, exception);

    var expected = acknowledgement(MODULE_ID, EntitlementAcknowledgement.ERROR_STATUS, exception.getMessage());
    verify(kafkaTemplate).send(getEnvTopicName(EntitlementAcknowledgementPublisher.ENTITLEMENT_CONFIRMATION_TOPIC),
      TENANT, expected);
  }

  private static SystemUserEvent event(ResourceEventType type, SystemUser newValue, SystemUser oldValue) {
    return SystemUserEvent.builder()
      .type(type)
      .tenant(TENANT)
      .newValue(newValue)
      .oldValue(oldValue)
      .build();
  }

  private static SystemUser systemUser(String moduleId) {
    return SystemUser.of(moduleId, "mod-foo", "module", Set.of("foo.bar"));
  }

  private static EntitlementAcknowledgement acknowledgement(String moduleId, String status, String details) {
    return EntitlementAcknowledgement.builder()
      .tenant(TENANT)
      .type(EntitlementAcknowledgement.SYSTEM_USER_TYPE)
      .moduleId(moduleId)
      .status(status)
      .details(details)
      .build();
  }
}
