package org.folio.uk.integration.keycloak;

import static java.lang.String.format;
import static java.util.Collections.singletonList;
import static java.util.List.of;
import static java.util.Objects.nonNull;
import static java.util.UUID.fromString;
import static org.apache.commons.collections4.CollectionUtils.isEmpty;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.folio.common.utils.KeycloakPermissionUtils.toPermissionName;
import static org.folio.uk.integration.keycloak.model.KeycloakUser.USER_ID_ATTR;

import feign.FeignException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.folio.spring.FolioExecutionContext;
import org.folio.uk.domain.dto.User;
import org.folio.uk.exception.RequestValidationException;
import org.folio.uk.integration.keycloak.config.KeycloakLoginClientProperties;
import org.folio.uk.integration.keycloak.model.Client;
import org.folio.uk.integration.keycloak.model.Credential;
import org.folio.uk.integration.keycloak.model.KeycloakUser;
import org.folio.uk.integration.keycloak.model.ScopePermission;
import org.springframework.stereotype.Component;

@Log4j2
@Component
@RequiredArgsConstructor
public class KeycloakService {

  private final KeycloakClient keycloakClient;
  private final TokenService tokenService;
  private final FolioExecutionContext folioExecutionContext;
  private final KeycloakLoginClientProperties loginClientProperties;

  public void createUser(User user, String password) {
    if (user.getId() == null) {
      throw new RequestValidationException("User id is missing", "id", user.getId());
    }
    var kcUser = toKeycloakUser(user, password);
    kcUser.setUserTenantAttr(of(getRealm()));
    log.info("Creating keycloak user: userId = {}", user.getId());

    findUserByUsername(user.getUsername(), false)
      .flatMap(this::getUserIdFromAttributes).ifPresentOrElse(
        userId -> updateUser(fromString(userId), user),
        () -> callKeycloak(create(kcUser),
          () -> buildUsersErrorMessage("Failed to create keycloak user", user.getId())));
  }

  public void createUserForMigration(User user, String password, List<String> userTenants) {
    var kcUser = toKeycloakUser(user, password);
    kcUser.setUserTenantAttr(userTenants);
    log.info("Creating keycloak user for users migration: userId = {}", user.getId());

    callKeycloak(create(kcUser),
      () -> buildUsersErrorMessage("Failed to create keycloak user", user.getId()));
  }

  public void updateUser(UUID id, User user) {
    var kcUser = toKeycloakUser(user);
    log.info("Updating keycloak user: userId = {}", user.getId());

    callKeycloak(update(id, kcUser),
      () -> buildUsersErrorMessage("Failed to update keycloak user", user.getId()));
  }

  public void deleteUser(UUID id) {
    log.info("Deleting keycloak user with id: {}", id);

    callKeycloak(delete(id),
      () -> buildUsersErrorMessage("Failed to delete keycloak user", id));
  }

  public KeycloakUser findKeycloakUserWithUserIdAttr(UUID id) {
    var query = USER_ID_ATTR + ":" + id;
    var found = keycloakClient.getUsersWithAttrs(getRealm(), query, true, getToken());

    if (isEmpty(found)) {
      return null;
    }

    if (found.size() != 1) {
      throw new KeycloakException(
        String.format("Too many keycloak users with '%s' attribute: %s", USER_ID_ATTR, id));
    }

    return found.get(0);
  }

  /**
   * Retrieves a single user by username.
   *
   * @param username - username as {@link String}
   * @return {@link Optional} of {@link KeycloakUser} if user is found, {@link Optional#empty()} if not.
   * @throws KeycloakException if search request failed.
   */
  public Optional<KeycloakUser> findUserByUsername(String username) {
    return findUserByUsername(username, true);
  }

  public Optional<KeycloakUser> findUserByUsername(String username, boolean briefRepresentation) {
    var realm = folioExecutionContext.getTenantId();
    var foundUsers = callKeycloak(
      () -> keycloakClient.findUsersByUsername(realm, username, briefRepresentation, getToken()),
      () -> "Failed to find a user by username: " + username);
    return isNotEmpty(foundUsers) ? Optional.of(foundUsers.get(0)) : Optional.empty();
  }

