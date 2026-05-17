package org.folio.uk.integration.kafka;

import static java.util.Objects.requireNonNull;
import static org.folio.common.utils.OkapiHeaders.URL;
import static org.folio.spring.integration.XOkapiHeaders.TENANT;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.folio.integration.kafka.model.ResourceEvent;
import org.folio.integration.kafka.model.TenantAwareEvent;
import org.folio.spring.FolioModuleMetadata;
import org.folio.spring.scope.FolioExecutionContextSetter;
import org.folio.uk.integration.configuration.OkapiConfigurationProperties;
import org.folio.uk.integration.kafka.model.SystemUserEvent;
import org.folio.uk.integration.kafka.model.UserEvent;
import org.folio.uk.integration.keycloak.SystemUserService;
import org.folio.uk.service.UserService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Log4j2
@Service
@RequiredArgsConstructor
public class KafkaMessageListener {

  private final FolioModuleMetadata metadata;
  private final SystemUserService systemUserService;
  private final UserService userService;
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
    requireNonNull(event.getType(), "Event type must not be null");
    log.info("System user event received: {}", event);

    handleEvent(event, e -> {
      switch (e.getType()) {
        case UPDATE -> systemUserService.updateOnEvent(e.getNewValue());
        case CREATE -> systemUserService.createOnEvent(e.getNewValue());
        case DELETE -> systemUserService.deleteOnEvent(e.getOldValue());
        default -> throw new IllegalStateException("Received system user event with unsupported type: " + e.getType());
      }
    });
  }

  @KafkaListener(
    id = "user-event-listener",
    containerFactory = "userKafkaListenerContainerFactory",
    groupId = "#{kafkaConsumerProperties.listener['user'].groupId}",
    topicPattern = "#{kafkaConsumerProperties.listener['user'].topicPattern}",
    concurrency = "#{kafkaConsumerProperties.listener['user'].concurrency}",
    filter = "tenantAwareMessageFilter")
  public void handleUserEvent(UserEvent event) {
    requireNonNull(event.getType(), "Event type must not be null");
    log.debug("User event received: {}", () -> briefView(event));

    handleEvent(event, e -> {
      switch (e.getType()) {
        case UPDATE -> userService.updateUserOnEvent(e.getNewValue(), e.getOldValue());
        case CREATE, DELETE ->
          log.debug("Received user event with type {} is ignored: eventId = {}", e.getType(), e.getId());
        default -> throw new IllegalStateException("Received user event with unsupported type: " + e.getType());
      }
    });
  }

  private <T extends TenantAwareEvent> void handleEvent(T event, Consumer<T> handler) {
    Map<String, Collection<String>> headers =
      Map.of(TENANT, List.of(event.getTenant()), URL, List.of(okapiProperties.getUrl()));
    try (var ignored = new FolioExecutionContextSetter(metadata, headers)) {
      handler.accept(event);
    }
  }

  private static String briefView(UserEvent event) {
    return new ToStringBuilder(event)
      .append("id", event.getId())
      .append("type", event.getType())
      .append("tenant", event.getTenant())
      .append("userId", event.getNewValue() != null
        ? event.getNewValue().getId()
        : event.getOldValue() != null ? event.getOldValue().getId() : null)
      .toString();
  }
}
