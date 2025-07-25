package org.folio.uk.service;

import static java.lang.String.format;
import static java.util.UUID.fromString;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.collections4.CollectionUtils.isEmpty;
import static org.apache.commons.collections4.ListUtils.emptyIfNull;
import static org.folio.common.utils.CollectionUtils.toStream;
import static org.folio.spring.utils.FolioExecutionContextUtils.prepareContextForTenant;
import static org.folio.uk.utils.UserUtils.getOriginalTenantIdOptional;

import feign.FeignException;
import jakarta.persistence.EntityNotFoundException;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.FolioModuleMetadata;
import org.folio.spring.scope.FolioExecutionContextSetter;
import org.folio.uk.domain.dto.CompositeUser;
import org.folio.uk.domain.dto.IncludedField;
import org.folio.uk.domain.dto.PermissionUser;
import org.folio.uk.domain.dto.ServicePointUser;
import org.folio.uk.domain.dto.User;
import org.folio.uk.domain.dto.Users;
import org.folio.uk.domain.model.UserType;
import org.folio.uk.integration.inventory.ServicePointsClient;
import org.folio.uk.integration.inventory.ServicePointsUserClient;
import org.folio.uk.integration.keycloak.KeycloakException;
import org.folio.uk.integration.keycloak.KeycloakService;
import org.folio.uk.integration.keycloak.config.KeycloakFederatedAuthProperties;
import org.folio.uk.integration.keycloak.model.KeycloakUser;
import org.folio.uk.integration.roles.RolesKeycloakConfigurationProperties;
import org.folio.uk.integration.roles.UserPermissionsClient;
import org.folio.uk.integration.users.UsersClient;
import org.folio.util.StringUtil;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Log4j2
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

  public static final String PERMISSION_NAME_FIELD = "permissionName";

  private final UsersClient usersClient;
  private final ServicePointsUserClient servicePointsUserClient;
  private final ServicePointsClient servicePointsClient;
  private final KeycloakService keycloakService;
  private final UserPermissionsClient userPermissionsClient;
  private final FolioModuleMetadata folioModuleMetadata;
  private final FolioExecutionContext folioExecutionContext;
  private final RolesKeycloakConfigurationProperties rolesKeycloakConfiguration;
  private final KeycloakFederatedAuthProperties keycloakFederatedAuthProperties;
  private final CapabilitiesService capabilitiesService;

  public User createUser(User user, boolean keycloakOnly) {
    return createUser(user, null, keycloakOnly);
  }

  public User createUser(User user, String password, boolean keycloakOnly) {
    return createUserPrivate(user, keycloakOnly, this::createUserInUserServiceSafe,
      createdUser -> {
        var kcUserId = keycloakService.upsertUser(createdUser, password);
        if (StringUtils.isNotEmpty(kcUserId) && Boolean.TRUE.equals(keycloakFederatedAuthProperties.isEnabled())) {
          keycloakService.linkIdentityProviderToUser(user, kcUserId);
        }
      });
  }

  @Retryable(
    maxAttemptsExpression = "#{@systemUserConfigurationProperties.retryAttempts}",
    backoff = @Backoff(delayExpression = "#{@systemUserConfigurationProperties.retryDelay}"),
    retryFor = {FeignException.class, KeycloakException.class},
    listeners = "methodLoggingRetryListener")
  public User createUserSafe(User user, String password, boolean keycloakOnly) {
    if (!keycloakOnly) {
      findUserIdKcAttribute(user).ifPresent(user::setId);
    }

    return createUserPrivate(user, keycloakOnly, this::createUserInUserServiceSafe,
      createdUser -> createUserInKeycloakSafe(createdUser, password));
  }

  public Users findUsers(String query, int limit) {
    return usersClient.query(query, limit);
  }

  public Optional<User> getUser(UUID id) {
    log.info("Retrieving user with: id = {}", id);

    return usersClient.lookupUserById(id);
  }

  public CompositeUser getUserBySelfReference(List<IncludedField> include, boolean expandPermissions,
                                              boolean overrideUser) {
    log.info("Retrieving user by self reference with parameters: include = {}, expandPermissions = {}",
      () -> StringUtils.join(emptyIfNull(include), ", "), () -> expandPermissions);

    var userId = getUserId();
    var user = usersClient.lookupUserById(userId)
      .orElseThrow(() -> new EntityNotFoundException("User was Not Found with: id = " + userId));

    // When overrideUser is set to true the shadow user will be used to retrieve the real user
    // with its permissions and service points to support the ECS login into member tenants
    if (Boolean.TRUE.equals(overrideUser) && StringUtils.equals(user.getType(), UserType.SHADOW.getValue())) {
      var originalTenantIdOptional = getOriginalTenantIdOptional(user);
      if (originalTenantIdOptional.isPresent()) {
        return getRealUserByReference(user, originalTenantIdOptional.get(), expandPermissions);
      }
    }

    return new CompositeUser().user(user)
      .permissions(fetchPermissionUser(userId, expandPermissions))
      .servicePointsUser(fetchServicePointUser(userId));
  }

  private CompositeUser getRealUserByReference(User shadowUser, String originalTenantId, boolean expandPermissions) {
    log.info("Overriding self reference to use a real shadowUser with: tenant = {}", originalTenantId);
    var userId = shadowUser.getId();

    try (var ignored = new FolioExecutionContextSetter(
      prepareContextForTenant(originalTenantId, folioModuleMetadata, folioExecutionContext))) {
      var realUser = usersClient.lookupUserById(userId)
        .filter(foundUser -> Objects.nonNull(foundUser.getId()))
        .orElseThrow(() -> new EntityNotFoundException("User was Not Found with: userId = " + userId));

      return new CompositeUser().user(realUser)
        .originalTenantId(originalTenantId)
        .permissions(fetchPermissionUser(realUser.getId(), expandPermissions))
        .servicePointsUser(fetchServicePointUser(realUser.getId()));
    }
  }

  public void updateUser(UUID id, User user) {
    log.info("Updating user: id = {}", id);

    usersClient.updateUser(id, user);
    var kcUser = keycloakService.findKeycloakUserWithUserIdAttr(id);
    if (kcUser.isPresent()) {
      keycloakService.updateUser(id, user);
    } else {
      log.info("User was not found in keycloak by user_id attribute: id = {}", id);
      keycloakService.upsertUser(user, null);
    }
  }

  public void deleteUser(UUID id) {
    log.info("Deleting user with: id = {}", id);

    usersClient.lookupUserById(id)
      .ifPresentOrElse(user -> removeUserWithLinkedResources(id),
        () -> log.debug("Can not delete user cause user does not exist: userId = {}", id));

    keycloakService.deleteUser(id);
  }

  public void deleteUserById(UUID id) {
    log.info("Deleting user with: id = {}", id);
    removeUserWithLinkedResources(id);
    keycloakService.deleteUser(id);
  }

  /**
   * Resolves user permissions.
   *
   * @param userId - user id
   * @param userPermissions - list of permissions should be resolved
   * @return list of resolved permissions
   */
  public List<String> resolvePermissions(UUID userId, List<String> userPermissions) {
    var permissions = userPermissionsClient.getPermissionsForUser(userId, false, userPermissions);
    return permissions.getPermissions();
  }

  private void removeUserWithLinkedResources(UUID id) {
    usersClient.deleteUser(id);
    capabilitiesService.unassignAll(id);
  }

  private Optional<UUID> findUserIdKcAttribute(User user) {
    return keycloakService.findUserByUsername(user.getUsername(), false)
      .flatMap(KeycloakUser::getUserIdAttr)
      .map(UUID::fromString);
  }

  private UUID getUserId() {
    var userIdFromHeader = folioExecutionContext.getUserId();
    var userIdFromToken = extractUserId(folioExecutionContext.getToken());
    if (userIdFromHeader == null && userIdFromToken == null) {
      throw new EntityNotFoundException("User id was not found in header or token");
    }

    return userIdFromHeader != null ? userIdFromHeader : userIdFromToken;
  }

  private ServicePointUser fetchServicePointUser(UUID userId) {
    try {
      return fetchServicePointUserInternal(userId);
    } catch (FeignException e) {
      log.warn("Failed to fetch service point user: userId = {}, message = {}", userId, e.getMessage());
      return null;
    }
  }

  private ServicePointUser fetchServicePointUserInternal(UUID userId) {
    var servicePointUsers = servicePointsUserClient.getServicePointsUser(userId);
    if (servicePointUsers.getTotalRecords() != 1) {
      return null;
    }

    var servicePoints = toStream(servicePointUsers.getServicePointsUsers())
      .flatMap(servicePointUser -> toStream(servicePointUser.getServicePointsIds()))
      .map(spId -> servicePointsClient.getServicePoint(fromString(spId)))
      .filter(Optional::isPresent)
      .map(Optional::get)
      .collect(toList());

    var servicePointUser = servicePointUsers.getServicePointsUsers().get(0);
    servicePointUser.setServicePoints(servicePoints);
    return servicePointUser;
  }

  private PermissionUser fetchPermissionUser(UUID userId, boolean expandPermissions) {
    var includeOnlyVisiblePermissions = rolesKeycloakConfiguration.isIncludeOnlyVisiblePermissions();
    var userPermissions = userPermissionsClient.getPermissionsForUser(userId, includeOnlyVisiblePermissions, null);
    var permissionsList = emptyIfNull(userPermissions.getPermissions());

    return new PermissionUser()
      .permissions(
        permissionsList.stream()
          // For backward compatibility of the API we must return a JSON object in permissions in case
          // expandPermissions is set to true. For now, however, we only fill in the
          // permissionName field and nothing else.
          .map(permissionName -> expandPermissions ? Map.of(PERMISSION_NAME_FIELD, permissionName) : permissionName)
          .toList()).userId(userId.toString());
  }

  private UUID extractUserId(String token) {
    JSONObject payload = parseTokenPayload(token);
    if (payload == null) {
      return null;
    }
    try {
      return fromString(payload.getString("user_id"));
    } catch (JSONException e) {
      return null;
    }
  }

  private JSONObject parseTokenPayload(String token) {
    String[] tokenParts = token.split("\\.");
    if (tokenParts.length != 3) {
      return null;
    }
    try {
      String encodedPayload = tokenParts[1];
      byte[] decodedJsonBytes = Base64.getUrlDecoder().decode(encodedPayload);
      String decodedJson = new String(decodedJsonBytes);

      return new JSONObject(decodedJson);
    } catch (JSONException e) {
      return null;
    }
  }

  private User createUserPrivate(User user, boolean keycloakOnly,
                                 Function<User, User> modUsersMethodCall, Consumer<User> keycloakMethodCall) {
    log.info("Creating user: id = {}, username = {}", user.getId(), user.getUsername());
    var created = user;
    if (!keycloakOnly) {
      created = modUsersMethodCall.apply(created);
    }

    keycloakMethodCall.accept(created);
    return created;
  }

  private User createUserInUserServiceSafe(User user) {
    try {
      return usersClient.createUser(user);
    } catch (FeignException.UnprocessableEntity e) {
      var username = user.getUsername();
      log.warn("User already exists: username = {}, message = {}", username, e.getMessage());
      return findUserByUsername(username);
    }
  }

  private void createUserInKeycloakSafe(User user, String password) {
    try {
      keycloakService.upsertUser(user, password);
    } catch (KeycloakException exception) {
      if (exception.getCause() instanceof FeignException.Conflict) {
        log.warn("System user is already created: username = {}, service = keycloak", user.getUsername());
      } else {
        throw exception;
      }
    }
  }

  private User findUserByUsername(String username) {
    var cql = "username==" + StringUtil.cqlEncode(username);
    var userByUsername = usersClient.query(cql, 1).getUsers();
    if (isEmpty(userByUsername)) {
      throw new EntityNotFoundException(format("Failed to find user: service = mod-users, username = %s", username));
    }

    return userByUsername.get(0);
  }
}
