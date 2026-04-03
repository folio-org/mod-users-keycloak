package org.folio.uk.integration.kafka;

import static org.mockito.Mockito.verify;

import java.util.Set;
import org.folio.test.types.UnitTest;
import org.folio.uk.integration.kafka.model.ResourceEvent;
import org.folio.uk.integration.kafka.model.ResourceEventType;
import org.folio.uk.integration.kafka.model.SystemUserEvent;
import org.folio.uk.integration.keycloak.SystemUserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;

@UnitTest
@ExtendWith(MockitoExtension.class)
class KafkaMessageListenerTest {

  @Spy private ObjectMapper objectMapper = new ObjectMapper();
  @Mock private SystemUserService systemUserService;
  @InjectMocks private KafkaMessageListener kafkaMessageListener;

  @Test
  void handleSystemUserEvent_positive_deleteEvent() {
    var oldValue = getSystemUserEvent();
    var event = ResourceEvent.builder().type(ResourceEventType.DELETE).tenant("tenant").oldValue(oldValue).build();

    kafkaMessageListener.handleSystemUserEvent(event);
    verify(systemUserService).deleteOnEvent(oldValue);
  }

  @Test
  void handleSystemUserEvent_positive_updateEvent() {
    var newValue = getSystemUserEvent();
    var event = ResourceEvent.builder().type(ResourceEventType.UPDATE).tenant("tenant").newValue(newValue).build();

    kafkaMessageListener.handleSystemUserEvent(event);
    verify(systemUserService).updateOnEvent(newValue);
  }

  @Test
  void handleSystemUserEvent_positive_createEvent() {
    var newValue = getSystemUserEvent();
    var event = ResourceEvent.builder().type(ResourceEventType.CREATE).tenant("tenant").newValue(newValue).build();

    kafkaMessageListener.handleSystemUserEvent(event);
    verify(systemUserService).createOnEvent(newValue);
  }

  private static SystemUserEvent getSystemUserEvent() {
    return SystemUserEvent.of("name", "type", Set.of("dummy"));
  }
}
