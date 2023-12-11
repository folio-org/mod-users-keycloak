package org.folio.uk.integration.keycloak;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.folio.uk.support.TestConstants.TENANT_NAME;
import static org.folio.uk.support.TestConstants.USER_ID;
import static org.folio.uk.support.TestConstants.USER_NAME;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import feign.FeignException;
import jakarta.ws.rs.NotFoundException;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.assertj.core.util.Lists;
import org.folio.spring.FolioExecutionContext;
import org.folio.test.types.UnitTest;
import org.folio.uk.integration.keycloak.config.KeycloakLoginClientProperties;
import org.folio.uk.integration.keycloak.model.Client;
import org.folio.uk.integration.keycloak.model.KeycloakRole;
import org.folio.uk.integration.keycloak.model.KeycloakUser;
import org.folio.uk.integration.keycloak.model.ScopePermission;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@UnitTest
@ExtendWith(MockitoExtension.class)
class KeycloakServiceTest {

  private static final String AUTH_TOKEN = "dGVzdC10b2tlbg==";
  private static final String ROLE = "test-role";
  private static final String LOGIN_CLIENT = "master-login-applications";
  private static final UUID LOGIN_CLIENT_KC_ID = UUID.randomUUID();
  private static final String LOGIN_CLIENT_SUFFIX = "-login-applications";

  @InjectMocks private KeycloakService keycloakService;
  @Mock private TokenService tokenService;
  @Mock private KeycloakClient keycloakClient;
  @Mock private FolioExecutionContext folioExecutionContext;
  @Mock private KeycloakLoginClientProperties loginClientProperties;

  @Test
  void findUserByUsername_positive() {
    var keycloakUsers = singletonList(keycloakUser());

    when(tokenService.issueToken()).thenReturn(AUTH_TOKEN);
    when(folioExecutionContext.getTenantId()).thenReturn(TENANT_NAME);
    when(keycloakClient.findUsersByUsername(TENANT_NAME, USER_NAME, true, AUTH_TOKEN)).thenReturn(keycloakUsers);

    var result = keycloakService.findUserByUsername(USER_NAME);

    assertThat(result).isPresent().get().isEqualTo(keycloakUser());
  }

  @Test
  void findUserByUsername_positive_emptyResult() {
    when(tokenService.issueToken()).thenReturn(AUTH_TOKEN);
    when(folioExecutionContext.getTenantId()).thenReturn(TENANT_NAME);
    when(keycloakClient.findUsersByUsername(TENANT_NAME, USER_NAME, true, AUTH_TOKEN)).thenReturn(emptyList());

    var result = keycloakService.findUserByUsername(USER_NAME);

    assertThat(result).isEmpty();
  }

  @Test
  void findUserByUsername_negative_feignException() {
    when(tokenService.issueToken()).thenReturn(AUTH_TOKEN);
    when(folioExecutionContext.getTenantId()).thenReturn(TENANT_NAME);
    when(keycloakClient.findUsersByUsername(TENANT_NAME, USER_NAME, true, AUTH_TOKEN)).thenThrow(FeignException.class);

    assertThatThrownBy(() -> keycloakService.findUserByUsername(USER_NAME))
      .isInstanceOf(KeycloakException.class)
      .hasMessage("Failed to find a user by username: " + USER_NAME);
  }

  @Test
  void hasRole_positive_trueResult() {
    var userId = USER_ID.toString();

    when(tokenService.issueToken()).thenReturn(AUTH_TOKEN);
    when(folioExecutionContext.getTenantId()).thenReturn(TENANT_NAME);
    when(keycloakClient.findUserRoles(TENANT_NAME, userId, AUTH_TOKEN)).thenReturn(List.of(keycloakRole(ROLE)));

    var result = keycloakService.hasRole(userId, ROLE);

    assertThat(result).isTrue();
  }

  @Test
  void hasRole_positive_falseResult() {
    var userId = USER_ID.toString();
    var resultRoles = List.of(keycloakRole("unknown"));

    when(tokenService.issueToken()).thenReturn(AUTH_TOKEN);
    when(folioExecutionContext.getTenantId()).thenReturn(TENANT_NAME);
    when(keycloakClient.findUserRoles(TENANT_NAME, userId, AUTH_TOKEN)).thenReturn(resultRoles);

    var result = keycloakService.hasRole(userId, ROLE);

    assertThat(result).isFalse();
  }

  @Test
  void hasRole_negative_feignException() {
    var userId = USER_ID.toString();

    when(tokenService.issueToken()).thenReturn(AUTH_TOKEN);
    when(folioExecutionContext.getTenantId()).thenReturn(TENANT_NAME);
    when(keycloakClient.findUserRoles(TENANT_NAME, userId, AUTH_TOKEN)).thenThrow(FeignException.class);

    assertThatThrownBy(() -> keycloakService.hasRole(userId, ROLE))
      .isInstanceOf(KeycloakException.class)
      .hasMessage(String.format("Failed to find user roles for user [role: %s, userId: %s]", ROLE, userId));
  }

