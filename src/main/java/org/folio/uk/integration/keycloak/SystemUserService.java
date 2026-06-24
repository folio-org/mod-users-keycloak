package org.folio.uk.integration.keycloak;

import static org.apache.commons.collections4.CollectionUtils.isEmpty;

import java.util.ArrayList;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.folio.spring.FolioExecutionContext;
import org.folio.uk.configuration.SystemUserConfigurationProperties;
import org.folio.uk.domain.dto.Personal;
import org.folio.uk.domain.dto.User;
import org.folio.uk.integration.kafka.model.SystemUser;
import org.folio.uk.integration.keycloak.model.KeycloakUser;
import org.folio.uk.integration.roles.dafaultrole.DefaultSystemUserRoleService;
import org.folio.uk.service.UserService;
import org.folio.util.StringUtil;
import org.springframework.stereotype.Service;

@Log4j2
@Service
@RequiredArgsConstructor
public class SystemUserService {

  private final UserService userService;
  private final KeycloakService keycloakService;
  private final FolioExecutionContext executionContext;
  private final SystemUserConfigurationProperties systemUserConfiguration;
  private final DefaultSystemUserRoleService defaultSystemUserRoleService;
  private final SystemUserPasswordService systemUserPasswordService;

  /**
   * Creates a system user for tenant.
   */
  public void create() {
    var username = generateValueByTemplate(systemUserConfiguration.getUsernameTemplate());
    var systemUserEmail = generateValueByTemplate(systemUserConfiguration.getEmailTemplate());
    var createdSystemUser = createUser(username, "System User", systemUserEmail, "system");
    checkAndUpdateSystemUserRole(createdSystemUser.getUsername());
  }

  /**
   * Creates a system user for tenant.
   *
   * @param event system user event
   */
  public void createOnEvent(SystemUser event) {
    var username = event.getName();
    var firstName = "System user - " + username;
    var user = createUser(username, firstName, null, event.getType());
    var permissions = event.getPermissions();
    if (isEmpty(permissions)) {
      return;
    }
    recreateAndAssignRole(user, permissions);
  }

  public void updateOnEvent(SystemUser event) {
    if (isEmpty(event.getPermissions())) {
      return;
    }
    var username = event.getName();
    findUserByUsername(username).ifPresentOrElse(
      user -> {
        systemUserPasswordService.migrateLegacyPasswordIfNeeded(executionContext.getTenantId(), username);
        recreateAndAssignRole(user, event.getPermissions());
      },
      () -> createOnEvent(event));
  }

  public void deleteOnEvent(SystemUser event) {
    var username = event.getName();
    findUserByUsername(username).ifPresent(user -> userService.deleteUserById(user.getId()));
  }

  public void delete() {
    var username = generateValueByTemplate(systemUserConfiguration.getUsernameTemplate());
    var query = "username==" + StringUtil.cqlEncode(username);
    var users = userService.findUsers(query, 1).getUsers();
    if (CollectionUtils.isNotEmpty(users)) {
      var user = users.get(0);
      userService.deleteUser(user.getId());
    }
  }

  private Optional<User> findUserByUsername(String username) {
    var query = "username==" + StringUtil.cqlEncode(username);
    var users = userService.findUsers(query, 1).getUsers();
    if (CollectionUtils.isNotEmpty(users)) {
      return Optional.of(users.getFirst());
    }
    log.debug("User not found by username: {}", username);
    return Optional.empty();
  }

  private void recreateAndAssignRole(User user, Set<String> permissions) {
    defaultSystemUserRoleService.createAndAssignDefaultRole(user, new ArrayList<>(permissions));
  }

  private User createUser(String username, String firstName, String email, String type) {
    var tenantId = executionContext.getTenantId();
    var user = new User()
      .id(UUID.randomUUID())
      .active(true)
      .username(username)
      .type(type)
      .personal(new Personal()
        .email(email)
        .firstName(firstName)
        .lastName("System"));

    var systemUserPassword = systemUserPasswordService.getOrCreatePassword(tenantId, username);

    return userService.createUserSafe(user, systemUserPassword, false);
  }

  private void checkAndUpdateSystemUserRole(String username) {
    var foundSystemUser = findKeycloakUserByUsername(username);
    var keycloakUserId = foundSystemUser.getId();
    var systemRoleName = systemUserConfiguration.getSystemUserRole();
    if (keycloakService.hasRole(keycloakUserId, systemRoleName)) {
      log.info("System role is already assigned to user [username: {}, role: {}]", userService, systemRoleName);
      return;
    }

    keycloakService.assignRole(keycloakUserId, systemRoleName);
  }

  private KeycloakUser findKeycloakUserByUsername(String username) {
    return keycloakService.findUserByUsername(username)
      .orElseThrow(() -> new KeycloakException("Failed to find a system user by username: " + username));
  }

  private String generateValueByTemplate(String template) {
    return template.replace("{tenantId}", executionContext.getTenantId());
  }
}
