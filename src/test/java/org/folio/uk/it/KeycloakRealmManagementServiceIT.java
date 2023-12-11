package org.folio.uk.it;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.UUID;
import org.folio.test.types.IntegrationTest;
import org.folio.uk.base.BaseIntegrationTest;
import org.folio.uk.base.KeycloakTestClient;
import org.folio.uk.integration.keycloak.KeycloakClient;
import org.folio.uk.integration.keycloak.TokenService;
import org.folio.uk.integration.keycloak.model.ScopePermission;
import org.folio.uk.support.TestConstants;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@IntegrationTest
class KeycloakRealmManagementServiceIT extends BaseIntegrationTest {

  @Autowired private TokenService tokenService;
  @Autowired private KeycloakClient keycloakClient;

  @BeforeAll
  static void beforeAll(@Autowired KeycloakTestClient client, @Autowired TokenService tokenService) {
    enableTenant(TestConstants.TENANT_NAME, tokenService, client);
  }

  @AfterAll
  static void afterAll(@Autowired KeycloakClient client, @Autowired TokenService tokenService) {
    removeTenant(TestConstants.TENANT_NAME);

    var authToken = tokenService.issueToken();
    var loginClientKcId = getLoginClientKcId(client, authToken);
    var permissions = findPermissions(client, authToken, loginClientKcId);

    assertThat(permissions).isEmpty();
  }

  @Test
  void checkPasswordResetPermissions() {
    var authToken = tokenService.issueToken();
    var loginClientKcId = getLoginClientKcId(keycloakClient, authToken);
    var permissions = findPermissions(keycloakClient, authToken, loginClientKcId);

    assertThat(permissions).hasSize(2);
  }

  private static List<ScopePermission> findPermissions(KeycloakClient client, String authToken, UUID loginClientKcId) {
    return client.findScopePermission(TestConstants.TENANT_NAME, loginClientKcId, "Password Reset policy", authToken);
  }

  private static UUID getLoginClientKcId(KeycloakClient client, String authToken) {
    var loginClient =
      client.findClientsByClientId(TestConstants.TENANT_NAME, "master-login-application", authToken).get(0);
    return loginClient.getId();
  }
}
