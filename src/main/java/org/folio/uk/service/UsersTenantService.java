package org.folio.uk.service;

import org.folio.spring.FolioExecutionContext;
import org.folio.spring.liquibase.FolioSpringLiquibase;
import org.folio.spring.service.TenantService;
import org.folio.tenant.domain.dto.TenantAttributes;
import org.folio.uk.integration.kafka.KafkaAdminService;
import org.folio.uk.integration.keycloak.KeycloakRealmManagementService;
import org.folio.uk.integration.keycloak.SystemUserService;
import org.folio.uk.integration.keycloak.TokenService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class UsersTenantService extends TenantService {

  private final SystemUserService systemUserService;
  private final TokenService tokenService;
  private final KafkaAdminService kafkaAdminService;
  private final KeycloakRealmManagementService realmService;

  public UsersTenantService(
    JdbcTemplate jdbcTemplate,
    FolioExecutionContext context,
    FolioSpringLiquibase folioSpringLiquibase,
    SystemUserService systemUserService,
    TokenService tokenService,
    KafkaAdminService kafkaAdmin,
    KeycloakRealmManagementService realmService) {
    super(jdbcTemplate, context, folioSpringLiquibase);
    this.systemUserService = systemUserService;
    this.tokenService = tokenService;
    this.kafkaAdminService = kafkaAdmin;
    this.realmService = realmService;
  }

  @Override
  protected void afterTenantUpdate(TenantAttributes tenantAttributes) {
    tokenService.renewToken();
    systemUserService.create();
    realmService.setupRealm();
    kafkaAdminService.restartEventListeners();
  }

  @Override
  protected void afterTenantDeletion(TenantAttributes tenantAttributes) {
    systemUserService.delete();
    realmService.cleanupRealm();
  }
}
