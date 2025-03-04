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
import org.folio.uk.domain.dto.User;
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
    log.info("Started user IDP linking migration");
    applyUserIdpMigration(usersIdp, keycloakService::linkIdentityProviderToUser);
  }

  public void unlinkUserIdpMigration(UsersIdp usersIdp) {
    log.info("Started user IDP unlinking migration");
    applyUserIdpMigration(usersIdp, keycloakService::unlinkIdentityProviderFromUser);
  }

  private void applyUserIdpMigration(UsersIdp usersIdp, BiConsumer<User, String> kcOperation) {
    if (Boolean.FALSE.equals(keycloakFederatedAuthProperties.isEnabled())) {
      log.info("Applying user IDP migration is disabled");
      return;
    }
    var centralTenantId = usersIdp.getCentralTenantId();
    var contextTenantId = folioExecutionContext.getTenantId();
    if (!StringUtils.equals(centralTenantId, contextTenantId)) {
      throw new IllegalStateException(String.format("Cannot apply user IDP migration, supplied centralTenantId '%s' "
        + "does not match context tenantId '%s'", centralTenantId, contextTenantId));
    }
    var userIds = new ArrayList<>(usersIdp.getUserIds());
    if (CollectionUtils.isEmpty(userIds)) {
      throw new IllegalStateException("Cannot apply user IDP migration, no userIds are supplied in request body");
    }
    log.info("Applying IDP migration to {} user(s) in {} tenant", userIds.size(), centralTenantId);
    var partitions = partition(userIds, idpMigrationProperties.getBatchSize()).stream()
      .map(part -> runAsync(getRunnableWithCurrentFolioContext(
        () -> findAndLinkUserIdpByPart(part, kcOperation))))
      .toArray(CompletableFuture[]::new);
    allOf(partitions).whenComplete(migrationCompleteHandler(userIds.size()));
  }

  private void findAndLinkUserIdpByPart(List<UUID> userIds, BiConsumer<User, String> kcOperation) {
    var query = convertFieldListToCqlQuery(userIds, USER_ID, true);
    userService.findUsers(query, Integer.MAX_VALUE).getUsers().forEach(user ->
      keycloakService.findKeycloakUserWithUserIdAttr(user.getId()).ifPresent(
        keycloakUser -> kcOperation.accept(user, keycloakUser.getId())));
  }

  private BiConsumer<Void, ? super Throwable> migrationCompleteHandler(int totalRecords) {
    return (result, ex) -> {
      if (Objects.nonNull(ex)) {
        log.error("User IDP migration has failed", ex);
        return;
      }
      log.info("User IDP migration has finished, total records: {}", totalRecords);
    };
  }
}
