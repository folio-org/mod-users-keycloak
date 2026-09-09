package org.folio.uk.it;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.awaitility.Durations.FIVE_SECONDS;
import static org.awaitility.Durations.TEN_SECONDS;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

import org.folio.test.TestUtils;
import org.folio.test.extensions.impl.WireMockAdminClient;
import org.folio.test.types.IntegrationTest;
import org.folio.uk.base.BaseIntegrationTest;
import org.folio.uk.integration.kafka.model.SystemUserEvent;
import org.folio.uk.integration.keycloak.SystemUserService;
import org.folio.uk.support.TestConstants;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@IntegrationTest
@TestPropertySource(properties = {
  "application.kafka.consumer.filtering.tenant-filter.enabled=true",
  "application.kafka.consumer.filtering.tenant-filter.tenant-disabled-strategy=FAIL",
  "application.retry.system-user-event.retry-delay=10ms"
})
class KafkaMessageFilteringFailStrategyIT extends BaseIntegrationTest {

  private static final String ENTITLEMENTS_PATH =
    "/entitlements/modules/mod-users-keycloak-0.0.1-TEST";

  @MockitoBean private SystemUserService systemUserService;

  @Autowired private KafkaTemplate<String, Object> kafkaTemplate;

  @Test
  void shouldRetryMessageUntilTenantBecomesEnabled() {
    wmAdminClient.addStubMapping(
      TestUtils.readString("wiremock/stubs/entitlements/get-entitlements-disabled.json"));

    kafkaTemplate.send(FOLIO_SYSTEM_USER_TOPIC, TestConstants.systemUserResourceEvent());

    await().atMost(TEN_SECONDS).untilAsserted(() ->
      assertThat(wmAdminClient.requestCount(entitlementsRequestCriteria())).isGreaterThanOrEqualTo(3));

    wmAdminClient.addStubMapping(
      TestUtils.readString("wiremock/stubs/entitlements/get-entitlements-enabled.json"));

    await().atMost(FIVE_SECONDS).untilAsserted(() ->
      verify(systemUserService).createOnEvent(any(SystemUserEvent.class)));
  }

  private static WireMockAdminClient.RequestCriteria entitlementsRequestCriteria() {
    return WireMockAdminClient.RequestCriteria.builder()
      .urlPath(ENTITLEMENTS_PATH)
      .method(HttpMethod.GET)
      .build();
  }
}
