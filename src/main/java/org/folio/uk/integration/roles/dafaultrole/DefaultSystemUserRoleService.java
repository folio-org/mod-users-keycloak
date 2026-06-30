package org.folio.uk.integration.roles.dafaultrole;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.uk.domain.dto.User;
import org.folio.uk.integration.roles.UserRolesClient;
import org.folio.uk.integration.roles.model.LoadablePermission;
import org.folio.uk.integration.roles.model.LoadableRole;
import org.folio.uk.integration.roles.model.UserRoles;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientResponseException;

@Service
@Log4j2
@RequiredArgsConstructor
public class DefaultSystemUserRoleService {

  private static final String DEFAULT_SYSTEM_USER_ROLE_NAME = "default-system-role-";
  private static final String ENTITY_EXISTS_EXCEPTION = "EntityExistsException";
  private static final String USER_ROLE_ALREADY_EXISTS = "Relations between user and roles already exists";

  private final DefaultRolesClient defaultRolesClient;
  private final UserRolesClient userRolesClient;
  @Qualifier("systemUserRoleRetryTemplate") private final RetryTemplate retryTemplate;

  public void createAndAssignDefaultRole(User user, List<String> permissions) {
    retryTemplate.execute(ctx -> {
      var defaultRole = buildDefaultRole(user, permissions);
      log.info("Creating default role for user: userId = {}, roleName = {}", user.getId(), defaultRole.getName());
      var createdRole = defaultRolesClient.createDefaultLoadableRole(defaultRole);
      log.debug("Created default role: role = {}", createdRole);

      assignRoleToUser(user, createdRole);

      return null;
    });
  }

  private void assignRoleToUser(User user, LoadableRole createdRole) {
    log.info("Assigning default role to user: roleName = {}, username = {}", createdRole.getName(),
      user.getUsername());
    var userRoleRequest = buildRoleUserRequest(user, createdRole);
    try {
      userRolesClient.assignRoleToUser(userRoleRequest);
    } catch (RestClientResponseException e) {
      if (!isUserRoleAlreadyAssigned(e)) {
        throw e;
      }
      log.info("Default role is already assigned to user: roleName = {}, username = {}",
        createdRole.getName(), user.getUsername());
      return;
    }

    log.debug("Assigned default role to user: roleName = {}, username = {}", createdRole.getName(),
      user.getUsername());
  }

  private static UserRoles buildRoleUserRequest(User user, LoadableRole createdRole) {
    var userRoleRequest = new UserRoles();
    userRoleRequest.setUserId(user.getId());
    userRoleRequest.setRoleIds(List.of(createdRole.getId()));
    return userRoleRequest;
  }

  private static LoadableRole buildDefaultRole(User user, List<String> permissions) {
    var defaultRole = new LoadableRole();
    defaultRole.setName(DEFAULT_SYSTEM_USER_ROLE_NAME + user.getUsername());
    defaultRole.setDescription("Default system role for system user " + user.getUsername());
    defaultRole.setPermissions(permissions.stream()
      .map(LoadablePermission::of)
      .toList());
    return defaultRole;
  }

  private static boolean isUserRoleAlreadyAssigned(RestClientResponseException exception) {
    if (exception.getStatusCode().value() != 400) {
      return false;
    }

    try {
      var errors = new JSONObject(exception.getResponseBodyAsString()).optJSONArray("errors");
      if (errors == null) {
        return false;
      }

      for (int i = 0; i < errors.length(); i++) {
        var error = errors.optJSONObject(i);
        if (error != null && isUserRoleAlreadyAssignedError(error)) {
          return true;
        }
      }
      return false;
    } catch (JSONException e) {
      return false;
    }
  }

  private static boolean isUserRoleAlreadyAssignedError(JSONObject error) {
    return ENTITY_EXISTS_EXCEPTION.equals(error.optString("type"))
      && error.optString("message").startsWith(USER_ROLE_ALREADY_EXISTS);
  }
}
