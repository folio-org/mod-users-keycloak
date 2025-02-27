package org.folio.uk.service;

import static org.folio.uk.utils.UserUtils.ORIGINAL_TENANT_ID_CUSTOM_FIELD;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atMostOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.folio.spring.FolioExecutionContext;
import org.folio.test.types.UnitTest;
import org.folio.uk.domain.dto.User;
import org.folio.uk.domain.dto.UserTenant;
import org.folio.uk.domain.dto.UserTenantCollection;
import org.folio.uk.domain.model.UserType;
import org.folio.uk.integration.keycloak.KeycloakClient;
import org.folio.uk.integration.keycloak.KeycloakService;
import org.folio.uk.integration.keycloak.TokenService;
import org.folio.uk.integration.keycloak.config.KeycloakFederatedAuthProperties;
import org.folio.uk.integration.keycloak.config.KeycloakLoginClientProperties;
import org.folio.uk.integration.keycloak.model.FederatedIdentity;
import org.folio.uk.integration.users.UserTenantsClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@UnitTest
@ExtendWith(MockitoExtension.class)
class UserServiceIdentityProviderTest {

  private static final String USER_ID = UUID.randomUUID().toString();
  private static final String STAFF_USERNAME = "test";
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
  @Mock private UserTenantsClient userTenantsClient;
  @Mock private FolioExecutionContext folioExecutionContext;
  @Mock private KeycloakLoginClientProperties loginClientProperties;
  @Mock private KeycloakFederatedAuthProperties keycloakFederatedAuthProperties;
  @InjectMocks private KeycloakService keycloakService;

  @Test
  void linkIdentityProviderToUser_positive() {
    when(folioExecutionContext.getTenantId()).thenReturn(CENTRAL_TENANT_NAME);
    var userTenant = createUserTenant();
    when(userTenantsClient.lookupByTenantId(CENTRAL_TENANT_NAME))
      .thenReturn(new UserTenantCollection().userTenants(List.of(userTenant)));
    when(keycloakClient.getUserIdentityProvider(CENTRAL_TENANT_NAME, KC_USER_ID, AUTH_TOKEN)).thenReturn(List.of());
    when(tokenService.issueToken()).thenReturn(AUTH_TOKEN);
    when(keycloakFederatedAuthProperties.getIdentityProviderSuffix()).thenReturn(PROVIDER_SUFFIX);
    var user = createShadowUser(Map.of(ORIGINAL_TENANT_ID_CUSTOM_FIELD, TENANT_NAME));
    keycloakService.linkIdentityProviderToUser(user, KC_USER_ID);

    verify(keycloakClient, atMostOnce()).linkIdentityProviderToUser(eq(CENTRAL_TENANT_NAME), eq(KC_USER_ID),
      eq(PROVIDER_ALIAS), any(FederatedIdentity.class), eq(AUTH_TOKEN));
  }

  @Test
  void linkIdentityProviderToUser_positive_emptyMemberTenant() {
    // Empty member tenant ("originatenantid")
    var user = createStaffUser(Map.of());
    keycloakService.linkIdentityProviderToUser(user, KC_USER_ID);

    verify(keycloakClient, never()).linkIdentityProviderToUser(eq(CENTRAL_TENANT_NAME), eq(KC_USER_ID),
      eq(PROVIDER_ALIAS), any(FederatedIdentity.class), eq(AUTH_TOKEN));
  }

  @Test
  void linkIdentityProviderToUser_positive_wrongUserType() {
    // Wrong user type
    var user = createStaffUser(Map.of(ORIGINAL_TENANT_ID_CUSTOM_FIELD, TENANT_NAME));
    keycloakService.linkIdentityProviderToUser(user, KC_USER_ID);

    verify(keycloakClient, never()).linkIdentityProviderToUser(eq(CENTRAL_TENANT_NAME), eq(KC_USER_ID),
      eq(PROVIDER_ALIAS), any(FederatedIdentity.class), eq(AUTH_TOKEN));
  }

