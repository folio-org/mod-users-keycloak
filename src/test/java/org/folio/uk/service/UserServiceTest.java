package org.folio.uk.service;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.List.of;
import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.folio.uk.domain.dto.IncludedField.EXPANDED_PERMS;
import static org.folio.uk.service.UserService.PERMISSION_NAME_FIELD;
import static org.folio.uk.utils.UserUtils.ORIGINAL_TENANT_ID_CUSTOM_FIELD;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.NONE;

import feign.FeignException.Conflict;
import feign.FeignException.InternalServerError;
import feign.FeignException.ServiceUnavailable;
import feign.FeignException.UnprocessableEntity;
import feign.Request;
import feign.Request.HttpMethod;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.FolioModuleMetadata;
import org.folio.test.types.UnitTest;
import org.folio.uk.domain.dto.User;
import org.folio.uk.domain.dto.Users;
import org.folio.uk.integration.inventory.ServicePointsClient;
import org.folio.uk.integration.inventory.ServicePointsUserClient;
import org.folio.uk.integration.inventory.model.ServicePointUserCollection;
import org.folio.uk.integration.keycloak.KeycloakException;
import org.folio.uk.integration.keycloak.KeycloakService;
import org.folio.uk.integration.keycloak.config.KeycloakFederatedAuthProperties;
import org.folio.uk.integration.policy.PolicyService;
import org.folio.uk.integration.roles.RolesKeycloakConfigurationProperties;
import org.folio.uk.integration.roles.UserCapabilitiesClient;
import org.folio.uk.integration.roles.UserCapabilitySetClient;
import org.folio.uk.integration.roles.UserPermissionsClient;
import org.folio.uk.integration.roles.UserRolesClient;
import org.folio.uk.integration.roles.model.CollectionResponse;
import org.folio.uk.integration.roles.model.UserPermissions;
import org.folio.uk.integration.users.UsersClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@UnitTest
@SpringBootTest(classes = {UserService.class, RetryTestConfiguration.class}, webEnvironment = NONE)
class UserServiceTest {

  private static final String PASSWORD = "dGVzdC1wYXNzd29yZA==";
  private static final String USERNAME = "test-username";

  @Autowired private UserService userService;
  @MockitoBean private UsersClient usersClient;
  @MockitoBean private ServicePointsUserClient servicePointsUserClient;
  @MockitoBean private ServicePointsClient servicePointsClient;
  @MockitoBean private KeycloakService keycloakService;
  @MockitoBean private UserPermissionsClient userPermissionsClient;
  @MockitoBean private UserRolesClient userRolesClient;
  @MockitoBean private UserCapabilitySetClient userCapabilitySetClient;
  @MockitoBean private UserCapabilitiesClient userCapabilitiesClient;
  @MockitoBean private PolicyService policyService;
  @MockitoBean private RolesKeycloakConfigurationProperties rolesKeycloakConfiguration;
  @MockitoBean private FolioModuleMetadata folioModuleMetadata;
  @MockitoBean private FolioExecutionContext folioExecutionContext;
  @MockitoBean private KeycloakFederatedAuthProperties keycloakFederatedAuthProperties;
  @MockitoBean private CapabilitiesService capabilitiesService;

  @AfterEach
  void tearDown() {
    verifyNoMoreInteractions(usersClient, keycloakService, userPermissionsClient, servicePointsUserClient,
      servicePointsClient);
  }

  @Test
  void createUserSafe_positive() {
    var user = user();
    when(usersClient.createUser(user)).thenReturn(user);

    var result = userService.createUserSafe(user, PASSWORD, false);

    assertThat(result).isEqualTo(user);
    verify(usersClient).createUser(user);
    verify(keycloakService).findUserByUsername(USERNAME, false);
    verify(keycloakService).upsertUser(user, PASSWORD);
  }

  @Test
  void createUserSafe_positive_userAlreadyExistsInModUsers() {
    var user = user("te*st");
    var users = new Users().addUsersItem(user).totalRecords(1);

    when(usersClient.createUser(user)).thenThrow(UnprocessableEntity.class);
    when(usersClient.query("username==\"te\\*st\"", 1)).thenReturn(users);

    var result = userService.createUserSafe(user, PASSWORD, false);

    assertThat(result).isEqualTo(user);
    verify(usersClient).createUser(user);
    verify(usersClient).query("username==\"te\\*st\"", 1);
    verify(keycloakService).findUserByUsername("te*st", false);
    verify(keycloakService).upsertUser(user, PASSWORD);
  }

