package org.folio.uk.integration.kafka;

import static org.folio.common.utils.OkapiHeaders.URL;
import static org.folio.spring.integration.XOkapiHeaders.TENANT;
import static org.folio.spring.scope.FolioExecutionScopeExecutionContextManager.getRunnableWithCurrentFolioContext;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.spring.FolioModuleMetadata;
import org.folio.spring.scope.FolioExecutionContextSetter;
import org.folio.uk.integration.configuration.OkapiConfigurationProperties;
import org.folio.uk.integration.kafka.model.ResourceEvent;
import org.folio.uk.integration.kafka.model.SystemUserEvent;
import org.folio.uk.integration.keycloak.SystemUserService;
import org.springframework.core.task.TaskExecutor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Log4j2
@Service
@RequiredArgsConstructor
public class KafkaMessageListener {

  private final ObjectMapper objectMapper;
  private final FolioModuleMetadata metadata;
  private final SystemUserService systemUserService;
  private final TaskExecutor asyncTaskExecutor;
  private final OkapiConfigurationProperties okapiProperties;

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
      case UPDATE -> executeWithContext(event,
        () -> systemUserService.updateOnEvent(objectMapper.convertValue(event.getNewValue(), SystemUserEvent.class)));
      case CREATE -> executeWithContext(event,
        () -> systemUserService.createOnEvent(objectMapper.convertValue(event.getNewValue(), SystemUserEvent.class)));
      case DELETE -> executeWithContext(event,
        () -> systemUserService.deleteOnEvent(objectMapper.convertValue(event.getOldValue(), SystemUserEvent.class)));
      default -> log.warn("Received system user event is not handled: {}", event);
    }
  }

  private void executeWithContext(ResourceEvent event, Runnable task) {
    Map<String, Collection<String>> headers =
      Map.of(TENANT, List.of(event.getTenant()), URL, List.of(okapiProperties.getUrl()));
    try (var ignored = new FolioExecutionContextSetter(metadata, headers)) {
      asyncTaskExecutor.execute(getRunnableWithCurrentFolioContext(task));
    }
  }
}
