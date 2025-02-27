package org.folio.uk.migration;

import static org.apache.commons.collections4.ListUtils.partition;
import static org.folio.spring.scope.FolioExecutionScopeExecutionContextManager.getRunnableWithCurrentFolioContext;
import static org.folio.spring.utils.FolioExecutionContextUtils.prepareContextForTenant;

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
import org.folio.spring.FolioModuleMetadata;
import org.folio.spring.scope.FolioExecutionContextSetter;
import org.folio.uk.domain.dto.UsersIdp;
import org.folio.uk.integration.keycloak.KeycloakService;
import org.folio.uk.migration.properties.UserMigrationProperties;
import org.folio.uk.service.UserService;
import org.springframework.stereotype.Service;

@Log4j2
@Service
@RequiredArgsConstructor
public class IdpMigrationService {

  private final UserService userService;
  private final KeycloakService keycloakService;
  private final FolioModuleMetadata folioModuleMetadata;
  private final FolioExecutionContext folioExecutionContext;
  private final UserMigrationProperties migrationProperties;

  public void linkUserIdpMigration(UsersIdp usersIdp) {
    var tenantId = usersIdp.getTenantId();
    if (StringUtils.isEmpty(tenantId)) {
      throw new IllegalStateException("Cannot link users to IDP, no tenant id is supplied in request body");
    }

    var userIds = new ArrayList<>(usersIdp.getUserIds());
    if (CollectionUtils.isEmpty(userIds)) {
      throw new IllegalStateException("Cannot link users to IDP, no userIds is supplied in request body");
    }

    log.info("Linking {} user(s) in {} tenant", userIds.size(), tenantId);

    // Should switch to a central tenant in order to fetch shadow users of the incoming staff user ids
    try (var ignored = new FolioExecutionContextSetter(
      prepareContextForTenant(tenantId, folioModuleMetadata, folioExecutionContext))) {

      var partitions = partition(userIds, migrationProperties.getBatchSize());
      CompletableFuture.allOf(partitions.stream()
          .map(part -> CompletableFuture.runAsync(getRunnableWithCurrentFolioContext(
            () -> findAndLinkUserIdpByPart(part))))
          .toArray(CompletableFuture[]::new))
        .whenComplete(migrationCompleteHandler(userIds.size()));
    }
  }

  private void findAndLinkUserIdpByPart(List<UUID> userIds) {
    userIds.forEach(userId ->
      userService.getUser(userId).ifPresent(user ->
        keycloakService.findKeycloakUserWithUserIdAttr(user.getId()).ifPresent(keycloakUser ->
          keycloakService.linkIdentityProviderToUser(user, keycloakUser.getId()))));
  }

  private BiConsumer<Void, ? super Throwable> migrationCompleteHandler(int totalRecords) {
    return (result, ex) -> {
      if (Objects.isNull(ex)) {
        log.info("User IDP linking Migration has finished, total records: {}", totalRecords);
        return;
      }
      log.error("User IDP linking Migration has failed", ex);
    };
  }
}
