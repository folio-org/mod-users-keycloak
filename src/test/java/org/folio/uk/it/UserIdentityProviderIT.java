package org.folio.uk.it;

import static org.folio.test.TestUtils.parseResponse;
import static org.folio.uk.support.TestConstants.CENTRAL_TENANT_NAME;
import static org.folio.uk.support.TestConstants.TENANT_NAME;

import org.folio.test.extensions.WireMockStub;
import org.folio.test.types.IntegrationTest;
import org.folio.uk.base.BaseIntegrationTest;
import org.folio.uk.domain.dto.User;
import org.folio.uk.integration.keycloak.config.KeycloakFederatedAuthProperties;
import org.folio.uk.support.TestConstants;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@IntegrationTest
@SuppressWarnings("squid:S2699")
class UserIdentityProviderIT extends BaseIntegrationTest {

  @Autowired protected KeycloakFederatedAuthProperties keycloakFederatedAuthProperties;

  private String tenant;
  private User user;

  @BeforeAll
  static void beforeAll() {
    enableTenant(CENTRAL_TENANT_NAME);
    enableTenant(TENANT_NAME);
  }

  @AfterAll
  static void afterAll() {
    removeTenant(CENTRAL_TENANT_NAME);
    removeTenant(TENANT_NAME);
  }

  @BeforeEach
  void beforeEach() {
    keycloakFederatedAuthProperties.setEnabled(true);
    createIdentityProviderInCentralTenant();
  }

  @AfterEach
  void afterEach() {
    removeShadowKeycloakUserInCentralTenant(tenant, user);
    removeIdentityProviderInCentralTenant();
  }

  @Test
  @WireMockStub({
    "/wiremock/stubs/users/create-user-shadow.json",
    "/wiremock/stubs/users/get-user-tenants.json"
  })
  void create_positive() throws Exception {
    tenant = CENTRAL_TENANT_NAME;
    user = TestConstants.shadowUser();

    var mvcResult = doPostWithTenant("/users-keycloak/users", tenant, user).andReturn();
    var resp = parseResponse(mvcResult, User.class);

    assertSuccessfulUserCreation(resp, user);

    verifyKeycloakUserAndIdentityProvider(tenant, user);
  }

  @Test
  @WireMockStub({
    "/wiremock/stubs/users/create-user-shadow.json",
    "/wiremock/stubs/users/get-user-tenants.json"
  })
  void update_positive() throws Exception {
    tenant = CENTRAL_TENANT_NAME;
    user = TestConstants.shadowUser();

    doPostWithTenant("/users-keycloak/users", tenant, user).andReturn();
    var mvcResult = doPostWithTenant("/users-keycloak/users", tenant, user).andReturn();
    var resp = parseResponse(mvcResult, User.class);

    assertSuccessfulUserCreation(resp, user);

    verifyKeycloakUserAndIdentityProvider(tenant, user);
  }

  @Test
  @WireMockStub({
    "/wiremock/stubs/users/create-user-shadow.json",
    "/wiremock/stubs/users/get-user-tenants-empty-array.json",
  })
  void create_positive_emptyUserTenants() throws Exception {
    tenant = CENTRAL_TENANT_NAME;
    user = TestConstants.shadowUser();

    var mvcResult = doPostWithTenant("/users-keycloak/users", tenant, user).andReturn();
    var resp = parseResponse(mvcResult, User.class);

    assertSuccessfulUserCreation(resp, user);

    verifyKeycloakUserAndWithNoIdentityProviderCreated(tenant, user);
  }

  @Test
  @WireMockStub({
    "/wiremock/stubs/users/create-user-shadow.json",
    "/wiremock/stubs/users/get-user-tenants-empty-central-tenant-id.json",
  })
  void create_positive_emptyCentralTenantUserTenants() throws Exception {
    tenant = CENTRAL_TENANT_NAME;
    user = TestConstants.shadowUser();

    var mvcResult = doPostWithTenant("/users-keycloak/users", tenant, user).andReturn();
    var resp = parseResponse(mvcResult, User.class);

    assertSuccessfulUserCreation(resp, user);

    verifyKeycloakUserAndWithNoIdentityProviderCreated(tenant, user);
  }

  @Test
  @WireMockStub({
    "/wiremock/stubs/users/create-user-central.json",
    "/wiremock/stubs/users/get-user-tenants-central.json",
  })
  void create_positive_asCentralTenantRealUser() throws Exception {
    tenant = CENTRAL_TENANT_NAME;
    user = TestConstants.user();

    var mvcResult = doPostWithTenant("/users-keycloak/users", tenant, user).andReturn();
    var resp = parseResponse(mvcResult, User.class);

    assertSuccessfulUserCreation(resp, user);

    verifyKeycloakUserAndWithNoIdentityProviderCreated(tenant, user);
  }

  @Test
  @WireMockStub({
    "/wiremock/stubs/users/create-user.json",
    "/wiremock/stubs/users/get-user-tenants-same-tenant-ids.json",
  })
  void create_positive_asMemberTenantRealUser() throws Exception {
    tenant = TENANT_NAME;
    user = TestConstants.user();

    var mvcResult = doPostWithTenant("/users-keycloak/users", tenant, user).andReturn();
    var resp = parseResponse(mvcResult, User.class);

    assertSuccessfulUserCreation(resp, user);

    verifyKeycloakUserAndWithNoIdentityProviderCreated(tenant, user);
  }
}
