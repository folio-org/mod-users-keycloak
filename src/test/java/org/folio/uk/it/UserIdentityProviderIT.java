package org.folio.uk.it;

import static org.assertj.core.api.Assertions.assertThat;
import static org.folio.test.TestUtils.asJsonString;
import static org.folio.test.TestUtils.parseResponse;
import static org.folio.uk.support.TestConstants.CENTRAL_TENANT_NAME;
import static org.folio.uk.support.TestConstants.TENANT_NAME;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import java.util.List;
import java.util.Objects;
import org.folio.test.extensions.WireMockStub;
import org.folio.test.types.IntegrationTest;
import org.folio.uk.base.BaseIntegrationTest;
import org.folio.uk.domain.dto.User;
import org.folio.uk.exception.RequestValidationException;
import org.folio.uk.integration.keycloak.config.KeycloakFederatedAuthProperties;
import org.folio.uk.integration.keycloak.model.FederatedIdentity;
import org.folio.uk.integration.keycloak.model.KeycloakUser;
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

  private User user;
  private KeycloakUser kcUser;

  @BeforeAll
  static void beforeAll() {
    enableTenant(CENTRAL_TENANT_NAME);
    enableTenant(TENANT_NAME);
  }

  @AfterAll
  static void afterAll() {
    // removeTenant(CENTRAL_TENANT_NAME);
    removeTenant(TENANT_NAME);
  }

  @BeforeEach
  void beforeEach() {
    keycloakFederatedAuthProperties.setEnabled(true);
    createIdentityProviderInCentralTenant();
    user = TestConstants.user();
    kcUser = createShadowKeycloakUserInCentralTenant(user);
  }

  @AfterEach
  void afterEach() {
    if (Objects.nonNull(kcUser)) {
      removeShadowKeycloakUserInCentralTenant(kcUser.getId());
    }
    removeIdentityProviderInCentralTenant();
  }

  @Test
  @WireMockStub({
    "/wiremock/stubs/users/create-user.json",
    "/wiremock/stubs/users/get-user-tenants.json"
  })
  void create_positive() throws Exception {
    var mvcResult = doPostWithTenant("/users-keycloak/users", TENANT_NAME, user).andReturn();
    var resp = parseResponse(mvcResult, User.class);

    assertSuccessfulUserCreation(resp, user);

    verifyKeycloakUserAndIdentityProvider(TENANT_NAME, user, kcUser.getId());
  }

  @Test
  @WireMockStub({
    "/wiremock/stubs/users/create-user.json",
    "/wiremock/stubs/users/get-user-tenants.json"
  })
  void create_positive_identityProviderAlreadyLinked() throws Exception {
    var federatedIdentity = FederatedIdentity.builder()
      .userId(user.getUsername())
      .userName(user.getUsername())
      .build();
    keycloakClient.linkIdentityProviderToUser(CENTRAL_TENANT_NAME, kcUser.getId(), PROVIDER_ALIAS, federatedIdentity,
      tokenService.issueToken());

    var mvcResult = doPostWithTenant("/users-keycloak/users", TENANT_NAME, user).andReturn();
    var resp = parseResponse(mvcResult, User.class);

    assertSuccessfulUserCreation(resp, user);

    verifyKeycloakUserAndIdentityProvider(TENANT_NAME, user, kcUser.getId());
  }

  @Test
  @WireMockStub({
    "/wiremock/stubs/users/create-user.json",
    "/wiremock/stubs/users/get-empty-user-tenants.json",
  })
  void create_positive_emptyUserTenants() throws Exception {
    var mvcResult = doPostWithTenant("/users-keycloak/users", TENANT_NAME, user).andReturn();
    var resp = parseResponse(mvcResult, User.class);

    assertSuccessfulUserCreation(resp, user);

    verifyKeycloakUserAndWithNoIdentityProvider(TENANT_NAME, user);
  }

  @Test
  @WireMockStub({
    "/wiremock/stubs/users/create-user.json",
    "/wiremock/stubs/users/get-empty-central-tenant-user-tenants.json",
  })
  void create_positive_emptyCentralTenantUserTenants() throws Exception {
    var mvcResult = doPostWithTenant("/users-keycloak/users", TENANT_NAME, user).andReturn();
    var resp = parseResponse(mvcResult, User.class);

    assertSuccessfulUserCreation(resp, user);

    verifyKeycloakUserAndWithNoIdentityProvider(TENANT_NAME, user);
  }

  @Test
  @WireMockStub({
    "/wiremock/stubs/users/create-user-central.json",
    "/wiremock/stubs/users/get-user-tenants-central.json",
  })
  void create_positive_asCentralTenant() throws Exception {
    var mvcResult = doPostWithTenant("/users-keycloak/users", CENTRAL_TENANT_NAME, user).andReturn();
    var resp = parseResponse(mvcResult, User.class);

    assertSuccessfulUserCreation(resp, user);

    verifyKeycloakUserAndWithNoIdentityProvider(CENTRAL_TENANT_NAME, user);
  }

  @Test
  @WireMockStub({
    "/wiremock/stubs/users/create-user.json",
    "/wiremock/stubs/users/get-user-tenants.json"
  })
  void create_negative_noShadowKeycloakUser() throws Exception {
    removeShadowKeycloakUserInCentralTenant(kcUser.getId());
    kcUser = null;
    var mvcResult = mockMvc.perform(post("/users-keycloak/users", List.of())
      .headers(okapiHeadersWithTenant(TENANT_NAME))
      .content(asJsonString(user))
      .contentType(APPLICATION_JSON));

    mvcResult.andExpectAll(validationErr(RequestValidationException.class.getSimpleName(),
      "Shadow keycloak user is missing", "userId", user.getId()));
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
