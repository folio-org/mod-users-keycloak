package org.folio.uk.it;

import static org.awaitility.Awaitility.await;
import static org.awaitility.Durations.FIVE_SECONDS;
import static org.awaitility.Durations.ONE_SECOND;
import static org.awaitility.Durations.TWO_SECONDS;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import org.folio.test.TestUtils;
import org.folio.test.types.IntegrationTest;
import org.folio.uk.base.BaseIntegrationTest;
import org.folio.uk.integration.kafka.model.SystemUserEvent;
import org.folio.uk.integration.keycloak.SystemUserService;
import org.folio.uk.support.TestConstants;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@IntegrationTest
@TestPropertySource(properties = {
  "application.kafka.consumer.filtering.tenant-filter.enabled=true",
  "application.retry.system-user-event.retry-delay=10ms"
})
class KafkaMessageFilteringIT extends BaseIntegrationTest {

  @MockitoBean private SystemUserService systemUserService;

  @Autowired private KafkaTemplate<String, Object> kafkaTemplate;

  @Test
  void shouldSkipMessageForDisabledTenant_andProcessForEnabledTenant() {
    wmAdminClient.addStubMapping(
      TestUtils.readString("wiremock/stubs/entitlements/get-entitlements-disabled.json"));

    kafkaTemplate.send(FOLIO_SYSTEM_USER_TOPIC, TestConstants.systemUserResourceEvent());

    await().during(ONE_SECOND).atMost(TWO_SECONDS).untilAsserted(() ->
      verify(systemUserService, never()).createOnEvent(any(SystemUserEvent.class)));

    wmAdminClient.addStubMapping(
      TestUtils.readString("wiremock/stubs/entitlements/get-entitlements-enabled.json"));

    kafkaTemplate.send(FOLIO_SYSTEM_USER_TOPIC, TestConstants.systemUserResourceEvent());

    await().atMost(FIVE_SECONDS).untilAsserted(() ->
      verify(systemUserService).createOnEvent(any(SystemUserEvent.class)));
  }
}
