package org.folio.uk.it;

import static org.assertj.core.api.Assertions.assertThat;
import static org.folio.test.TestUtils.parseResponse;
import static org.folio.uk.support.TestConstants.TENANT_NAME;

import org.folio.test.extensions.WireMockStub;
import org.folio.test.types.IntegrationTest;
import org.folio.uk.base.BaseIntegrationTest;
import org.folio.uk.domain.dto.User;
import org.folio.uk.integration.keycloak.config.KeycloakFederatedAuthProperties;
import org.folio.uk.support.TestConstants;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@IntegrationTest
@SuppressWarnings("squid:S2699")
class UserIdentityProviderIT extends BaseIntegrationTest {

  @Autowired
  protected KeycloakFederatedAuthProperties keycloakFederatedAuthProperties;

  @BeforeAll
  static void beforeAll() {
    enableTenant(TENANT_NAME);
  }

  @AfterAll
  static void afterAll() {
    removeTenant(TENANT_NAME);
  }

  @BeforeEach
  void beforeEach() {
    keycloakFederatedAuthProperties.setEnabled(true);
  }

  @Disabled
  @Test
  @WireMockStub({
    "/wiremock/stubs/users/create-user.json",
    "/wiremock/stubs/users/get-user-tenants.json"
  })
  void create_positive() throws Exception {
    var user = TestConstants.user();
    var mvcResult = doPost("/users-keycloak/users", user).andReturn();
    var resp = parseResponse(mvcResult, User.class);

    assertSuccessfulUserCreation(resp, user);

    verifyKeycloakUser(user);
  }

  @Test
  @WireMockStub({
    "/wiremock/stubs/users/create-user.json",
    "/wiremock/stubs/users/get-user-tenants-central.json",
  })
  void create_positive_isCentralTenant() throws Exception {
    var user = TestConstants.user();
    var mvcResult = doPost("/users-keycloak/users", user).andReturn();
    var resp = parseResponse(mvcResult, User.class);

    assertSuccessfulUserCreation(resp, user);

    verifyKeycloakUserAndWithNoIdentityProvider(user);
  }

  private void assertSuccessfulUserCreation(User resp, User user) {
    assertThat(resp.getId()).isEqualTo(user.getId());
    assertThat(resp.getUsername()).isEqualTo(user.getUsername());
    assertThat(resp.getBarcode()).isEqualTo(user.getBarcode());
    assertThat(resp.getPatronGroup()).isEqualTo(user.getPatronGroup());

    assertThat(resp.getPersonal()).isNotNull();
    assertThat(user.getPersonal()).isNotNull();
    assertThat(resp.getPersonal().getFirstName()).isEqualTo(user.getPersonal().getFirstName());
    assertThat(resp.getPersonal().getLastName()).isEqualTo(user.getPersonal().getLastName());
    assertThat(resp.getPersonal().getEmail()).isEqualTo(user.getPersonal().getEmail());

    assertThat(resp.getMetadata()).isNotNull();
  }
}
