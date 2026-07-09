package org.folio.uk.integration.roles.dafaultrole;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import org.folio.test.types.UnitTest;
import org.folio.uk.domain.dto.Error;
import org.folio.uk.domain.dto.ErrorResponse;
import org.folio.uk.domain.dto.User;
import org.folio.uk.integration.roles.UserRolesClient;
import org.folio.uk.integration.roles.model.LoadablePermission;
import org.folio.uk.integration.roles.model.LoadableRole;
import org.folio.uk.integration.roles.model.UserRoles;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.web.client.HttpClientErrorException;
import tools.jackson.databind.ObjectMapper;

@UnitTest
@ExtendWith(MockitoExtension.class)
class DefaultSystemUserRoleServiceTest {

  @InjectMocks private DefaultSystemUserRoleService defaultSystemUserRoleService;

  @Mock private DefaultRolesClient defaultRolesClient;
  @Mock private UserRolesClient userRolesClient;
  @Mock private RetryTemplate retryTemplate;
  @Mock private ObjectMapper objectMapper;

  @BeforeEach
  void setUp() {
    when(retryTemplate.execute(any()))
      .thenAnswer(invocation ->
        invocation.getArgument(0, RetryCallback.class).doWithRetry(null)
      );
  }

  @Test
  void createAndAssignDefaultRole_positive() {
    var username = "mod-test";
    var userId = randomUUID();
    var roleId = randomUUID();
    var createdRole = new LoadableRole();
    createdRole.setId(roleId);
    createdRole.setName("default-system-role-" + username);
    var permissions = List.of("permission.one", "permission.two");

    var user = new User().id(userId).username(username);

    when(defaultRolesClient.createDefaultLoadableRole(any())).thenReturn(createdRole);

    defaultSystemUserRoleService.createAndAssignDefaultRole(user, permissions);

    verify(retryTemplate).execute(any());

    var roleCaptor = forClass(LoadableRole.class);
    verify(defaultRolesClient).createDefaultLoadableRole(roleCaptor.capture());

    var capturedRole = roleCaptor.getValue();
    assertThat(capturedRole.getName()).isEqualTo("default-system-role-" + username);
    assertThat(capturedRole.getDescription()).isEqualTo("Default system role for system user " + username);
    assertThat(capturedRole.getPermissions()).hasSize(2);
    assertThat(capturedRole.getPermissions())
      .extracting(LoadablePermission::getPermissionName)
      .contains("permission.one", "permission.two");

    var userRolesCaptor = forClass(UserRoles.class);
    verify(userRolesClient).assignRoleToUser(userRolesCaptor.capture());

    var capturedUserRoles = userRolesCaptor.getValue();
    assertThat(capturedUserRoles.getUserId()).isEqualTo(userId);
    assertThat(capturedUserRoles.getRoleIds()).containsExactly(roleId);
  }

  @Test
  void createAndAssignDefaultRole_negative_roleCreationError() {
    final var userId = randomUUID();
    var username = "error-user";
    var user = new User().id(userId).username(username);
    var permissions = List.of("permission.one");

    when(defaultRolesClient.createDefaultLoadableRole(any()))
      .thenThrow(new RuntimeException("Role creation failed"));

    assertThatThrownBy(() -> defaultSystemUserRoleService.createAndAssignDefaultRole(user, permissions))
      .isInstanceOf(RuntimeException.class)
      .hasMessage("Role creation failed");

    verify(retryTemplate).execute(any());
    verify(defaultRolesClient).createDefaultLoadableRole(any());
    verifyNoInteractions(userRolesClient);
  }

  @Test
  void createAndAssignDefaultRole_negative_roleAssigningToUserError() {
    var userId = randomUUID();
    var username = "assign-error";
    var roleId = randomUUID();
    var createdRole = new LoadableRole();
    createdRole.setId(roleId);
    createdRole.setName("default-system-role-" + username);
    var user = new User().id(userId).username(username);
    var permissions = List.of("permission.one");

    when(defaultRolesClient.createDefaultLoadableRole(any())).thenReturn(createdRole);
    when(userRolesClient.assignRoleToUser(any()))
      .thenThrow(new IllegalStateException("Role assignment failed"));

    assertThatThrownBy(() -> defaultSystemUserRoleService.createAndAssignDefaultRole(user, permissions))
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("Role assignment failed");

    verify(retryTemplate).execute(any());
    verify(defaultRolesClient).createDefaultLoadableRole(any());
    verify(userRolesClient).assignRoleToUser(any());
  }

  @Test
  void createAndAssignDefaultRole_positive_userRoleAlreadyAssigned() throws Exception {
    final var userId = randomUUID();
    var username = "assigned-user";
    var roleId = randomUUID();
    var createdRole = new LoadableRole();
    createdRole.setId(roleId);
    createdRole.setName("default-system-role-" + username);

    when(defaultRolesClient.createDefaultLoadableRole(any())).thenReturn(createdRole);
    when(userRolesClient.assignRoleToUser(any())).thenThrow(badRequestError());
    when(objectMapper.readValue(any(String.class), eq(ErrorResponse.class)))
      .thenReturn(errorResponse("EntityExistsException"));

    var user = new User().id(userId).username(username);
    var permissions = List.of("permission.one");

    defaultSystemUserRoleService.createAndAssignDefaultRole(user, permissions);

    verify(retryTemplate).execute(any());
    verify(defaultRolesClient).createDefaultLoadableRole(any());
    verify(userRolesClient).assignRoleToUser(any());
    verify(objectMapper).readValue(any(String.class), eq(ErrorResponse.class));
  }

  @Test
  void createAndAssignDefaultRole_negative_roleAssigningToUserBadRequestError() throws Exception {
    var userId = randomUUID();
    var username = "bad-request-user";
    var roleId = randomUUID();
    var createdRole = new LoadableRole();
    createdRole.setId(roleId);
    createdRole.setName("default-system-role-" + username);
    var user = new User().id(userId).username(username);
    var permissions = List.of("permission.one");

    when(defaultRolesClient.createDefaultLoadableRole(any())).thenReturn(createdRole);
    when(userRolesClient.assignRoleToUser(any())).thenThrow(badRequestError());
    when(objectMapper.readValue(any(String.class), eq(ErrorResponse.class)))
      .thenReturn(errorResponse("ValidationException"));

    assertThatThrownBy(() -> defaultSystemUserRoleService.createAndAssignDefaultRole(user, permissions))
      .isInstanceOf(HttpClientErrorException.BadRequest.class);

    verify(retryTemplate).execute(any());
    verify(defaultRolesClient).createDefaultLoadableRole(any());
    verify(userRolesClient).assignRoleToUser(any());
    verify(objectMapper).readValue(any(String.class), eq(ErrorResponse.class));
  }

  private static HttpClientErrorException badRequestError() {
    var body = """
      {"errors":[{"code":"found_error","message":"error","parameters":[],"type":"EntityExistsException"}]}
      """;

    return HttpClientErrorException.create(HttpStatus.BAD_REQUEST, "Bad Request", HttpHeaders.EMPTY,
      body.getBytes(UTF_8), UTF_8);
  }

  private static ErrorResponse errorResponse(String type) {
    return new ErrorResponse().errors(List.of(new Error().type(type)));
  }
}
