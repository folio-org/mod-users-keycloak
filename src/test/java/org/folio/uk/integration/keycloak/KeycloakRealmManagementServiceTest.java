package org.folio.uk.integration.keycloak;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.folio.common.utils.KeycloakPermissionUtils.toPermissionName;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import feign.FeignException;
import java.util.List;
import org.folio.test.types.UnitTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@UnitTest
@ExtendWith(MockitoExtension.class)
class KeycloakRealmManagementServiceTest {
  private static final String POLICY = "Password Reset policy";
  private static final String ENDPOINT1 = "/users-keycloak/password-reset/reset";
  private static final String ENDPOINT2 = "/users-keycloak/password-reset/validate";
  private static final List<String> SCOPES = List.of("POST");

  @Mock private KeycloakService keycloakService;
  @Mock private RealmConfigurationProvider realmConfigurationProvider;
  @InjectMocks private KeycloakRealmManagementService service;

  @Test
  void setupRealm_positive() {
    service.setupRealm();
    verify(keycloakService).createScopePermission(POLICY, ENDPOINT1, SCOPES);
    verify(keycloakService).createScopePermission(POLICY, ENDPOINT2, SCOPES);
  }

  @Test
  void setupRealm_negative_exception() {
    doThrow(FeignException.class).when(keycloakService).createScopePermission(any(), any(), any());
    assertThatThrownBy(() -> service.setupRealm())
      .isInstanceOf(FeignException.class);
  }

  @Test
  void cleanupRealm_positive() {
    service.cleanupRealm();
    verify(keycloakService).deleteScopePermission(toPermissionName(SCOPES, POLICY, ENDPOINT1));
    verify(keycloakService).deleteScopePermission(toPermissionName(SCOPES, POLICY, ENDPOINT2));
    verify(realmConfigurationProvider).evictAllClientConfigurations();
  }

  @Test
  void cleanupRealm_negative() {
    doThrow(FeignException.class).when(keycloakService).deleteScopePermission(any());
    assertThatThrownBy(() -> service.cleanupRealm())
      .isInstanceOf(FeignException.class);
  }
}
