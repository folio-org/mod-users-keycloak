package org.folio.uk.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atMostOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.folio.spring.FolioExecutionContext;
import org.folio.uk.domain.dto.User;
import org.folio.uk.domain.dto.UserTenant;
import org.folio.uk.domain.model.UserType;
import org.folio.uk.integration.keycloak.KeycloakClient;
import org.folio.uk.integration.keycloak.KeycloakService;
import org.folio.uk.integration.keycloak.TokenService;
import org.folio.uk.integration.keycloak.config.KeycloakFederatedAuthProperties;
import org.folio.uk.integration.keycloak.config.KeycloakLoginClientProperties;
import org.folio.uk.integration.keycloak.model.FederatedIdentity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserServiceIdentityProviderTest {

  private static final String USER_ID = UUID.randomUUID().toString();
  private static final String SHADOW_USERNAME = "test_12345";
  private static final String REAL_USERNAME = "test";
  private static final String KC_USER_ID = UUID.randomUUID().toString();
  private static final String CENTRAL_TENANT_NAME = "centraltenant";
  private static final String TENANT_NAME = "testtenant";
  private static final String AUTH_TOKEN = "authToken";
  private static final String PROVIDER_SUFFIX = "-keycloak-oidc";
  private static final String PROVIDER_ALIAS = "testtenant-keycloak-oidc";

  @Mock private KeycloakClient keycloakClient;
  @Mock private TokenService tokenService;
  @Mock private CacheableUserTenantService usersTenantService;
  @Mock private FolioExecutionContext folioExecutionContext;
  @Mock private KeycloakLoginClientProperties loginClientProperties;
  @Mock private KeycloakFederatedAuthProperties keycloakFederatedAuthProperties;
  @InjectMocks private KeycloakService keycloakService;

  @Test
  void linkIdentityProviderToUser_positive() {
    var userTenant = createUserTenant();
    when(usersTenantService.getUserTenant(UUID.fromString(USER_ID))).thenReturn(Optional.of(userTenant));
    when(keycloakClient.getUserIdentityProvider(CENTRAL_TENANT_NAME, KC_USER_ID, AUTH_TOKEN)).thenReturn(List.of());
    when(tokenService.issueToken()).thenReturn(AUTH_TOKEN);
    when(folioExecutionContext.getTenantId()).thenReturn(CENTRAL_TENANT_NAME);
    when(keycloakFederatedAuthProperties.getIdentityProviderSuffix()).thenReturn(PROVIDER_SUFFIX);
    var user = createUser(UserType.SHADOW.getValue());
    keycloakService.linkIdentityProviderToUser(user, KC_USER_ID);

    verify(keycloakClient, atMostOnce()).linkIdentityProviderToUser(eq(CENTRAL_TENANT_NAME), eq(KC_USER_ID),
      eq(PROVIDER_ALIAS), any(FederatedIdentity.class), eq(AUTH_TOKEN));
  }

  @Test
  void linkIdentityProviderToUser_positive_wrongUserType() {
    // Wrong user type
    var user = createUser(UserType.STAFF.getValue());
    keycloakService.linkIdentityProviderToUser(user, KC_USER_ID);

    verify(keycloakClient, never()).linkIdentityProviderToUser(eq(CENTRAL_TENANT_NAME), eq(KC_USER_ID),
      eq(PROVIDER_ALIAS), any(FederatedIdentity.class), eq(AUTH_TOKEN));
  }

  @Test
  void linkIdentityProviderToUser_positive_emptyUserTenant() {
    // Empty user tenant
    when(usersTenantService.getUserTenant(UUID.fromString(USER_ID))).thenReturn(Optional.empty());
    var user = createUser(UserType.SHADOW.getValue());
    keycloakService.linkIdentityProviderToUser(user, KC_USER_ID);

    verify(keycloakClient, never()).linkIdentityProviderToUser(eq(CENTRAL_TENANT_NAME), eq(KC_USER_ID),
      eq(PROVIDER_ALIAS), any(FederatedIdentity.class), eq(AUTH_TOKEN));
  }

  @Test
  void linkIdentityProviderToUser_positive_emptyCentralTenantIdUserTenant() {
    // Empty central tenant id user tenant
    var userTenant = new UserTenant()
      .userId(USER_ID)
      .tenantId(TENANT_NAME);
    when(usersTenantService.getUserTenant(UUID.fromString(USER_ID))).thenReturn(Optional.of(userTenant));
    var user = createUser(UserType.SHADOW.getValue());
    keycloakService.linkIdentityProviderToUser(user, KC_USER_ID);

    verify(keycloakClient, never()).linkIdentityProviderToUser(eq(CENTRAL_TENANT_NAME), eq(KC_USER_ID),
      eq(PROVIDER_ALIAS), any(FederatedIdentity.class), eq(AUTH_TOKEN));
  }

  @Test
  void linkIdentityProviderToUser_positive_nonCentralTenantUserTenant() {
    // Non central tenant user tenant
    var userTenant = new UserTenant()
      .userId(USER_ID)
      .tenantId(TENANT_NAME)
      .centralTenantId(TENANT_NAME);
    when(folioExecutionContext.getTenantId()).thenReturn(CENTRAL_TENANT_NAME);
    when(usersTenantService.getUserTenant(UUID.fromString(USER_ID))).thenReturn(Optional.of(userTenant));
    var user = createUser(UserType.SHADOW.getValue());
    keycloakService.linkIdentityProviderToUser(user, KC_USER_ID);

    verify(keycloakClient, never()).linkIdentityProviderToUser(eq(CENTRAL_TENANT_NAME), eq(KC_USER_ID),
      eq(PROVIDER_ALIAS), any(FederatedIdentity.class), eq(AUTH_TOKEN));
  }

  @Test
  void linkIdentityProviderToUser_positive_identityProviderAlreadyLinked() {
    var userTenant = createUserTenant();
    when(usersTenantService.getUserTenant(UUID.fromString(USER_ID))).thenReturn(Optional.of(userTenant));
    // Identity provider already linked
    when(keycloakClient.getUserIdentityProvider(CENTRAL_TENANT_NAME, KC_USER_ID, AUTH_TOKEN))
      .thenReturn(List.of(createUserIdentityProvider()));
    when(tokenService.issueToken()).thenReturn(AUTH_TOKEN);
    when(folioExecutionContext.getTenantId()).thenReturn(CENTRAL_TENANT_NAME);
    when(keycloakFederatedAuthProperties.getIdentityProviderSuffix()).thenReturn(PROVIDER_SUFFIX);
    var user = createUser(UserType.SHADOW.getValue());
    keycloakService.linkIdentityProviderToUser(user, KC_USER_ID);

    verify(keycloakClient, never()).linkIdentityProviderToUser(eq(CENTRAL_TENANT_NAME), eq(KC_USER_ID),
      eq(PROVIDER_ALIAS), any(FederatedIdentity.class), eq(AUTH_TOKEN));
  }

  private User createUser(String userType) {
    return new User()
      .id(UUID.fromString(USER_ID))
      .username(SHADOW_USERNAME)
      .type(userType);
  }

  private UserTenant createUserTenant() {
    return new UserTenant()
      .userId(USER_ID)
      .tenantId(TENANT_NAME)
      .centralTenantId(CENTRAL_TENANT_NAME);
  }

  private FederatedIdentity createUserIdentityProvider() {
    return FederatedIdentity.builder()
      .providerAlias(PROVIDER_ALIAS)
      .userId(REAL_USERNAME)
      .userName(REAL_USERNAME)
      .build();
  }
}