  @Test
  void assignRole_positive() {
    var keycloakRole = keycloakRole(ROLE);

    when(tokenService.issueToken()).thenReturn(AUTH_TOKEN);
    when(folioExecutionContext.getTenantId()).thenReturn(TENANT_NAME);
    when(keycloakClient.findByName(TENANT_NAME, ROLE, AUTH_TOKEN)).thenReturn(keycloakRole);

    var userId = USER_ID.toString();
    keycloakService.assignRole(userId, ROLE);

    verify(keycloakClient).assignRolesToUser(TENANT_NAME, userId, List.of(keycloakRole), AUTH_TOKEN);
  }

  @Test
  void assignRole_negative_roleIsNotFound() {
    var userId = USER_ID.toString();

    when(tokenService.issueToken()).thenReturn(AUTH_TOKEN);
    when(folioExecutionContext.getTenantId()).thenReturn(TENANT_NAME);
    when(keycloakClient.findByName(TENANT_NAME, ROLE, AUTH_TOKEN)).thenThrow(NotFoundException.class);

    assertThatThrownBy(() -> keycloakService.assignRole(userId, ROLE))
      .isInstanceOf(KeycloakException.class)
      .hasMessage("Failed to find a role by name: " + ROLE);
  }

  @Test
  void assignRole_negative_feignException() {
    var userId = USER_ID.toString();
    var keycloakRole = keycloakRole(ROLE);

    when(tokenService.issueToken()).thenReturn(AUTH_TOKEN);
    when(folioExecutionContext.getTenantId()).thenReturn(TENANT_NAME);
    when(keycloakClient.findByName(TENANT_NAME, ROLE, AUTH_TOKEN)).thenReturn(keycloakRole);
    doThrow(FeignException.class).when(keycloakClient)
      .assignRolesToUser(TENANT_NAME, userId, List.of(keycloakRole), AUTH_TOKEN);

    assertThatThrownBy(() -> keycloakService.assignRole(userId, ROLE))
      .isInstanceOf(KeycloakException.class)
      .hasMessage(String.format("Failed to assign a role to user [userId: %s, role: %s]", userId, ROLE));
  }

  @Test
  void createScopePermission_positive() {
    when(tokenService.issueToken()).thenReturn(AUTH_TOKEN);
    when(folioExecutionContext.getTenantId()).thenReturn(TENANT_NAME);
    when(loginClientProperties.getClientNameSuffix()).thenReturn(LOGIN_CLIENT_SUFFIX);

    when(keycloakClient.findClientsByClientId(TENANT_NAME, LOGIN_CLIENT, AUTH_TOKEN)).thenReturn(
      List.of(keycloakClient()));

    var permission = scopePermission();
    when(keycloakClient.createScopePermission(any(), any(), any(), any())).thenReturn(permission);

    keycloakService.createScopePermission("policy", "/foo/bar", List.of("POST"));

    verify(keycloakClient).createScopePermission(TENANT_NAME, LOGIN_CLIENT_KC_ID, permission, AUTH_TOKEN);
  }

  @Test
  void createScopePermission_positive_ignoreConflict() {
    when(tokenService.issueToken()).thenReturn(AUTH_TOKEN);
    when(folioExecutionContext.getTenantId()).thenReturn(TENANT_NAME);
    when(loginClientProperties.getClientNameSuffix()).thenReturn(LOGIN_CLIENT_SUFFIX);

    when(keycloakClient.findClientsByClientId(TENANT_NAME, LOGIN_CLIENT, AUTH_TOKEN)).thenReturn(
      List.of(keycloakClient()));

    var permission = scopePermission();
    when(keycloakClient.createScopePermission(any(), any(), any(), any())).thenThrow(FeignException.Conflict.class);

    keycloakService.createScopePermission("policy", "/foo/bar", List.of("POST"));

    verify(keycloakClient).createScopePermission(TENANT_NAME, LOGIN_CLIENT_KC_ID, permission, AUTH_TOKEN);
  }

  @Test
  void createScopePermission_negative_clientIsNotFound() {
    when(tokenService.issueToken()).thenReturn(AUTH_TOKEN);
    when(folioExecutionContext.getTenantId()).thenReturn(TENANT_NAME);
    when(loginClientProperties.getClientNameSuffix()).thenReturn(LOGIN_CLIENT_SUFFIX);

    when(keycloakClient.findClientsByClientId(TENANT_NAME, LOGIN_CLIENT, AUTH_TOKEN)).thenReturn(
      Collections.emptyList());

    assertThatThrownBy(() -> keycloakService.createScopePermission("policy", "/foo/bar", List.of("POST")))
      .isInstanceOf(KeycloakException.class)
      .hasMessage(String.format("Keycloak client is not found by clientId: %s", LOGIN_CLIENT));
  }

