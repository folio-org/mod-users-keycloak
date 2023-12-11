package org.folio.uk.service;

import static java.lang.String.format;
import static java.util.UUID.fromString;
import static org.apache.commons.collections4.CollectionUtils.isEmpty;
import static org.apache.commons.collections4.ListUtils.emptyIfNull;
import static org.folio.common.utils.CollectionUtils.toStream;

import feign.FeignException;
import jakarta.persistence.EntityNotFoundException;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.folio.spring.FolioExecutionContext;
import org.folio.uk.domain.dto.CompositeUser;
import org.folio.uk.domain.dto.IncludedField;
import org.folio.uk.domain.dto.PermissionUser;
import org.folio.uk.domain.dto.ServicePointUser;
import org.folio.uk.domain.dto.User;
import org.folio.uk.domain.dto.Users;
import org.folio.uk.integration.inventory.ServicePointsClient;
import org.folio.uk.integration.inventory.ServicePointsUserClient;
import org.folio.uk.integration.keycloak.KeycloakException;
import org.folio.uk.integration.keycloak.KeycloakService;
import org.folio.uk.integration.keycloak.model.KeycloakUser;
import org.folio.uk.integration.roles.UserPermissionsClient;
import org.folio.uk.integration.users.UsersClient;
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

  private final UsersClient usersClient;
  private final ServicePointsUserClient servicePointsUserClient;
  private final ServicePointsClient servicePointsClient;
  private final KeycloakService keycloakService;
  private final UserPermissionsClient userPermissionsClient;
  private final FolioExecutionContext folioExecutionContext;

  public User createUser(User user, boolean keycloakOnly) {
    return createUser(user, null, keycloakOnly);
  }

  public User createUser(User user, String password, boolean keycloakOnly) {

    return createUserPrivate(user, keycloakOnly, this::createUserInUserServiceSafe,
      createdUser -> keycloakService.createUser(createdUser, password));
  }

  @Retryable(
    maxAttemptsExpression = "#{@systemUserConfigurationProperties.retryAttempts}",
    backoff = @Backoff(delayExpression = "#{@systemUserConfigurationProperties.retryDelay}"),
    retryFor = {FeignException.class, KeycloakException.class},
    listeners = "methodLoggingRetryListener")
  public User createUserSafe(User user, String password, boolean keycloakOnly) {
    if (!keycloakOnly) {
      findUserIdKcAttribute(user).ifPresent(userId -> user.setId(userId));
    }

    return createUserPrivate(user, keycloakOnly, this::createUserInUserServiceSafe,
      createdUser -> createUserInKeycloakSafe(createdUser, password));
  }

  public Users findUsers(String query, int limit) {
    return usersClient.query(query, limit);
  }

  public User getUser(UUID id) {
    log.info("Retrieving user with: id = {}", id);

    return usersClient.lookupUserById(id).orElseThrow(() -> new EntityNotFoundException("Not Found"));
  }

  public Users queryUsers(String query, int limit) {
    log.info("Querying users: query = {}", query);

    return usersClient.query(query, limit);
  }

  public CompositeUser getUserBySelfReference(List<IncludedField> include, boolean expandPermissions) {
    log.info("Retrieving user by self reference with parameters: include = {}, expandPermissions = {}",
      () -> StringUtils.join(emptyIfNull(include), ", "), () -> expandPermissions);

    var userId = getUserId();

    var user = usersClient.lookupUserById(userId)
      .orElseThrow(() -> new EntityNotFoundException("User was Not Found with: id = " + userId));

    return new CompositeUser()
      .user(user)
      .permissions(fetchPermissionUser(userId))
      .servicePointsUser(fetchServicePointUser(userId));
  }

  public void updateUser(UUID id, User user) {
    log.info("Updating user: id = {}", id);

    usersClient.updateUser(id, user);
    keycloakService.updateUser(id, user);
  }

  public void deleteUser(UUID id) {
    log.info("Deleting user with: id = {}", id);

    keycloakService.deleteUser(id);
    usersClient.lookupUserById(id)
      .ifPresentOrElse(user -> usersClient.deleteUser(id),
        () -> log.debug("Can not delete user cause user does not exist: userId = {}", id));
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
      .collect(Collectors.toList());

    var servicePointUser = servicePointUsers.getServicePointsUsers().get(0);
    servicePointUser.setServicePoints(servicePoints);
    return servicePointUser;
  }

  private PermissionUser fetchPermissionUser(UUID userId) {
    var perms = emptyIfNull(userPermissionsClient.getPermissionsForUser(userId, true, true)
      .getPermissions());
    return new PermissionUser()
      .permissions(perms)
      .userId(userId.toString());
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
      byte[] decodedJsonBytes = Base64.getDecoder().decode(encodedPayload);
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
      keycloakService.createUser(user, password);
    } catch (KeycloakException exception) {
      if (exception.getCause() instanceof FeignException.Conflict) {
        log.warn("System user is already created: username = {}, service = keycloak", user.getUsername());
      } else {
        throw exception;
      }
    }
  }

  private User findUserByUsername(String username) {
    var userByUsername = usersClient.query("username==" + username, 1).getUsers();
    if (isEmpty(userByUsername)) {
      throw new EntityNotFoundException(format("Failed to find user: service = mod-users, username = %s", username));
    }

    return userByUsername.get(0);
  }
}
