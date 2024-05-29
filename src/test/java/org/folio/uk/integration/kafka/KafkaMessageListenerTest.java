package org.folio.uk.integration.kafka;

import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Set;
import org.awaitility.Durations;
import org.folio.test.types.UnitTest;
import org.folio.uk.integration.configuration.OkapiConfigurationProperties;
import org.folio.uk.integration.kafka.model.ResourceEvent;
import org.folio.uk.integration.kafka.model.ResourceEventType;
import org.folio.uk.integration.kafka.model.SystemUserEvent;
import org.folio.uk.integration.keycloak.SystemUserService;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@UnitTest
@ExtendWith(MockitoExtension.class)
class KafkaMessageListenerTest {

  @Spy private static ThreadPoolTaskExecutor asyncTaskExecutor = new ThreadPoolTaskExecutor();
  @Spy private ObjectMapper objectMapper = new ObjectMapper();
  @Mock private SystemUserService systemUserService;
  @Mock private OkapiConfigurationProperties okapiProperties;
  @InjectMocks private KafkaMessageListener kafkaMessageListener;

  @BeforeAll
  static void beforeAll() {
    asyncTaskExecutor.setCorePoolSize(1);
    asyncTaskExecutor.initialize();
  }

  @AfterAll
  static void afterAll() {
    asyncTaskExecutor.shutdown();
  }

  @BeforeEach
  void setUp() {
    when(okapiProperties.getUrl()).thenReturn("dummy");
  }

  @Test
  void handleSystemUserEvent_positive_deleteEvent() {
    var event = ResourceEvent.builder().type(ResourceEventType.DELETE).tenant("tenant").build();

    kafkaMessageListener.handleSystemUserEvent(event);
    await().atMost(Durations.FIVE_SECONDS).untilAsserted(() -> verify(systemUserService).delete());
  }

  @Test
  void handleSystemUserEvent_positive_updateEvent() {
    var newValue = SystemUserEvent.of("name", "type", Set.of("dummy"));
    var event = ResourceEvent.builder().type(ResourceEventType.UPDATE).tenant("tenant").newValue(newValue).build();

    kafkaMessageListener.handleSystemUserEvent(event);
    await().atMost(Durations.FIVE_SECONDS).untilAsserted(() -> verify(systemUserService).updateOnEvent(newValue));
  }

  @Test
  void handleSystemUserEvent_positive_createEvent() {
    var newValue = SystemUserEvent.of("name", "type", Set.of("dummy"));
    var event = ResourceEvent.builder().type(ResourceEventType.CREATE).tenant("tenant").newValue(newValue).build();

    kafkaMessageListener.handleSystemUserEvent(event);
    await().atMost(Durations.FIVE_SECONDS).untilAsserted(() -> verify(systemUserService).createOnEvent(newValue));
  }
}
