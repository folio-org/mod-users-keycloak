package org.folio.uk.integration.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.uk.integration.kafka.model.ResourceEvent;
import org.folio.uk.integration.kafka.model.SystemUserEvent;
import org.folio.uk.integration.keycloak.SystemUserService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

@Log4j2
@Service
@RequiredArgsConstructor
public class KafkaMessageListener {

  private final ObjectMapper objectMapper;
  private final SystemUserService systemUserService;

  /**
   * Handles system user event.
   *
   * @param event - system user {@link ResourceEvent} object
   */
  @KafkaListener(
    id = "system-user-event-listener",
    containerFactory = "kafkaListenerContainerFactory",
    groupId = "#{folioKafkaProperties.listener['system-user'].groupId}",
    topicPattern = "#{folioKafkaProperties.listener['system-user'].topicPattern}")
  public void handleSystemUserEvent(ResourceEvent event) {
    log.info("System user event received: {}", event);

    switch (event.getType()) {
      case UPDATE ->
        systemUserService.updateOnEvent(objectMapper.convertValue(event.getNewValue(), SystemUserEvent.class));
      case CREATE ->
        systemUserService.createOnEvent(objectMapper.convertValue(event.getNewValue(), SystemUserEvent.class));
      case DELETE ->
        systemUserService.deleteOnEvent(objectMapper.convertValue(event.getOldValue(), SystemUserEvent.class));
      default -> log.warn("Received system user event is not handled: {}", event);
    }
  }
}