  @Test
  void createUserSafe_positive_userAlreadyExistsInKeycloak() {
    var user = user();
    var users = new Users().addUsersItem(user).totalRecords(1);

    var uri = "http://keycloak:8200/admin/realms/test/users";
    var request = Request.create(HttpMethod.POST, uri, emptyMap(), null, UTF_8, null);
    var cause = new Conflict("User exists", request, null, emptyMap());
    var keycloakException = new KeycloakException("Failed to create keycloak user", cause);

    when(usersClient.createUser(user)).thenReturn(user);
    doThrow(keycloakException).when(keycloakService).upsertUser(user, PASSWORD);
    when(usersClient.query("username==\"test-username\"", 1)).thenReturn(users);

    var result = userService.createUserSafe(user, PASSWORD, false);

    assertThat(result).isEqualTo(user);
    verify(usersClient).createUser(user);
    verify(keycloakService).findUserByUsername(USERNAME, false);
    verify(keycloakService).upsertUser(user, PASSWORD);
  }

  @Test
  void createUserSafe_negative_userAlreadyExistsInModUsersButNotFoundByUsername() {
    var user = user();
    var foundUsers = new Users().users(emptyList()).totalRecords(0);

    when(usersClient.createUser(user)).thenThrow(UnprocessableEntity.class);
    when(usersClient.query("username==\"test-username\"", 1)).thenReturn(foundUsers);

    assertThatThrownBy(() -> userService.createUserSafe(user, PASSWORD, false))
      .isInstanceOf(EntityNotFoundException.class)
      .hasMessage("Failed to find user: service = mod-users, username = test-username");

    verify(usersClient).createUser(user);
    verify(usersClient).query("username==\"test-username\"", 1);
    verify(keycloakService).findUserByUsername(USERNAME, false);
  }

  @Test
  void createUserSafe_positive_retryInvokedForModUsersCall() {
    var user = user();
    var request = Request.create(HttpMethod.POST, "http://users/users", emptyMap(), null, UTF_8, null);
    var error = new InternalServerError("Failed to perform request", request, null, emptyMap());
    when(usersClient.createUser(user)).thenThrow(error).thenReturn(user);

    var result = userService.createUserSafe(user, PASSWORD, false);

    assertThat(result).isEqualTo(user);
    verify(usersClient, times(2)).createUser(user);
    verify(keycloakService, times(2)).findUserByUsername(USERNAME, false);
    verify(keycloakService).upsertUser(user, PASSWORD);
  }

  @Test
  void createUserSafe_positive_retryInvokedForKeycloakCall() {
    var user = user();
    var uri = "http://keycloak:8200/admin/realms/test/users";
    var request = Request.create(HttpMethod.POST, uri, emptyMap(), null, UTF_8, null);
    var cause = new ServiceUnavailable("Service unavailable", request, null, emptyMap());
    var keycloakException = new KeycloakException("Failed to create keycloak user", cause);

    when(usersClient.createUser(user)).thenReturn(user);
    doThrow(keycloakException).doThrow(keycloakException).doReturn(UUID.randomUUID().toString())
      .when(keycloakService).upsertUser(user, PASSWORD);

    var result = userService.createUserSafe(user, PASSWORD, false);

    assertThat(result).isEqualTo(user);
    verify(usersClient, times(3)).createUser(user);
    verify(keycloakService, times(3)).findUserByUsername(USERNAME, false);
    verify(keycloakService, times(3)).upsertUser(user, PASSWORD);
  }