  /**
   * Checks if keycloak user has a role.
   *
   * @param keycloakUserId - keycloak user identifier
   * @param role - role to be present for user
   * @return true if role is present for keycloak user by id, false otherwise.
   */
  public boolean hasRole(String keycloakUserId, String role) {
    var systemUserRoles = callKeycloak(
      () -> keycloakClient.findUserRoles(getRealm(), keycloakUserId, getToken()),
      () -> String.format("Failed to find user roles for user [role: %s, userId: %s]", role, keycloakUserId));

    return systemUserRoles.stream().anyMatch(userRole -> Objects.equals(userRole.getName(), role));
  }

  /**
   * Assigns a role to a user by id.
   *
   * @param keycloakUserId - keycloak user identifier
   * @param roleName - role name as identifier
   * @throws KeycloakException if keycloak requests failed.
   */
  public void assignRole(String keycloakUserId, String roleName) {
    log.info("Assigning system role to a system user [keycloakUserId: {}]", keycloakUserId);

    var role = callKeycloak(
      () -> keycloakClient.findByName(getRealm(), roleName, getToken()),
      () -> "Failed to find a role by name: " + roleName);

    callKeycloak(
      () -> keycloakClient.assignRolesToUser(getRealm(), keycloakUserId, singletonList(role), getToken()),
      () -> String.format("Failed to assign a role to user [userId: %s, role: %s]", keycloakUserId, roleName));
  }

  /**
   * Creates a scope based permission for the given policy.
   *
   * @param policyName - keycloak policy identifier
   * @param resource   - resource name as identifier
   * @param scopes     - resource scopes
   * @throws KeycloakException if keycloak requests failed.
   */
  public void createScopePermission(String policyName, String resource, List<String> scopes) {
    log.info("Creating permission to resource '{} {}' for policy '{}'", scopes, resource, policyName);

    var realm = getRealm();
    var clientId = realm + loginClientProperties.getClientNameSuffix();
    var client = findClientWithClientId(realm, clientId);
    var permissionName = toPermissionName(scopes, policyName, resource);
    var permission = ScopePermission.builder()
      .name(permissionName)
      .resources(of(resource))
      .policies(of(policyName))
      .scopes(scopes)
      .build();

    createPermissionIgnoringConflict(realm, client.getId(), permission);
  }

  /**
   * Deletes scope permission from login client.
   *
   * @param permissionName permission name
   */
  public void deleteScopePermission(String permissionName) {
    log.info("Deleting permission {}", permissionName);
    var realm = getRealm();
    var clientId = realm + loginClientProperties.getClientNameSuffix();
    var client = findClientWithClientId(realm, clientId);
    var clientKcId = client.getId();

    findPermission(clientKcId, permissionName).ifPresentOrElse(
      permission -> keycloakClient.deleteScopePermission(realm, clientKcId, permission.getId(), getToken()),
      () -> log.warn("Permission is not found: " + permissionName));
  }

  /**
   * Finds a client by the given clientId.
   *
   * @param realm    - name of the realm
   * @param clientId - client name as clientId
   * @return Found client
   */
  public Client findClientWithClientId(String realm, String clientId) {
    try {
      var found = keycloakClient.findClientsByClientId(realm, clientId, getToken());

      if (isEmpty(found)) {
        throw new KeycloakException(format("Keycloak client is not found by clientId: %s", clientId));
      }

      if (found.size() != 1) {
        throw new KeycloakException(format("Too many keycloak clients with clientId: %s", clientId));
      }

      var client = found.get(0);
      if (client == null) {
        throw new KeycloakException(format("Keycloak client is not found by clientId: %s", clientId));
      }
      return client;
    } catch (KeycloakException e) {
      throw e;
    } catch (Exception e) {
      throw new KeycloakException(
        String.format("Failed to find a keycloak client with clientId: %s", clientId), e);
    }
  }

