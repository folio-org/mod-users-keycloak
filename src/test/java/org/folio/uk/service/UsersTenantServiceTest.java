package org.folio.uk.service;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import org.folio.spring.FolioExecutionContext;
import org.folio.spring.liquibase.FolioSpringLiquibase;
import org.folio.tenant.domain.dto.TenantAttributes;
import org.folio.test.types.UnitTest;
import org.folio.uk.integration.kafka.KafkaAdminService;
import org.folio.uk.integration.keycloak.KeycloakRealmManagementService;
import org.folio.uk.integration.keycloak.SystemUserService;
import org.folio.uk.integration.keycloak.TokenService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

@UnitTest
@ExtendWith(MockitoExtension.class)
class UsersTenantServiceTest {

  private static final TenantAttributes TENANT_ATTRIBUTES = new TenantAttributes();

  @Mock private JdbcTemplate jdbcTemplate;
  @Mock private FolioExecutionContext context;
  @Mock private FolioSpringLiquibase folioSpringLiquibase;

  @Mock private SystemUserService systemUserService;
  @Mock private TokenService tokenService;
  @Mock private KeycloakRealmManagementService realmService;

  @Mock private KafkaAdminService kafkaAdminService;

  @InjectMocks private UsersTenantService service;

  @Test
  void afterTenantUpdate_positive() {
    service.afterTenantUpdate(TENANT_ATTRIBUTES);

    verify(tokenService).renewToken();
    verify(systemUserService).create();
    verify(realmService).setupRealm();
    verify(kafkaAdminService).restartEventListeners();
  }

  @Test
  void afterTenantUpdate_negative() {
    doThrow(new RuntimeException("Failure")).when(tokenService).renewToken();

    assertThatThrownBy(() -> service.afterTenantUpdate(TENANT_ATTRIBUTES));

    verifyNoInteractions(systemUserService, realmService, kafkaAdminService);
  }

  @Test
  void afterTenantDeletion_positive() {
    service.afterTenantDeletion(TENANT_ATTRIBUTES);

    verify(systemUserService).delete();
    verify(realmService).cleanupRealm();
  }
}
