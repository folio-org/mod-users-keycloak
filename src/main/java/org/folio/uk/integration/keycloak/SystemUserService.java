package org.folio.uk.integration.keycloak;

import static org.apache.commons.collections4.CollectionUtils.isEmpty;
import static org.folio.common.configuration.properties.FolioEnvironment.getFolioEnvName;
import static org.folio.tools.store.utils.SecretGenerator.generateSecret;

import java.util.ArrayList;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.folio.spring.FolioExecutionContext;
import org.folio.tools.store.SecureStore;
import org.folio.tools.store.properties.SecureStoreProperties;
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

  private final SecureStore secureStore;
  private final UserService userService;
  private final KeycloakService keycloakService;
  private final FolioExecutionContext executionContext;
  private final SystemUserConfigurationProperties systemUserConfiguration;
  private final DefaultSystemUserRoleService defaultSystemUserRoleService;
  private final SecureStoreProperties secureStoreProperties;

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
      user -> recreateAndAssignRole(user, event.getPermissions()),
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

    var systemUserPassword = getOrCreatePassword(tenantId, username);

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

  private static String getSystemUserStoreKey(String env, String tenant, String username) {
    return String.format("%s_%s_%s", env, tenant, username);
  }

  private String getOrCreatePassword(String tenant, String username) {
    var systemUserKey = getSystemUserStoreKey(secureStoreProperties.getEnvironment(), tenant, username);
    return secureStore.lookup(systemUserKey)
      .orElseGet(() -> lookupLegacyOrGenerate(systemUserKey, tenant, username));
  }

  private String lookupLegacyOrGenerate(String systemUserKey, String tenant, String username) {
    var legacyKey = getLegacySystemUserStoreKey(tenant, username);
    if (Objects.equals(systemUserKey, legacyKey)) {
      return generateAndSavePassword(systemUserKey);
    }
    return secureStore.lookup(legacyKey)
      .map(password -> {
        log.debug("Found legacy system user password, copying to new key [legacyKey: {}, key: {}]",
          legacyKey, systemUserKey);
        return savePassword(systemUserKey, password);
      })
      .orElseGet(() -> generateAndSavePassword(systemUserKey));
  }

  private static String getLegacySystemUserStoreKey(String tenant, String username) {
    return getSystemUserStoreKey(getFolioEnvName(), tenant, username);
  }

  private String generateAndSavePassword(String key) {
    log.debug("Generating system user password [key: {}]", key);
    var secret = generateSecret(systemUserConfiguration.getPasswordLength());
    return savePassword(key, secret);
  }

  private String savePassword(String key, String secret) {
    secureStore.set(key, secret);
    return secret;
  }
}