  /**
   * Retrieves a single scope permission by name.
   *
   * @return {@link Optional} of {@link ScopePermission} if permission is found, {@link Optional#empty()} if not.
   * @throws KeycloakException if search request failed.
   */
  private Optional<ScopePermission> findPermission(UUID clientId, String permission) {
    var realm = folioExecutionContext.getTenantId();
    var foundUsers = callKeycloak(
      () -> keycloakClient.findScopePermission(realm, clientId, permission, getToken()),
      () -> String.format("Failed to find a permission %s", permission));
    return isNotEmpty(foundUsers) ? Optional.of(foundUsers.get(0)) : Optional.empty();
  }

  private void callKeycloak(Runnable method, Supplier<String> expMsgSupplier) {
    try {
      method.run();
    } catch (Exception cause) {
      throw new KeycloakException(expMsgSupplier.get(), cause);
    }
  }

  private <T> T callKeycloak(Callable<T> callable, Supplier<String> errorMessageSupplier) {
    try {
      return callable.call();
    } catch (Exception e) {
      throw new KeycloakException(errorMessageSupplier.get(), e);
    }
  }

  private Runnable create(KeycloakUser kcUser) {
    return () -> {
      var res = keycloakClient.createUser(getRealm(), kcUser, getToken());

      if (res.getStatusCode().is2xxSuccessful() && res.getHeaders().getLocation() != null) {
        var path = res.getHeaders().getLocation().getPath();
        var id = StringUtils.substringAfterLast(path, "/");

        log.info("Keycloak user created with id: {}", id);
      }
    };
  }

  private Runnable update(UUID userId, KeycloakUser kcUser) {
    return () -> {
      var existing = findKeycloakUserWithUserIdAttr(userId);

      if (existing == null) {
        throw new KeycloakException(
          String.format("Keycloak user doesn't exist with the given '%s' attribute: %s", USER_ID_ATTR, userId));
      }

      kcUser.setId(existing.getId());
      kcUser.setCreatedTimestamp(existing.getCreatedTimestamp());
      kcUser.setEmailVerified(existing.getEmailVerified());

      keycloakClient.updateUser(getRealm(), kcUser.getId(), kcUser, getToken());
    };
  }

  private Runnable delete(UUID id) {
    return () -> {
      var kcUser = findKeycloakUserWithUserIdAttr(id);

      if (kcUser != null) {
        keycloakClient.deleteUser(getRealm(), kcUser.getId(), getToken());
      } else {
        log.debug("Keycloak user is not found: userId = {}", id);
      }
    };
  }

  private String getToken() {
    return tokenService.issueToken();
  }

  private String getRealm() {
    return folioExecutionContext.getTenantId();
  }

  private KeycloakUser toKeycloakUser(User user) {
    var result = new KeycloakUser();

    result.setEnabled(user.getActive());
    result.setUserName(user.getUsername());

    var personal = user.getPersonal();
    if (nonNull(personal)) {
      result.setEmail(personal.getEmail());
      result.setFirstName(personal.getFirstName());
      result.setLastName(personal.getLastName());
    }

    result.setUserIdAttr(user.getId());

    return result;
  }

  private KeycloakUser toKeycloakUser(User user, String password) {
    var keycloakUser = toKeycloakUser(user);
    if (StringUtils.isNotBlank(password)) {
      keycloakUser.setCredentials(List.of(new Credential("password", password, false)));
    }
    return keycloakUser;
  }

  private String buildUsersErrorMessage(String message, UUID userId) {
    return format("%s: userId = %s, realm = %s", message, userId, getRealm());
  }

  private void createPermissionIgnoringConflict(String realm, UUID clientId, ScopePermission permission) {
    try {
      var res = keycloakClient.createScopePermission(realm, clientId, permission, getToken());
      log.info("Keycloak permission created with id: {}", res.getId());
    } catch (FeignException.Conflict e) {
      log.info("Permission already exists [message: {}]", e.getMessage());
    } catch (Exception e) {
      throw new KeycloakException(
        String.format("Failed to create a permission [resource: %s, policies: %s]", permission.getResources(),
          permission.getPolicies()), e);
    }
  }

  private Optional<String> getUserIdFromAttributes(KeycloakUser user) {
    return Optional.ofNullable(user.getAttributes())
      .map(userAttributes -> userAttributes.get(USER_ID_ATTR))
      .filter(CollectionUtils::isNotEmpty)
      .map(userAttributes -> userAttributes.get(0));
  }
}
