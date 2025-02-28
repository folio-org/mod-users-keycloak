package org.folio.uk.it;

import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_NO_CONTENT;
import static org.awaitility.Awaitility.await;
import static org.folio.test.TestUtils.parseResponse;
import static org.folio.uk.support.TestConstants.CENTRAL_TENANT_NAME;
import static org.folio.uk.support.TestConstants.TENANT_NAME;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import org.folio.test.extensions.WireMockStub;
import org.folio.test.types.IntegrationTest;
import org.folio.uk.base.BaseIntegrationTest;
import org.folio.uk.domain.dto.User;
import org.folio.uk.domain.dto.UsersIdp;
import org.folio.uk.integration.keycloak.config.KeycloakFederatedAuthProperties;
import org.folio.uk.support.TestConstants;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@IntegrationTest
class IdpMigrationIT extends BaseIntegrationTest {

  @Autowired
  protected KeycloakFederatedAuthProperties keycloakFederatedAuthProperties;

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
    keycloakFederatedAuthProperties.setEnabled(false);
    createIdentityProviderInCentralTenant();
  }

  @AfterEach
  void afterEach() {
    removeShadowKeycloakUserInCentralTenant(tenant, user);
    removeIdentityProviderInCentralTenant();
  }

  @Test
  @WireMockStub({
    "/wiremock/stubs/users/create-shadow-user.json",
    "/wiremock/stubs/users/get-user-tenants.json",
    "/wiremock/stubs/users/get-shadow-users.json"
  })
  void linkUserIdpMigration_positive() throws Exception {
    createAndVerifyShadowUserWithoutLinkedIdp();

    keycloakFederatedAuthProperties.setEnabled(true);

    var usersIdp = createUsersIdp(tenant, Set.of(TestConstants.USER_ID));
    doPostWithTenantAndStatusCode("/users-keycloak/idp-migrations", tenant, usersIdp, SC_NO_CONTENT).andReturn();

    await().atMost(15, TimeUnit.SECONDS).pollDelay(10, TimeUnit.SECONDS).until(() -> {
      verifyKeycloakUserAndIdentityProvider(tenant, user);
      return true;
    });
  }

  @Test
  @WireMockStub({
    "/wiremock/stubs/users/create-shadow-user.json",
    "/wiremock/stubs/users/get-user-tenants.json",
    "/wiremock/stubs/users/get-shadow-users.json"
  })
  void unlinkUserIdpMigration_positive() throws Exception {
    keycloakFederatedAuthProperties.setEnabled(true);

    createAndVerifyShadowUserWithLinkedIdp();

    var usersIdp = createUsersIdp(tenant, Set.of(TestConstants.USER_ID));
    doDeleteWithTenantAndStatusCode("/users-keycloak/idp-migrations", tenant, usersIdp, SC_NO_CONTENT).andReturn();

    await().atMost(15, TimeUnit.SECONDS).pollDelay(10, TimeUnit.SECONDS).until(() -> {
      verifyKeycloakUserAndWithNoIdentityProviderExisting(tenant, user);
      return true;
    });
  }

  @Test
  @WireMockStub("/wiremock/stubs/users/create-shadow-user.json")
  void linkUserIdpMigration_positive_singleTenantUxDisabled() throws Exception {
    createAndVerifyShadowUserWithoutLinkedIdp();

    // With SINGLE_TENANT_UX=false (i.e. disabled)
    keycloakFederatedAuthProperties.setEnabled(false);

    var usersIdp = createUsersIdp(tenant, Set.of(TestConstants.USER_ID));
    doPostWithTenantAndStatusCode("/users-keycloak/idp-migrations", tenant, usersIdp, SC_NO_CONTENT).andReturn();

    verifyKeycloakUserAndWithNoIdentityProviderExisting(tenant, user);
  }

  @Test
  @WireMockStub("/wiremock/stubs/users/create-shadow-user.json")
  void linkUserIdpMigration_negative_nonCentralTenant() throws Exception {
    createAndVerifyShadowUserWithoutLinkedIdp();

    keycloakFederatedAuthProperties.setEnabled(true);

    // With non-central tenant
    var usersIdp = createUsersIdp(TENANT_NAME, Set.of(TestConstants.USER_ID));
    doPostWithTenantAndStatusCode("/users-keycloak/idp-migrations", tenant, usersIdp, SC_BAD_REQUEST).andReturn();

    verifyKeycloakUserAndWithNoIdentityProviderExisting(tenant, user);
  }

  @Test
  @WireMockStub("/wiremock/stubs/users/create-shadow-user.json")
  void linkUserIdpMigration_negative_emptyUserIds() throws Exception {
    createAndVerifyShadowUserWithoutLinkedIdp();

    keycloakFederatedAuthProperties.setEnabled(true);

    // With empty userIds
    var usersIdp = createUsersIdp(tenant, Set.of());
    doPostWithTenantAndStatusCode("/users-keycloak/idp-migrations", tenant, usersIdp, SC_BAD_REQUEST).andReturn();

    verifyKeycloakUserAndWithNoIdentityProviderExisting(tenant, user);
  }

  private void createAndVerifyShadowUserWithoutLinkedIdp() throws Exception {
    createAndVerifyShadowUser(user -> verifyKeycloakUserAndWithNoIdentityProviderExisting(tenant, user));
  }

  private void createAndVerifyShadowUserWithLinkedIdp() throws Exception {
    createAndVerifyShadowUser(user -> verifyKeycloakUserAndIdentityProvider(tenant, user));
  }

  private void createAndVerifyShadowUser(Consumer<User> verificationStep) throws Exception {
    tenant = CENTRAL_TENANT_NAME;
    user = TestConstants.shadowUser();

    var mvcResult = doPostWithTenant("/users-keycloak/users", tenant, user).andReturn();
    var resp = parseResponse(mvcResult, User.class);

    assertSuccessfulUserCreation(resp, user);

    verificationStep.accept(user);
  }

  private UsersIdp createUsersIdp(String centralTenantId, Set<UUID> userIds) {
    return new UsersIdp().centralTenantId(centralTenantId).userIds(userIds);
  }
}
