package org.folio.uk.integration.kafka;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.folio.uk.support.TestConstants.TENANT_NAME;
import static org.folio.uk.support.TestConstants.USER_ID;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.Set;
import org.folio.integration.kafka.model.ResourceEventType;
import org.folio.test.types.UnitTest;
import org.folio.uk.domain.dto.User;
import org.folio.uk.integration.configuration.OkapiConfigurationProperties;
import org.folio.uk.integration.kafka.model.SystemUser;
import org.folio.uk.integration.kafka.model.SystemUserEvent;
import org.folio.uk.integration.kafka.model.UserEvent;
import org.folio.uk.integration.keycloak.SystemUserService;
import org.folio.uk.service.UserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@UnitTest
@ExtendWith(MockitoExtension.class)
class KafkaMessageListenerTest {

  @Mock private SystemUserService systemUserService;
  @Mock private UserService userService;
  @Mock private OkapiConfigurationProperties okapiProperties;
  @InjectMocks private KafkaMessageListener kafkaMessageListener;

  @BeforeEach
  void setUp() {
    when(okapiProperties.getUrl()).thenReturn("dummy");
  }

  @AfterEach
  void tearDown() {
    verifyNoMoreInteractions(systemUserService, userService);
  }

  @Test
  void handleSystemUserEvent_positive_deleteEvent() {
    var oldValue = systemUser();
    var event = SystemUserEvent.builder().type(ResourceEventType.DELETE).tenant(TENANT_NAME).oldValue(oldValue).build();

    kafkaMessageListener.handleSystemUserEvent(event);

    verify(systemUserService).deleteOnEvent(oldValue);
  }

  @Test
  void handleSystemUserEvent_positive_updateEvent() {
    var newValue = systemUser();
    var event = SystemUserEvent.builder().type(ResourceEventType.UPDATE).tenant(TENANT_NAME).newValue(newValue).build();

    kafkaMessageListener.handleSystemUserEvent(event);

    verify(systemUserService).updateOnEvent(newValue);
  }

  @Test
  void handleSystemUserEvent_positive_createEvent() {
    var newValue = systemUser();
    var event = SystemUserEvent.builder().type(ResourceEventType.CREATE).tenant(TENANT_NAME).newValue(newValue).build();

    kafkaMessageListener.handleSystemUserEvent(event);

    verify(systemUserService).createOnEvent(newValue);
  }

  @Test
  void handleSystemUserEvent_positive_unhandledEventType() {
    var event = SystemUserEvent.builder().type(ResourceEventType.DELETE_ALL).tenant(TENANT_NAME).build();

    kafkaMessageListener.handleSystemUserEvent(event);

    verifyNoInteractions(systemUserService);
  }

  @Test
  void handleUserEvent_positive_updateEvent() {
    var newValue = user(true);
    var oldValue = user(false);
    var event = UserEvent.builder()
      .type(ResourceEventType.UPDATE)
      .tenant(TENANT_NAME)
      .newValue(newValue)
      .oldValue(oldValue)
      .build();

    kafkaMessageListener.handleUserEvent(event);

    verify(userService).updateUserOnEvent(newValue, oldValue);
  }

  @Test
  void handleUserEvent_positive_createEventIsIgnored() {
    var event = UserEvent.builder()
      .type(ResourceEventType.CREATE)
      .id("event-id")
      .tenant(TENANT_NAME)
      .newValue(user(true))
      .build();

    kafkaMessageListener.handleUserEvent(event);

    verifyNoInteractions(userService);
  }

  @Test
  void handleUserEvent_positive_deleteEventIsIgnored() {
    var event = UserEvent.builder()
      .type(ResourceEventType.DELETE)
      .id("event-id")
      .tenant(TENANT_NAME)
      .oldValue(user(false))
      .build();

    kafkaMessageListener.handleUserEvent(event);

    verifyNoInteractions(userService);
  }

  @Test
  void handleUserEvent_negative_unsupportedEventType() {
    var event = UserEvent.builder()
      .type(ResourceEventType.DELETE_ALL)
      .tenant(TENANT_NAME)
      .build();

    assertThatThrownBy(() -> kafkaMessageListener.handleUserEvent(event))
      .isInstanceOf(IllegalStateException.class)
      .hasMessageContaining("Received user event with unsupported type: DELETE_ALL");
  }

  private static SystemUser systemUser() {
    return SystemUser.of("name", "type", Set.of("dummy"));
  }

  private static User user(boolean active) {
    return new User().id(USER_ID).active(active);
  }
}
