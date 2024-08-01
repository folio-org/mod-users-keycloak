package org.folio.uk.it;

import static org.folio.uk.support.TestConstants.TENANT_NAME;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;
import org.folio.test.extensions.WireMockStub;
import org.folio.test.types.IntegrationTest;
import org.folio.uk.base.BaseIntegrationTest;
import org.folio.uk.integration.keycloak.model.KeycloakUser;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@IntegrationTest
public class AuthUserIT extends BaseIntegrationTest {

  public static final String KC_USERNAME = "test_kc_username";

  @BeforeAll
  static void beforeAll() {
    enableTenant(TENANT_NAME);
  }

  @AfterAll
  static void afterAll() {
    removeTenant(TENANT_NAME);
  }

  @BeforeEach
  public void beforeEach() {
    var token = tokenService.issueToken();
    keycloakClient.findUsersByUsername(TENANT_NAME, KC_USERNAME, true, token)
      .forEach(user -> keycloakClient.deleteUser(TENANT_NAME, user.getId(), token));
  }

  @Test
  void verify_negative_nonExistingKeycloakUser() throws Exception {
    mockMvc.perform(get("/users-keycloak/auth-users/d3958402-2f80-421b-a527-9933245a3556").headers(okapiHeaders())
      .contentType(APPLICATION_JSON)).andExpectAll(status().isNotFound());
  }

  @Test
  void verify_positive_existingKeycloakUser() throws Exception {
    createKeycloakUser();
    mockMvc.perform(get("/users-keycloak/auth-users/d3958402-2f80-421b-a527-9933245a3556").headers(okapiHeaders())
      .contentType(APPLICATION_JSON)).andExpectAll(status().isNoContent());
  }

  @Test
  @WireMockStub(scripts = {"/wiremock/stubs/users/get-user.json"})
  void createKeycloakUser_positive_noKeycloakUser() throws Exception {
    mockMvc.perform(post("/users-keycloak/auth-users/d3958402-2f80-421b-a527-9933245a3556").headers(okapiHeaders())
      .contentType(APPLICATION_JSON)).andExpectAll(status().isCreated());
  }

  @Test
  @WireMockStub(scripts = {"/wiremock/stubs/users/get-user.json"})
  void createKeycloakUser_positive_keycloakUserAlreadyExists() throws Exception {
    createKeycloakUser();
    mockMvc.perform(post("/users-keycloak/auth-users/d3958402-2f80-421b-a527-9933245a3556").headers(okapiHeaders())
      .contentType(APPLICATION_JSON)).andExpectAll(status().isNoContent());
  }

  @Test
  @WireMockStub(scripts = {"/wiremock/stubs/users/get-user-no-username.json"})
  void createKeycloakUser_negative_userWithoutUsername() throws Exception {
    mockMvc.perform(post("/users-keycloak/auth-users/d3958402-2f80-421b-a527-9933245a3556").headers(okapiHeaders())
      .contentType(APPLICATION_JSON)).andExpectAll(status().isBadRequest(),
      jsonPath("$.errors[0].message", containsString("User without username cannot be created in Keycloak")),
      jsonPath("$.errors[0].code", is("user.absent-username")),
      jsonPath("$.errors[0].type", is("RequestValidationException")));
  }

  private void createKeycloakUser() {
    keycloakClient.createUser(TENANT_NAME, KeycloakUser.builder().userName(KC_USERNAME)
        .attributes(Map.of(KeycloakUser.USER_ID_ATTR, List.of("d3958402-2f80-421b-a527-9933245a3556"))).build(),
      tokenService.issueToken());
  }
}
