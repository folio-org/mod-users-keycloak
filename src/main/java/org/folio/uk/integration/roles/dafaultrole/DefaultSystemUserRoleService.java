package org.folio.uk.integration.roles.dafaultrole;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.uk.domain.dto.User;
import org.folio.uk.integration.roles.UserRolesClient;
import org.folio.uk.integration.roles.model.LoadablePermission;
import org.folio.uk.integration.roles.model.LoadableRole;
import org.folio.uk.integration.roles.model.UserRoles;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Service;

@Service
@Log4j2
@RequiredArgsConstructor
public class DefaultSystemUserRoleService {

  private static final String DEFAULT_SYSTEM_USER_ROLE_NAME = "default-system-role-";

  private final DefaultRolesClient defaultRolesClient;
  private final UserRolesClient userRolesClient;
  @Qualifier("systemUserRoleRetryTemplate") private final RetryTemplate retryTemplate;

  public void createAndAssignDefaultRole(User user, List<String> permissions) {
    retryTemplate.execute(ctx -> {
      var defaultRole = buildDefaultRole(user, permissions);
      log.info("Creating default role for user: userId = {}, roleName = {}", user.getId(), defaultRole.getName());
      var createdRole = defaultRolesClient.createDefaultLoadableRole(defaultRole);
      log.debug("Created default role: role = {}", createdRole);

      log.info("Assigning default role to user: roleName = {}, username = {}", createdRole.getName(),
        user.getUsername());
      var userRoleRequest = buildRoleUserRequest(user, createdRole);
      userRolesClient.assignRoleToUser(userRoleRequest);
      log.debug("Assigned default role to user: roleName = {}, username = {}", createdRole.getName(),
        user.getUsername());

      return null;
    });
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
}
