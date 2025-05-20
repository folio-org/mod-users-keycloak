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

  private final DefaultRolesClient defaultRolesClient;
  private final UserRolesClient userRolesClient;
  @Qualifier("systemUserRoleRetryTemplate") private final RetryTemplate retryTemplate;

  public void createAndAssignDefaultRole(User user, List<String> permissions) {
    retryTemplate.execute(ctx -> {
      log.info("Creating default role for user {}", user.getId());
      LoadableRole defaultRole = new LoadableRole();
      defaultRole.setName("default-system-role-" + user.getUsername());
      defaultRole.setDescription("Default system role for system user " + user.getUsername());
      defaultRole.setPermissions(permissions.stream()
        .map(LoadablePermission::of)
        .toList());
      LoadableRole createdRole = defaultRolesClient.createDefaultRole(defaultRole);
      log.info("Created default role {}", createdRole.getId());

      log.info("Assigning default role {} to user {}", createdRole.getId(), user.getId());
      UserRoles userRoleRequest = new UserRoles();
      userRoleRequest.setUserId(user.getId());
      userRoleRequest.setRoleIds(List.of(createdRole.getId()));
      userRolesClient.assignRoleToUser(userRoleRequest);
      log.info("Assigned default role {} to user {}", createdRole.getId(), user.getId());

      return null;
    });
  }
}