  @Test
  void deleteUser_positive_userExists_withResources() {
    var user = user();
    var userId = user.getId();
    var collectionResponse = new CollectionResponse();
    collectionResponse.setTotalRecords(1);

    when(usersClient.lookupUserById(userId)).thenReturn(Optional.of(user));
    doNothing().when(keycloakService).deleteUser(userId);
    doNothing().when(usersClient).deleteUser(userId);
    doNothing().when(userRolesClient).deleteUserRoles(userId);
    doNothing().when(userCapabilitySetClient).deleteUserCapabilitySet(userId);
    doNothing().when(userCapabilitiesClient).deleteUserCapabilities(userId);
    doNothing().when(policyService).removePolicyByUserId(userId);
    when(userRolesClient.findUserRoles(userId)).thenReturn(Optional.of(collectionResponse));
    when(userCapabilitySetClient.findUserCapabilitySet(userId)).thenReturn(Optional.of(collectionResponse));
    when(userCapabilitiesClient.findUserCapabilities(userId)).thenReturn(Optional.of(collectionResponse));

    userService.deleteUser(userId);

    verify(usersClient).lookupUserById(userId);
    verify(usersClient).deleteUser(userId);
    verify(keycloakService).deleteUser(userId);
    verify(capabilitiesService).unassignAll(userId);
  }

  @Test
  void deleteUser_positive_userExists_withoutResources() {
    var user = user();
    var userId = user.getId();
    var collectionResponse = new CollectionResponse();
    collectionResponse.setTotalRecords(0);

    when(usersClient.lookupUserById(userId)).thenReturn(Optional.of(user));
    doNothing().when(keycloakService).deleteUser(userId);
    doNothing().when(usersClient).deleteUser(userId);
    doNothing().when(userRolesClient).deleteUserRoles(userId);
    doNothing().when(userCapabilitySetClient).deleteUserCapabilitySet(userId);
    doNothing().when(userCapabilitiesClient).deleteUserCapabilities(userId);
    doNothing().when(policyService).removePolicyByUserId(userId);
    when(userRolesClient.findUserRoles(userId)).thenReturn(Optional.of(collectionResponse));
    when(userCapabilitySetClient.findUserCapabilitySet(userId)).thenReturn(Optional.of(collectionResponse));
    when(userCapabilitiesClient.findUserCapabilities(userId)).thenReturn(Optional.of(collectionResponse));

    userService.deleteUser(userId);

    verify(usersClient).lookupUserById(userId);
    verify(usersClient).deleteUser(userId);
    verify(keycloakService).deleteUser(userId);
    verify(userRolesClient, times(0)).deleteUserRoles(userId);
    verify(userCapabilitySetClient, times(0)).deleteUserCapabilitySet(userId);
    verify(userCapabilitiesClient, times(0)).deleteUserCapabilities(userId);
    verify(capabilitiesService).unassignAll(userId);
  }

  @Test
  void deleteUser_positive_userNotExists() {
    var user = user();
    var userId = user.getId();

    when(usersClient.lookupUserById(userId)).thenReturn(Optional.empty());
    doNothing().when(keycloakService).deleteUser(userId);

    userService.deleteUser(userId);

    verify(usersClient).lookupUserById(userId);
    verify(usersClient, times(0)).deleteUser(userId);
    verify(keycloakService).deleteUser(userId);
  }

  @Test
  void deleteUserById_positive_withResources() {
    var userId = randomUUID();
    var collectionResponse = new CollectionResponse();
    collectionResponse.setTotalRecords(1);

    doNothing().when(keycloakService).deleteUser(userId);
    doNothing().when(usersClient).deleteUser(userId);
    doNothing().when(userRolesClient).deleteUserRoles(userId);
    doNothing().when(userCapabilitySetClient).deleteUserCapabilitySet(userId);
    doNothing().when(userCapabilitiesClient).deleteUserCapabilities(userId);
    doNothing().when(policyService).removePolicyByUserId(userId);
    when(userRolesClient.findUserRoles(userId)).thenReturn(Optional.of(collectionResponse));
    when(userCapabilitySetClient.findUserCapabilitySet(userId)).thenReturn(Optional.of(collectionResponse));
    when(userCapabilitiesClient.findUserCapabilities(userId)).thenReturn(Optional.of(collectionResponse));

    userService.deleteUserById(userId);

    verify(usersClient).deleteUser(userId);
    verify(keycloakService).deleteUser(userId);
    verify(capabilitiesService).unassignAll(userId);
  }

  @Test
  void resolvePermissions() {
    var userId = randomUUID();
    var request = of("user.item.*");
    var response = of("user.item.get", "user.item.post", "user.item.put");
    var userPermissions = new UserPermissions();
    userPermissions.setPermissions(response);

    when(userPermissionsClient.getPermissionsForUser(userId, false, request)).thenReturn(userPermissions);

    var result = userService.resolvePermissions(userId, request);

    assertThat(result).containsExactlyInAnyOrderElementsOf(response);
    verify(userPermissionsClient).getPermissionsForUser(userId, false, request);
  }

