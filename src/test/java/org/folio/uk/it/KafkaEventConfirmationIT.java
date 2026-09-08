package org.folio.uk.it;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.awaitility.Durations.FIVE_SECONDS;
import static org.folio.integration.kafka.model.ResourceEventType.CREATE;
import static org.folio.integration.kafka.model.ResourceEventType.DELETE;
import static org.folio.integration.kafka.model.ResourceResultStatus.FAILURE;
import static org.folio.integration.kafka.model.ResourceResultStatus.SUCCESS;
import static org.folio.test.FakeKafkaConsumer.getEvents;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;

import java.util.Set;
import org.apache.kafka.clients.admin.NewTopic;
import org.folio.integration.kafka.consumer.confirmation.ResourceResultEventPublisher;
import org.folio.integration.kafka.model.ResourceResultEvent;
import org.folio.integration.kafka.model.ResourceResultStatus;
import org.folio.test.FakeKafkaConsumer;
import org.folio.test.types.IntegrationTest;
import org.folio.uk.base.BaseIntegrationTest;
import org.folio.uk.integration.kafka.model.SystemUser;
import org.folio.uk.integration.kafka.model.SystemUserEvent;
import org.folio.uk.integration.keycloak.SystemUserService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

@IntegrationTest
@TestPropertySource(properties = "application.event-confirmation.enabled=true")
class KafkaEventConfirmationIT extends BaseIntegrationTest {

  private static final String CONFIRMATION_TOPIC = "it-test.mgr-tenant-entitlements.resource-result";
  private static final String EVENT_ID = "5f26fe20-d7bc-11ef-9cd2-0242ac120002";
  private static final String RESOURCE_NAME = "System user";
  private static final String MODULE_ID = "mod-foo-1.0.0";

  @MockitoSpyBean private SystemUserService systemUserService;
  @Autowired private ResourceResultEventPublisher eventPublisher;
  @Autowired private KafkaTemplate<String, Object> kafkaTemplate;

  @BeforeAll
  static void beforeAll(@Autowired FakeKafkaConsumer fakeKafkaConsumer) {
    fakeKafkaConsumer.registerTopic(CONFIRMATION_TOPIC, ResourceResultEvent.class);
  }

  @BeforeEach
  void beforeEach() {
    FakeKafkaConsumer.removeAllEvents();
  }

  @Test
  void createOnEvent_positive_publishesSuccessConfirmation() {
    doAnswer(inv -> {
      var e = inv.getArgument(0, SystemUserEvent.class);
      eventPublisher.publishSuccessFor(e, MODULE_ID);
      return null;
    }).when(systemUserService).createOnEvent(any());

    kafkaTemplate.send(FOLIO_SYSTEM_USER_TOPIC, systemUserEvent(CREATE));

    awaitConfirmation(EVENT_ID, TENANT_NAME, RESOURCE_NAME, MODULE_ID, SUCCESS, false);
  }

  @Test
  void deleteOnEvent_positive_publishesSuccessConfirmation() {
    doAnswer(inv -> {
      var e = inv.getArgument(0, SystemUserEvent.class);
      eventPublisher.publishSuccessFor(e, MODULE_ID);
      return null;
    }).when(systemUserService).deleteOnEvent(any());

    kafkaTemplate.send(FOLIO_SYSTEM_USER_TOPIC, systemUserEvent(DELETE));

    awaitConfirmation(EVENT_ID, TENANT_NAME, RESOURCE_NAME, MODULE_ID, SUCCESS, false);
  }

  @Test
  void createOnEvent_negative_publishesFailureConfirmation() {
    doThrow(new RuntimeException("system user creation failed"))
      .when(systemUserService).createOnEvent(any());

    kafkaTemplate.send(FOLIO_SYSTEM_USER_TOPIC, systemUserEvent(CREATE));

    awaitConfirmation(EVENT_ID, TENANT_NAME, RESOURCE_NAME, MODULE_ID, FAILURE, true);
  }

  @Test
  void deleteOnEvent_negative_publishesFailureConfirmation() {
    doThrow(new RuntimeException("system user deletion failed"))
      .when(systemUserService).deleteOnEvent(any());

    kafkaTemplate.send(FOLIO_SYSTEM_USER_TOPIC, systemUserEvent(DELETE));

    awaitConfirmation(EVENT_ID, TENANT_NAME, RESOURCE_NAME, MODULE_ID, FAILURE, true);
  }

  private static void awaitConfirmation(String id, String tenant, String resourceName,
    String moduleId, ResourceResultStatus status, boolean withDetails) {
    await().atMost(FIVE_SECONDS).untilAsserted(() -> {
      var events = getEvents(CONFIRMATION_TOPIC, ResourceResultEvent.class);
      assertThat(events).hasSize(1);
      var c = events.getFirst().value();
      assertThat(c.getId()).isEqualTo(id);
      assertThat(c.getTenant()).isEqualTo(tenant);
      assertThat(c.getResourceName()).isEqualTo(resourceName);
      assertThat(c.getModuleId()).isEqualTo(moduleId);
      assertThat(c.getStatus()).isEqualTo(status);
      if (withDetails) {
        assertThat(c.getDetails()).isNotNull();
      }
    });
  }

  private static SystemUserEvent systemUserEvent(org.folio.integration.kafka.model.ResourceEventType type) {
    var systemUser = SystemUser.of(MODULE_ID, "mod-foo", "module", Set.of("foo.bar"));
    return switch (type) {
      case CREATE -> SystemUserEvent.builder()
        .id(EVENT_ID).type(CREATE).tenant(TENANT_NAME).resourceName(RESOURCE_NAME)
        .newValue(systemUser).build();
      case DELETE -> SystemUserEvent.builder()
        .id(EVENT_ID).type(DELETE).tenant(TENANT_NAME).resourceName(RESOURCE_NAME)
        .oldValue(systemUser).build();
      default -> throw new UnsupportedOperationException("Unsupported event type: " + type);
    };
  }

  @TestConfiguration
  static class ConfirmationTopicConfiguration {

    @Bean
    public NewTopic confirmationTopic() {
      return new NewTopic(CONFIRMATION_TOPIC, 1, (short) 1);
    }
  }
}
