package org.folio.uk.migration;

import static java.util.concurrent.CompletableFuture.allOf;
import static java.util.concurrent.CompletableFuture.runAsync;
import static org.apache.commons.collections4.ListUtils.partition;
import static org.folio.spring.scope.FolioExecutionScopeExecutionContextManager.getRunnableWithCurrentFolioContext;
import static org.folio.uk.utils.QueryUtils.convertFieldListToCqlQuery;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.folio.spring.FolioExecutionContext;
import org.folio.uk.domain.dto.UsersIdp;
import org.folio.uk.integration.keycloak.KeycloakService;
import org.folio.uk.integration.keycloak.config.KeycloakFederatedAuthProperties;
import org.folio.uk.migration.properties.IdpMigrationProperties;
import org.folio.uk.service.UserService;
import org.springframework.stereotype.Service;

@Log4j2
@Service
@RequiredArgsConstructor
public class IdpMigrationService {

  private static final String USER_ID = "id";

  private final UserService userService;
  private final KeycloakService keycloakService;
  private final FolioExecutionContext folioExecutionContext;
  private final IdpMigrationProperties idpMigrationProperties;
  private final KeycloakFederatedAuthProperties keycloakFederatedAuthProperties;

  public void linkUserIdpMigration(UsersIdp usersIdp) {
    if (Boolean.FALSE.equals(keycloakFederatedAuthProperties.isEnabled())) {
      log.info("Linking users to an IDP is disabled");
      return;
    }
    var centralTenantId = usersIdp.getCentralTenantId();
    if (!StringUtils.equals(folioExecutionContext.getTenantId(), centralTenantId)) {
      throw new IllegalStateException("Cannot link users to an IDP, supplied tenantId doesn't match context tenantId");
    }
    var userIds = new ArrayList<>(usersIdp.getUserIds());
    if (CollectionUtils.isEmpty(userIds)) {
      throw new IllegalStateException("Cannot link users to an IDP, no userIds is supplied in request body");
    }
    log.info("Linking {} user(s) in {} tenant", userIds.size(), centralTenantId);
    var partitions = partition(userIds, idpMigrationProperties.getBatchSize()).stream()
      .map(part -> runAsync(getRunnableWithCurrentFolioContext(() -> findAndLinkUserIdpByPart(part))))
      .toArray(CompletableFuture[]::new);
    allOf(partitions).whenComplete(migrationCompleteHandler(userIds.size()));
  }

  private void findAndLinkUserIdpByPart(List<UUID> userIds) {
    var query = convertFieldListToCqlQuery(userIds, USER_ID, true);
    userService.findUsers(query, Integer.MAX_VALUE).getUsers().forEach(user ->
      keycloakService.findKeycloakUserWithUserIdAttr(user.getId()).ifPresent(keycloakUser ->
        keycloakService.linkIdentityProviderToUser(user, keycloakUser.getId())));
  }

  private BiConsumer<Void, ? super Throwable> migrationCompleteHandler(int totalRecords) {
    return (result, ex) -> {
      if (Objects.nonNull(ex)) {
        log.error("User IDP linking Migration has failed", ex);
        return;
      }
      log.info("User IDP linking Migration has finished, total records: {}", totalRecords);
    };
  }
}
