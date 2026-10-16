package org.folio.uk.it;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.awaitility.Awaitility.await;
import static org.awaitility.Durations.FIVE_SECONDS;
import static org.folio.common.utils.OkapiHeaders.TENANT;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.folio.integration.kafka.consumer.filter.TenantModuleVersionRepository;
import org.folio.test.types.IntegrationTest;
import org.folio.uk.base.BaseIntegrationTest;
import org.folio.uk.integration.keycloak.SystemUserService;
import org.folio.uk.support.TestConstants;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@IntegrationTest
@TestPropertySource(properties =
  "application.kafka.consumer.filtering.tenant-filter.enabled=true")
class KafkaMessageFilteringIT extends BaseIntegrationTest {

  private static final String CURRENT_TENANT = "filtercurrent";
  private static final String DIFFERENT_VERSION_TENANT = "filterdifferent";
  private static final String PRE_MARKER_TENANT = "filterlegacy";
  private static final String CURRENT_MODULE_ID = "mod-users-keycloak-0.0.1-TEST";
  private static final String DIFFERENT_MODULE_ID = "mod-users-keycloak-0.0.0";

  @MockitoBean private SystemUserService systemUserService;

  @Autowired private KafkaTemplate<String, Object> kafkaTemplate;
  @Autowired private JdbcOperations jdbcOperations;
  @Autowired private TenantModuleVersionRepository repository;

  @AfterEach
  void removeTenantSchemas() {
    dropSchema(CURRENT_TENANT);
    dropSchema(DIFFERENT_VERSION_TENANT);
    dropSchema(PRE_MARKER_TENANT);
  }

  @Test
  void shouldProcessMessageForTenantOwnedByCurrentVersion() throws Exception {
    recreateInitializedTenant(CURRENT_TENANT, true);
    repository.saveModuleVersion(schema(CURRENT_TENANT), CURRENT_MODULE_ID);

    sendSystemUserEvent(CURRENT_TENANT);

    await().atMost(FIVE_SECONDS).untilAsserted(() -> verifyProcessed(CURRENT_TENANT));
  }

  @Test
  void shouldSkipMessageForTenantOwnedByDifferentVersion() throws Exception {
    recreateInitializedTenant(DIFFERENT_VERSION_TENANT, true);
    repository.saveModuleVersion(schema(DIFFERENT_VERSION_TENANT), DIFFERENT_MODULE_ID);
    prepareCurrentTenant();

    sendSystemUserEvent(DIFFERENT_VERSION_TENANT);
    sendSystemUserEvent(CURRENT_TENANT);

    await().atMost(FIVE_SECONDS).untilAsserted(() -> verifyProcessed(CURRENT_TENANT));
    verifyNotProcessed(DIFFERENT_VERSION_TENANT);
  }

  @Test
  void shouldSkipMessageForTenantWhoseSchemaPredatesMarkerTable() throws Exception {
    recreateInitializedTenant(PRE_MARKER_TENANT, false);
    prepareCurrentTenant();

    sendSystemUserEvent(PRE_MARKER_TENANT);
    sendSystemUserEvent(CURRENT_TENANT);

    await().atMost(FIVE_SECONDS).untilAsserted(() -> verifyProcessed(CURRENT_TENANT));
    verifyNotProcessed(PRE_MARKER_TENANT);
  }

  private void prepareCurrentTenant() {
    recreateInitializedTenant(CURRENT_TENANT, true);
    repository.saveModuleVersion(schema(CURRENT_TENANT), CURRENT_MODULE_ID);
  }

  private void sendSystemUserEvent(String tenant) throws Exception {
    var event = TestConstants.systemUserResourceEvent();
    event.setTenant(tenant);
    var producerRecord = new ProducerRecord<String, Object>(FOLIO_SYSTEM_USER_TOPIC, event);
    producerRecord.headers().add(TENANT, tenant.getBytes(UTF_8));
    kafkaTemplate.send(producerRecord).get();
  }

  private void verifyProcessed(String tenant) {
    verify(systemUserService).createOnEvent(argThat(event -> tenant.equals(event.getTenant())));
  }

  private void verifyNotProcessed(String tenant) {
    verify(systemUserService, never()).createOnEvent(argThat(event -> tenant.equals(event.getTenant())));
  }

  @SuppressWarnings("java:S2077") // Test schemas are derived exclusively from constants in this class.
  private void recreateInitializedTenant(String tenant, boolean createMarkerTable) {
    var schema = schema(tenant);
    dropSchema(tenant);
    jdbcOperations.execute("CREATE SCHEMA \"%s\"".formatted(schema));
    jdbcOperations.execute("CREATE TABLE \"%s\".user_migration_job (id INTEGER PRIMARY KEY)".formatted(schema));
    if (createMarkerTable) {
      jdbcOperations.execute("""
        CREATE TABLE "%s".tenant_module_version (
          id INTEGER PRIMARY KEY,
          module_id VARCHAR(255) NOT NULL,
          updated_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
        )
        """.formatted(schema));
    }
  }

  @SuppressWarnings("java:S2077") // Test schemas are derived exclusively from constants in this class.
  private void dropSchema(String tenant) {
    jdbcOperations.execute("DROP SCHEMA IF EXISTS \"%s\" CASCADE".formatted(schema(tenant)));
  }

  private static String schema(String tenant) {
    return tenant + "_mod_users_keycloak";
  }
}