  @Test
  void getUserBySelfReference_positive() {
    var userId = randomUUID();
    when(folioExecutionContext.getUserId()).thenReturn(userId);
    when(folioExecutionContext.getToken()).thenReturn("");
    when(usersClient.lookupUserById(userId)).thenReturn(Optional.of(mock(User.class)));
    when(servicePointsUserClient.getServicePointsUser(userId)).thenReturn(mock(ServicePointUserCollection.class));
    var permissions = new UserPermissions();
    permissions.setPermissions(List.of("some.permission"));
    when(userPermissionsClient.getPermissionsForUser(eq(userId), any(), any())).thenReturn(permissions);

    var resultExpanded = userService.getUserBySelfReference(List.of(EXPANDED_PERMS), true, false);

    verify(usersClient, times(1)).lookupUserById(userId);
    verify(userPermissionsClient, times(1)).getPermissionsForUser(eq(userId), any(), any());
    verify(servicePointsUserClient, times(1)).getServicePointsUser(userId);

    assertThat(resultExpanded.getPermissions()).isNotNull();
    assertThat(resultExpanded.getPermissions().getPermissions()).hasSize(1);
    assertThat(resultExpanded.getPermissions().getPermissions().get(0)).isEqualTo(
      Map.of(PERMISSION_NAME_FIELD, "some.permission"));

    var result = userService.getUserBySelfReference(List.of(EXPANDED_PERMS), false, false);

    verify(usersClient, times(2)).lookupUserById(userId);
    verify(userPermissionsClient, times(2)).getPermissionsForUser(eq(userId), any(), any());
    verify(servicePointsUserClient, times(2)).getServicePointsUser(userId);

    assertThat(result.getPermissions()).isNotNull();
    assertThat(result.getPermissions().getPermissions()).hasSize(1);
    assertThat(result.getPermissions().getPermissions().get(0)).isEqualTo("some.permission");
  }

  @Test
  void getUserBySelfReference_positive_overrideUser() {
    var userId = randomUUID();
    var user = mock(User.class);
    when(user.getId()).thenReturn(userId);
    when(user.getUsername()).thenReturn("username-12345");
    when(user.getType()).thenReturn("shadow");
    when(user.getCustomFields().get(ORIGINAL_TENANT_ID_CUSTOM_FIELD))
      .thenReturn(Map.of(ORIGINAL_TENANT_ID_CUSTOM_FIELD, "testtenant"));
    when(folioExecutionContext.getUserId()).thenReturn(userId);
    when(folioExecutionContext.getToken()).thenReturn("");
    when(folioExecutionContext.getOkapiHeaders()).thenReturn(Map.ofEntries(
      Map.entry("x-okapi-tenant", List.of("centraltenant"))));
    when(usersClient.lookupUserById(userId)).thenReturn(Optional.of(user));
    when(servicePointsUserClient.getServicePointsUser(userId)).thenReturn(mock(ServicePointUserCollection.class));
    var permissions = new UserPermissions();
    permissions.setPermissions(List.of("some.permission"));
    when(userPermissionsClient.getPermissionsForUser(eq(userId), any(), any())).thenReturn(permissions);

    var resultExpanded = userService.getUserBySelfReference(List.of(EXPANDED_PERMS), true, true);

    verify(usersClient, times(2)).lookupUserById(userId);
    verify(userPermissionsClient, times(1)).getPermissionsForUser(eq(userId), any(), any());
    verify(servicePointsUserClient, times(1)).getServicePointsUser(userId);

    assertThat(resultExpanded.getPermissions()).isNotNull();
    assertThat(resultExpanded.getPermissions().getPermissions()).hasSize(1);
    assertThat(resultExpanded.getPermissions().getPermissions().get(0)).isEqualTo(
      Map.of(PERMISSION_NAME_FIELD, "some.permission"));

    var result = userService.getUserBySelfReference(List.of(EXPANDED_PERMS), false, true);

    verify(usersClient, times(4)).lookupUserById(userId);
    verify(userPermissionsClient, times(2)).getPermissionsForUser(eq(userId), any(), any());
    verify(servicePointsUserClient, times(2)).getServicePointsUser(userId);

    assertThat(result.getPermissions()).isNotNull();
    assertThat(result.getPermissions().getPermissions()).hasSize(1);
    assertThat(result.getPermissions().getPermissions().get(0)).isEqualTo("some.permission");
  }