  @Test
  void linkIdentityProviderToUser_positive_emptyUserTenant() {
    when(folioExecutionContext.getTenantId()).thenReturn(CENTRAL_TENANT_NAME);
    // Empty user tenant
    when(userTenantsClient.lookupByTenantId(CENTRAL_TENANT_NAME))
      .thenReturn(new UserTenantCollection().userTenants(List.of()));
    var user = createShadowUser(Map.of(ORIGINAL_TENANT_ID_CUSTOM_FIELD, TENANT_NAME));
    keycloakService.linkIdentityProviderToUser(user, KC_USER_ID);

    verify(keycloakClient, never()).linkIdentityProviderToUser(eq(CENTRAL_TENANT_NAME), eq(KC_USER_ID),
      eq(PROVIDER_ALIAS), any(FederatedIdentity.class), eq(AUTH_TOKEN));
  }

  @Test
  void linkIdentityProviderToUser_positive_emptyCentralTenantIdUserTenant() {
    // Empty central tenant id user tenant
    when(folioExecutionContext.getTenantId()).thenReturn(CENTRAL_TENANT_NAME);
    var userTenant = new UserTenant()
      .userId(USER_ID)
      .tenantId(TENANT_NAME);
    when(userTenantsClient.lookupByTenantId(CENTRAL_TENANT_NAME))
      .thenReturn(new UserTenantCollection().userTenants(List.of(userTenant)));
    var user = createShadowUser(Map.of(ORIGINAL_TENANT_ID_CUSTOM_FIELD, TENANT_NAME));
    keycloakService.linkIdentityProviderToUser(user, KC_USER_ID);

    verify(keycloakClient, never()).linkIdentityProviderToUser(eq(CENTRAL_TENANT_NAME), eq(KC_USER_ID),
      eq(PROVIDER_ALIAS), any(FederatedIdentity.class), eq(AUTH_TOKEN));
  }

  @Test
  void linkIdentityProviderToUser_positive_nonCentralTenantUserTenant() {
    when(folioExecutionContext.getTenantId()).thenReturn(CENTRAL_TENANT_NAME);
    // Non-central tenant user tenant
    var userTenant = new UserTenant()
      .userId(USER_ID)
      .tenantId(TENANT_NAME)
      .centralTenantId(TENANT_NAME);
    when(userTenantsClient.lookupByTenantId(CENTRAL_TENANT_NAME))
      .thenReturn(new UserTenantCollection().userTenants(List.of(userTenant)));
    var user = createShadowUser(Map.of(ORIGINAL_TENANT_ID_CUSTOM_FIELD, TENANT_NAME));
    keycloakService.linkIdentityProviderToUser(user, KC_USER_ID);

    verify(keycloakClient, never()).linkIdentityProviderToUser(eq(CENTRAL_TENANT_NAME), eq(KC_USER_ID),
      eq(PROVIDER_ALIAS), any(FederatedIdentity.class), eq(AUTH_TOKEN));
  }

  @Test
  void linkIdentityProviderToUser_positive_identityProviderAlreadyLinked() {
    when(folioExecutionContext.getTenantId()).thenReturn(CENTRAL_TENANT_NAME);
    var userTenant = createUserTenant();
    when(userTenantsClient.lookupByTenantId(CENTRAL_TENANT_NAME))
      .thenReturn(new UserTenantCollection().userTenants(List.of(userTenant)));
    // Identity provider already linked
    when(keycloakClient.getUserIdentityProvider(CENTRAL_TENANT_NAME, KC_USER_ID, AUTH_TOKEN))
      .thenReturn(List.of(createUserIdentityProvider()));
    when(tokenService.issueToken()).thenReturn(AUTH_TOKEN);
    when(keycloakFederatedAuthProperties.getIdentityProviderSuffix()).thenReturn(PROVIDER_SUFFIX);
    var user = createShadowUser(Map.of(ORIGINAL_TENANT_ID_CUSTOM_FIELD, TENANT_NAME));
    keycloakService.linkIdentityProviderToUser(user, KC_USER_ID);

    verify(keycloakClient, never()).linkIdentityProviderToUser(eq(CENTRAL_TENANT_NAME), eq(KC_USER_ID),
      eq(PROVIDER_ALIAS), any(FederatedIdentity.class), eq(AUTH_TOKEN));
  }

  private User createShadowUser(Map<String, Object> customFields) {
    return new User()
      .id(UUID.fromString(USER_ID))
      .username(SHADOW_USERNAME)
      .type(UserType.SHADOW.getValue())
      .customFields(customFields);
  }

  private User createStaffUser(Map<String, Object> customFields) {
    return new User()
      .id(UUID.fromString(USER_ID))
      .username(STAFF_USERNAME)
      .type(UserType.STAFF.getValue())
      .customFields(customFields);
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
