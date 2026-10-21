package org.folio.uk.it;

import static org.assertj.core.api.Assertions.assertThat;
import static org.folio.common.utils.OkapiHeaders.TENANT;
import static org.folio.uk.support.TestConstants.TENANT_NAME;
import static org.mockito.Mockito.verify;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.folio.integration.kafka.consumer.filter.TenantModuleVersionRepository;
import org.folio.spring.FolioModuleMetadata;
import org.folio.spring.scope.FolioExecutionContextSetter;
import org.folio.test.types.IntegrationTest;
import org.folio.uk.base.BaseIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

@IntegrationTest
class TenantModuleVersionLifecycleIT extends BaseIntegrationTest {

  @Autowired private JdbcOperations jdbcOperations;
  @Autowired private FolioModuleMetadata folioModuleMetadata;

  @MockitoSpyBean private TenantModuleVersionRepository repository;

  @Test
  void tenantLifecycle_enableAndDelete_managesModuleVersionMarker() {
    enableTenant(TENANT_NAME);

    try {
      var moduleId = withTenant(() -> repository.findModuleVersion().orElseThrow());
      assertThat(moduleId).isEqualTo(folioModuleMetadata.getModuleId());
      assertThat(withTenant(repository::markerTableExists)).isTrue();
    } finally {
      removeTenant(TENANT_NAME);
    }

    verify(repository).deleteModuleVersion();
    assertThat(tenantSchemaExists()).isFalse();
  }

  private boolean tenantSchemaExists() {
    var schema = folioModuleMetadata.getDBSchemaName(TENANT_NAME);
    var exists = jdbcOperations.queryForObject("""
      SELECT EXISTS (
        SELECT 1
        FROM information_schema.schemata
        WHERE schema_name = ?
      )
      """, Boolean.class, schema);
    return Boolean.TRUE.equals(exists);
  }

  private <T> T withTenant(java.util.function.Supplier<T> operation) {
    var headers = Map.<String, Collection<String>>of(TENANT, List.of(TENANT_NAME));
    try (var ignored = new FolioExecutionContextSetter(folioModuleMetadata, headers)) {
      return operation.get();
    }
  }
}
