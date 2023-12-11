package org.folio.uk.integration.keycloak;

import static org.folio.common.utils.KeycloakPermissionUtils.toPermissionName;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

@Service
@Log4j2
@RequiredArgsConstructor
public class KeycloakRealmManagementService {
  private static final String PASSWORD_RESET_POLICY = "Password Reset policy";
  private static final List<String> RESOURCES =
    List.of("/users-keycloak/password-reset/reset", "/users-keycloak/password-reset/validate");
  private static final List<String> SCOPES = List.of("POST");

  private final KeycloakService keycloakService;
  private final RealmConfigurationProvider realmConfigurationProvider;

  public void setupRealm() {
    log.info("Creating Keycloak permissions for password reset client");

    RESOURCES.stream()
      .forEach(resource -> keycloakService.createScopePermission(PASSWORD_RESET_POLICY, resource, SCOPES));
  }

  public void cleanupRealm() {
    log.info("Removing Keycloak permissions for password reset client");

    RESOURCES.stream()
      .map(resource -> toPermissionName(SCOPES, PASSWORD_RESET_POLICY, resource))
      .forEach(keycloakService::deleteScopePermission);

    realmConfigurationProvider.evictAllClientConfigurations();
  }
}
