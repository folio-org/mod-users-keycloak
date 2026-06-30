package org.folio.uk.integration.roles.dafaultrole;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.uk.domain.dto.Error;
import org.folio.uk.domain.dto.ErrorResponse;
import org.folio.uk.domain.dto.User;
import org.folio.uk.integration.roles.UserRolesClient;
import org.folio.uk.integration.roles.model.LoadablePermission;
import org.folio.uk.integration.roles.model.LoadableRole;
import org.folio.uk.integration.roles.model.UserRoles;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientResponseException;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Service
@Log4j2
@RequiredArgsConstructor
public class DefaultSystemUserRoleService {

  private static final String DEFAULT_SYSTEM_USER_ROLE_NAME = "default-system-role-";
  private static final String ENTITY_EXISTS_EXCEPTION = "EntityExistsException";

  private final DefaultRolesClient defaultRolesClient;
  private final UserRolesClient userRolesClient;
  @Qualifier("systemUserRoleRetryTemplate") private final RetryTemplate retryTemplate;
  private final ObjectMapper objectMapper;

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
      if (isUserRoleAlreadyAssigned(e)) {
        log.info("Default role is already assigned to user: roleName = {}, username = {}",
          createdRole.getName(), user.getUsername());
        return;
      }

      throw e;
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

  private boolean isUserRoleAlreadyAssigned(RestClientResponseException exception) {
    if (!exception.getStatusCode().isSameCodeAs(BAD_REQUEST)) {
      return false;
    }

    try {
      var errors = objectMapper.readValue(exception.getResponseBodyAsString(), ErrorResponse.class).getErrors();
      if (errors == null) {
        return false;
      }

      return errors.stream().anyMatch(DefaultSystemUserRoleService::isUserRoleAlreadyAssignedError);
    } catch (JacksonException e) {
      return false;
    }
  }

  private static boolean isUserRoleAlreadyAssignedError(Error error) {
    return ENTITY_EXISTS_EXCEPTION.equals(error.getType());
  }
}
