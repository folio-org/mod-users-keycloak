package org.folio.uk.it;

import static org.assertj.core.api.Assertions.assertThat;
import static org.folio.test.extensions.impl.KeycloakContainerExtension.getKeycloakAdminClient;
import static org.folio.uk.support.TestConstants.TENANT_NAME;

import java.util.List;
import java.util.UUID;
import org.folio.test.types.IntegrationTest;
import org.folio.uk.base.BaseIntegrationTest;
import org.folio.uk.integration.keycloak.KeycloakClient;
import org.folio.uk.integration.keycloak.TokenService;
import org.folio.uk.integration.keycloak.model.ScopePermission;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@IntegrationTest
class KeycloakRealmManagementServiceIT extends BaseIntegrationTest {

  @Autowired private TokenService tokenService;
  @Autowired private KeycloakClient keycloakClient;

  @BeforeAll
  static void beforeAll() {
    enableTenant(TENANT_NAME);
  }

  @AfterAll
  static void afterAll(@Autowired KeycloakClient client, @Autowired TokenService tokenService) {
    removeTenant(TENANT_NAME, false);

    var authToken = tokenService.issueToken();
    var loginClientKcId = getLoginClientKcId(client, authToken);
    var permissions = findPermissions(client, authToken, loginClientKcId);

    assertThat(permissions).isEmpty();
    getKeycloakAdminClient().realm(TENANT_NAME).remove();
  }

  @Test
  void checkPasswordResetPermissions() {
    var authToken = tokenService.issueToken();
    var loginClientKcId = getLoginClientKcId(keycloakClient, authToken);
    var permissions = findPermissions(keycloakClient, authToken, loginClientKcId);

    assertThat(permissions).hasSize(2);
  }

  private static List<ScopePermission> findPermissions(KeycloakClient client, String authToken, UUID loginClientKcId) {
    return client.findScopePermission(TENANT_NAME, loginClientKcId, "Password Reset policy", authToken);
  }

  private static UUID getLoginClientKcId(KeycloakClient client, String authToken) {
    var loginClient = client.findClientsByClientId(TENANT_NAME, TENANT_NAME + "-login-application", authToken).get(0);
    return loginClient.getId();
  }
}
