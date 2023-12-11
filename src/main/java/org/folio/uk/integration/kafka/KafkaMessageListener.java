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
import org.folio.uk.integration.kafka.model.ResourceEventType;
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
    var eventType = event.getType();

    if (eventType != ResourceEventType.CREATE) {
      return;
    }

    var folioHeaders = prepareFolioHeaders(event);
    try (var ignored = new FolioExecutionContextSetter(metadata, folioHeaders)) {
      var sysUserEvent = objectMapper.convertValue(event.getNewValue(), SystemUserEvent.class);
      asyncTaskExecutor.execute(getRunnableWithCurrentFolioContext(
        () -> systemUserService.createOnEvent(sysUserEvent)));
    }
  }

  private Map<String, Collection<String>> prepareFolioHeaders(ResourceEvent event) {
    return Map.of(
      TENANT, List.of(event.getTenant()),
      URL, List.of(okapiProperties.getUrl())
    );
  }
}
