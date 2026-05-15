package org.folio.uk.integration.kafka;

import static org.folio.common.utils.OkapiHeaders.URL;
import static org.folio.spring.integration.XOkapiHeaders.TENANT;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.integration.kafka.model.ResourceEvent;
import org.folio.integration.kafka.model.ResourceEventType;
import org.folio.integration.kafka.model.TenantAwareEvent;
import org.folio.spring.FolioModuleMetadata;
import org.folio.spring.scope.FolioExecutionContextSetter;
import org.folio.uk.integration.configuration.OkapiConfigurationProperties;
import org.folio.uk.integration.kafka.model.SystemUserEvent;
import org.folio.uk.integration.kafka.model.UserEvent;
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
    containerFactory = "systemUserKafkaListenerContainerFactory",
    groupId = "#{kafkaConsumerProperties.listener['system-user'].groupId}",
    topicPattern = "#{kafkaConsumerProperties.listener['system-user'].topicPattern}",
    filter = "tenantAwareMessageFilter")
  public void handleSystemUserEvent(SystemUserEvent event) {
    log.info("System user event received: {}", event);

    handleEvent(event, e -> {
      switch (e.getType()) {
        case UPDATE -> systemUserService.updateOnEvent(e.getNewValue());
        case CREATE -> systemUserService.createOnEvent(e.getNewValue());
        case DELETE -> systemUserService.deleteOnEvent(e.getOldValue());
        default -> log.warn("Received system user event is not handled: {}", e);
      }
    });
  }

  @KafkaListener(
    id = "user-event-listener",
    containerFactory = "userKafkaListenerContainerFactory",
    groupId = "#{kafkaConsumerProperties.listener['user'].groupId}",
    topicPattern = "#{kafkaConsumerProperties.listener['user'].topicPattern}",
    filter = "tenantAwareMessageFilter")
  public void handleUserEvent(UserEvent event) {
    if (event.getType() == ResourceEventType.UPDATE
      && event.getNewValue() != null && event.getOldValue() != null
      && event.getNewValue().getActive() != event.getOldValue().getActive()) {
      log.info("User active status changed, updating user accordingly: {}", event);

      handleEvent(event, e -> {
        var newValue = e.getNewValue();
        log.info("Updating user: {}", newValue.getUsername());
      });
    }
  }

  private <T extends TenantAwareEvent> void handleEvent(T event, Consumer<T> handler) {
    Map<String, Collection<String>> headers =
      Map.of(TENANT, List.of(event.getTenant()), URL, List.of(okapiProperties.getUrl()));
    try (var ignored = new FolioExecutionContextSetter(metadata, headers)) {
      handler.accept(event);
    }
  }
}