  @Test
  void createScopePermission_negative_feignException() {
    when(tokenService.issueToken()).thenReturn(AUTH_TOKEN);
    when(folioExecutionContext.getTenantId()).thenReturn(TENANT_NAME);
    when(loginClientProperties.getClientNameSuffix()).thenReturn(LOGIN_CLIENT_SUFFIX);

    when(keycloakClient.findClientsByClientId(TENANT_NAME, LOGIN_CLIENT, AUTH_TOKEN)).thenReturn(
      List.of(keycloakClient()));

    doThrow(FeignException.class).when(keycloakClient)
      .createScopePermission(any(), any(), any(), any());

    assertThatThrownBy(() -> keycloakService.createScopePermission("policy", "/foo/bar", List.of("POST")))
      .isInstanceOf(KeycloakException.class)
      .hasMessage(
        String.format("Failed to create a permission [resource: [%s], policies: [%s]]", "/foo/bar", "policy"));
  }

  @Test
  void findClientWithClientId_positive() {
    when(tokenService.issueToken()).thenReturn(AUTH_TOKEN);

    var client = keycloakClient();
    when(keycloakClient.findClientsByClientId(TENANT_NAME, LOGIN_CLIENT, AUTH_TOKEN)).thenReturn(
      List.of(client));

    var found = keycloakService.findClientWithClientId(TENANT_NAME, LOGIN_CLIENT);
    assertThat(found).isEqualTo(client);
  }

  @Test
  void findClientWithClientId_negative_clientIsNotFound() {
    when(tokenService.issueToken()).thenReturn(AUTH_TOKEN);

    when(keycloakClient.findClientsByClientId(TENANT_NAME, LOGIN_CLIENT, AUTH_TOKEN)).thenReturn(
      Collections.emptyList());

    assertThatThrownBy(() -> keycloakService.findClientWithClientId(TENANT_NAME, LOGIN_CLIENT))
      .isInstanceOf(KeycloakException.class)
      .hasMessage(String.format("Keycloak client is not found by clientId: %s", LOGIN_CLIENT));
  }

  @Test
  void findClientWithClientId_negative_clientIsNull() {
    when(tokenService.issueToken()).thenReturn(AUTH_TOKEN);

    Client client = null;
    when(keycloakClient.findClientsByClientId(TENANT_NAME, LOGIN_CLIENT, AUTH_TOKEN)).thenReturn(
      Lists.newArrayList(client));

    assertThatThrownBy(() -> keycloakService.findClientWithClientId(TENANT_NAME, LOGIN_CLIENT))
      .isInstanceOf(KeycloakException.class)
      .hasMessage(String.format("Keycloak client is not found by clientId: %s", LOGIN_CLIENT));
  }

  @Test
  void findClientWithClientId_negative_multipleClientsFound() {
    when(tokenService.issueToken()).thenReturn(AUTH_TOKEN);

    when(keycloakClient.findClientsByClientId(TENANT_NAME, LOGIN_CLIENT, AUTH_TOKEN)).thenReturn(
      List.of(keycloakClient(), keycloakClient()));

    assertThatThrownBy(() -> keycloakService.findClientWithClientId(TENANT_NAME, LOGIN_CLIENT))
      .isInstanceOf(KeycloakException.class)
      .hasMessage(String.format("Too many keycloak clients with clientId: %s", LOGIN_CLIENT));
  }

  @Test
  void findClientWithClientId_negative_feignException() {
    when(tokenService.issueToken()).thenReturn(AUTH_TOKEN);

    when(keycloakClient.findClientsByClientId(TENANT_NAME, LOGIN_CLIENT, AUTH_TOKEN)).thenThrow(FeignException.class);

    assertThatThrownBy(() -> keycloakService.findClientWithClientId(TENANT_NAME, LOGIN_CLIENT))
      .isInstanceOf(KeycloakException.class)
      .hasMessage(String.format("Failed to find a keycloak client with clientId: %s", LOGIN_CLIENT));
  }

  private static ScopePermission scopePermission() {
    var permission = new ScopePermission();
    permission.setName("[POST] access for 'policy' to '/foo/bar'");
    permission.setScopes(List.of("POST"));
    permission.setResources(List.of("/foo/bar"));
    permission.setPolicies(List.of("policy"));
    return permission;
  }

  private static Client keycloakClient() {
    return new Client(LOGIN_CLIENT_KC_ID, LOGIN_CLIENT);
  }

  private static KeycloakRole keycloakRole(String name) {
    var keycloakRole = new KeycloakRole();
    keycloakRole.setName(name);
    return keycloakRole;
  }

  private static KeycloakUser keycloakUser() {
    var keycloakUser = new KeycloakUser();
    keycloakUser.setId(USER_ID.toString());
    keycloakUser.setUserName(USER_NAME);
    return keycloakUser;
  }
}