  @Test
  void getUserBySelfReference_positive_noCustomFieldOriginalTenantId() {
    var userId = randomUUID();
    var user = mock(User.class);
    when(user.getId()).thenReturn(userId);
    when(user.getUsername()).thenReturn("username_12345");
    when(user.getType()).thenReturn("shadow");
    // No custom Field "originaltenantid"
    when(user.getCustomFields().get(ORIGINAL_TENANT_ID_CUSTOM_FIELD))
      .thenReturn(Map.of());
    when(folioExecutionContext.getUserId()).thenReturn(userId);
    when(folioExecutionContext.getToken()).thenReturn("");
    when(folioExecutionContext.getOkapiHeaders()).thenReturn(Map.ofEntries(
      Map.entry("x-okapi-tenant", List.of("centraltenant"))));
    when(usersClient.lookupUserById(userId)).thenReturn(Optional.of(user));
    when(servicePointsUserClient.getServicePointsUser(userId)).thenReturn(mock(ServicePointUserCollection.class));
    var permissions = new UserPermissions();
    permissions.setPermissions(List.of("some.permission"));
    when(userPermissionsClient.getPermissionsForUser(eq(userId), any(), any())).thenReturn(permissions);

    var resultExpanded = userService.getUserBySelfReference(List.of(EXPANDED_PERMS), true, true);

    verify(usersClient, times(1)).lookupUserById(userId);
    verify(userPermissionsClient, times(1)).getPermissionsForUser(eq(userId), any(), any());
    verify(servicePointsUserClient, times(1)).getServicePointsUser(userId);

    assertThat(resultExpanded.getPermissions()).isNotNull();
    assertThat(resultExpanded.getPermissions().getPermissions()).hasSize(1);
    assertThat(resultExpanded.getPermissions().getPermissions().get(0)).isEqualTo(
      Map.of(PERMISSION_NAME_FIELD, "some.permission"));
  }

  @ParameterizedTest
  @ValueSource(strings = {"staff", ""})
  void getUserBySelfReference_positive_overrideUserWrongUserType(String userType) {
    var userId = randomUUID();
    var user = mock(User.class);
    when(user.getId()).thenReturn(userId);
    when(user.getUsername()).thenReturn("username-12345");
    // Wrong user type (expecting "shadow")
    when(user.getType()).thenReturn(userType);
    when(user.getCustomFields().get(ORIGINAL_TENANT_ID_CUSTOM_FIELD))
      .thenReturn(Map.of(ORIGINAL_TENANT_ID_CUSTOM_FIELD, "testtenant"));
    when(folioExecutionContext.getUserId()).thenReturn(userId);
    when(folioExecutionContext.getToken()).thenReturn("");
    when(folioExecutionContext.getOkapiHeaders()).thenReturn(Map.ofEntries(
      Map.entry("x-okapi-tenant", List.of("centraltenant"))));
    when(usersClient.lookupUserById(userId)).thenReturn(Optional.of(user));
    when(servicePointsUserClient.getServicePointsUser(userId)).thenReturn(mock(ServicePointUserCollection.class));
    var permissions = new UserPermissions();
    permissions.setPermissions(List.of("some.permission"));
    when(userPermissionsClient.getPermissionsForUser(eq(userId), any(), any())).thenReturn(permissions);

    var resultExpanded = userService.getUserBySelfReference(List.of(EXPANDED_PERMS), true, true);

    verify(usersClient, times(1)).lookupUserById(userId);
    verify(userPermissionsClient, times(1)).getPermissionsForUser(eq(userId), any(), any());
    verify(servicePointsUserClient, times(1)).getServicePointsUser(userId);

    assertThat(resultExpanded.getPermissions()).isNotNull();
    assertThat(resultExpanded.getPermissions().getPermissions()).hasSize(1);
    assertThat(resultExpanded.getPermissions().getPermissions().get(0)).isEqualTo(
      Map.of(PERMISSION_NAME_FIELD, "some.permission"));
  }

  private static User user() {
    return user(USERNAME);
  }

  private static User user(String username) {
    return new User()
      .id(randomUUID())
      .username(username);
  }
}
