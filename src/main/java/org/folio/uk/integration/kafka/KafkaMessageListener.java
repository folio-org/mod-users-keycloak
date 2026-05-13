package org.folio.uk.integration.kafka;

import static org.folio.common.utils.OkapiHeaders.URL;
import static org.folio.spring.integration.XOkapiHeaders.TENANT;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.integration.kafka.model.ResourceEvent;
import org.folio.spring.FolioModuleMetadata;
import org.folio.spring.scope.FolioExecutionContextSetter;
import org.folio.uk.integration.configuration.OkapiConfigurationProperties;
import org.folio.uk.integration.kafka.model.SystemUserEvent;
import org.folio.uk.integration.keycloak.SystemUserService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Log4j2
@Service
@RequiredArgsConstructor
public class KafkaMessageListener {

  private final FolioModuleMetadata metadata;
  private final SystemUserService systemUserService;
  private final OkapiConfigurationProperties okapiProperties;

  /**
   * Handles system user event.
   *
   * @param event - system user {@link ResourceEvent} object
   */
  @KafkaListener(
    id = "system-user-event-listener",
    containerFactory = "kafkaListenerContainerFactory",
    groupId = "#{kafkaConsumerProperties.listener['system-user'].groupId}",
    topicPattern = "#{kafkaConsumerProperties.listener['system-user'].topicPattern}",
    filter = "tenantAwareMessageFilter")
  public void handleSystemUserEvent(SystemUserEvent event) {
    log.info("System user event received: {}", event);
    Map<String, Collection<String>> headers =
      Map.of(TENANT, List.of(event.getTenant()), URL, List.of(okapiProperties.getUrl()));
    try (var ignored = new FolioExecutionContextSetter(metadata, headers)) {
      switch (event.getType()) {
        case UPDATE -> systemUserService.updateOnEvent(event.getNewValue());
        case CREATE -> systemUserService.createOnEvent(event.getNewValue());
        case DELETE -> systemUserService.deleteOnEvent(event.getOldValue());
        default -> log.warn("Received system user event is not handled: {}", event);
      }
    }
  }
}
