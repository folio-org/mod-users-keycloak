package org.folio.uk.integration.roles.dafaultrole;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.folio.test.types.UnitTest;
import org.folio.uk.domain.dto.User;
import org.folio.uk.integration.roles.UserRolesClient;
import org.folio.uk.integration.roles.model.CollectionResponse;
import org.folio.uk.integration.roles.model.LoadablePermission;
import org.folio.uk.integration.roles.model.LoadableRole;
import org.folio.uk.integration.roles.model.UserRoles;
import org.folio.uk.integration.roles.model.UserRolesResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.retry.support.RetryTemplateBuilder;
import org.springframework.web.client.HttpClientErrorException;

@UnitTest
class DefaultSystemUserRoleServiceTest {

  private TestDefaultRolesClient defaultRolesClient;
  private TestUserRolesClient userRolesClient;
  private DefaultSystemUserRoleService defaultSystemUserRoleService;

  @BeforeEach
  void setUp() {
    defaultRolesClient = new TestDefaultRolesClient();
    userRolesClient = new TestUserRolesClient();
    var retryTemplate = new RetryTemplateBuilder().maxAttempts(1).build();
    defaultSystemUserRoleService = new DefaultSystemUserRoleService(defaultRolesClient, userRolesClient, retryTemplate);
  }

  @Test
  void createAndAssignDefaultRole_positive() {
    var username = "mod-test";
    var userId = randomUUID();
    var roleId = randomUUID();
    var createdRole = role(roleId, username);
    var permissions = List.of("permission.one", "permission.two");
    var user = new User().id(userId).username(username);

    defaultRolesClient.createdRole = createdRole;

    defaultSystemUserRoleService.createAndAssignDefaultRole(user, permissions);

    assertThat(defaultRolesClient.calls).isEqualTo(1);
    assertThat(defaultRolesClient.capturedRole.getName()).isEqualTo("default-system-role-" + username);
    assertThat(defaultRolesClient.capturedRole.getDescription())
      .isEqualTo("Default system role for system user " + username);
    assertThat(defaultRolesClient.capturedRole.getPermissions()).hasSize(2);
    assertThat(defaultRolesClient.capturedRole.getPermissions())
      .extracting(LoadablePermission::getPermissionName)
      .contains("permission.one", "permission.two");

    assertThat(userRolesClient.assignCalls).isEqualTo(1);
    assertThat(userRolesClient.capturedUserRoles.getUserId()).isEqualTo(userId);
    assertThat(userRolesClient.capturedUserRoles.getRoleIds()).containsExactly(roleId);
  }

  @Test
  void createAndAssignDefaultRole_negative_roleCreationError() {
    var user = new User().id(randomUUID()).username("error-user");
    var permissions = List.of("permission.one");
    defaultRolesClient.exception = new RuntimeException("Role creation failed");

    assertThatThrownBy(() -> defaultSystemUserRoleService.createAndAssignDefaultRole(user, permissions))
      .isInstanceOf(RuntimeException.class)
      .hasMessage("Role creation failed");

    assertThat(defaultRolesClient.calls).isEqualTo(1);
    assertThat(userRolesClient.assignCalls).isZero();
  }

  @Test
  void createAndAssignDefaultRole_negative_roleAssigningToUserError() {
    var userId = randomUUID();
    var username = "assign-error";
    var roleId = randomUUID();
    var user = new User().id(userId).username(username);
    var permissions = List.of("permission.one");
    defaultRolesClient.createdRole = role(roleId, username);
    userRolesClient.exception = new IllegalStateException("Role assignment failed");

    assertThatThrownBy(() -> defaultSystemUserRoleService.createAndAssignDefaultRole(user, permissions))
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("Role assignment failed");

    assertThat(defaultRolesClient.calls).isEqualTo(1);
    assertThat(userRolesClient.assignCalls).isEqualTo(1);
  }

  @Test
  void createAndAssignDefaultRole_positive_userRoleAlreadyAssigned() {
    var userId = randomUUID();
    var username = "assigned-user";
    var roleId = randomUUID();
    var user = new User().id(userId).username(username);
    var permissions = List.of("permission.one");
    defaultRolesClient.createdRole = role(roleId, username);
    userRolesClient.exception = userRoleAlreadyExists(userId, roleId);

    defaultSystemUserRoleService.createAndAssignDefaultRole(user, permissions);

    assertThat(defaultRolesClient.calls).isEqualTo(1);
    assertThat(userRolesClient.assignCalls).isEqualTo(1);
    assertThat(userRolesClient.capturedUserRoles.getUserId()).isEqualTo(userId);
    assertThat(userRolesClient.capturedUserRoles.getRoleIds()).containsExactly(roleId);
  }

  private static LoadableRole role(UUID roleId, String username) {
    var createdRole = new LoadableRole();
    createdRole.setId(roleId);
    createdRole.setName("default-system-role-" + username);
    return createdRole;
  }

  private static HttpClientErrorException userRoleAlreadyExists(Object userId, Object roleId) {
    var body = """
      {"errors":[{"code":"found_error","message":"Relations between user and roles already exists \
      (userId: %s, roles: [%s])","parameters":[],"type":"EntityExistsException"}],"total_records":1}
      """.formatted(userId, roleId);

    return HttpClientErrorException.create(HttpStatus.BAD_REQUEST, "Bad Request", HttpHeaders.EMPTY,
      body.getBytes(UTF_8), UTF_8);
  }

  private static final class TestDefaultRolesClient implements DefaultRolesClient {

    private LoadableRole createdRole;
    private RuntimeException exception;
    private LoadableRole capturedRole;
    private int calls;

    @Override
    public LoadableRole createDefaultLoadableRole(LoadableRole role) {
      calls++;
      capturedRole = role;
      if (exception != null) {
        throw exception;
      }
      return createdRole;
    }
  }

  private static final class TestUserRolesClient implements UserRolesClient {

    private RuntimeException exception;
    private UserRoles capturedUserRoles;
    private int assignCalls;

    @Override
    public void deleteUserRoles(UUID id) {
      throw new UnsupportedOperationException();
    }

    @Override
    public Optional<CollectionResponse> findUserRoles(UUID id) {
      throw new UnsupportedOperationException();
    }

    @Override
    public UserRolesResponse assignRoleToUser(UserRoles userRoles) {
      assignCalls++;
      capturedUserRoles = userRoles;
      if (exception != null) {
        throw exception;
      }
      return null;
    }
  }
}
