package org.folio.uk.integration.kafka;

import static org.folio.integration.kafka.producer.KafkaUtils.getEnvTopicName;
import static org.folio.uk.integration.kafka.model.EntitlementAcknowledgement.ERROR_STATUS;
import static org.folio.uk.integration.kafka.model.EntitlementAcknowledgement.SUCCESS_STATUS;
import static org.folio.uk.integration.kafka.model.EntitlementAcknowledgement.SYSTEM_USER_TYPE;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.uk.integration.kafka.model.EntitlementAcknowledgement;
import org.folio.uk.integration.kafka.model.SystemUserEvent;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Publishes asynchronous entitlement processing results for MTE.
 */
@Log4j2
@Component
@RequiredArgsConstructor
public class EntitlementAcknowledgementPublisher {

  static final String ENTITLEMENT_CONFIRMATION_TOPIC = "entitlement-confirmation";

  private final KafkaTemplate<String, Object> kafkaTemplate;

  /**
   * Publishes a successful system-user entitlement acknowledgement.
   *
   * @param event processed system-user event
   */
  public void publishSuccess(SystemUserEvent event) {
    publish(event, SUCCESS_STATUS, null);
  }

  /**
   * Publishes a failed system-user entitlement acknowledgement.
   *
   * @param event system-user event that failed
   * @param exception processing exception
   */
  public void publishError(SystemUserEvent event, Exception exception) {
    publish(event, ERROR_STATUS, exception.getMessage());
  }

  private void publish(SystemUserEvent event, String status, String details) {
    var acknowledgement = EntitlementAcknowledgement.builder()
      .tenant(event.getTenant())
      .type(SYSTEM_USER_TYPE)
      .moduleId(getModuleId(event))
      .status(status)
      .details(details)
      .build();
    var topic = getEnvTopicName(ENTITLEMENT_CONFIRMATION_TOPIC);
    kafkaTemplate.send(topic, event.getTenant(), acknowledgement);
    log.debug("Entitlement acknowledgement sent: topic = {}, acknowledgement = {}", topic, acknowledgement);
  }

  private static String getModuleId(SystemUserEvent event) {
    var systemUser = event.getNewValue() != null ? event.getNewValue() : event.getOldValue();
    return systemUser == null ? null : systemUser.getModuleId();
  }
}
