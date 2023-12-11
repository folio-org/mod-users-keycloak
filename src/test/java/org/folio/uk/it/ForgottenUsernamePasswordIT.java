package org.folio.uk.it;

import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import org.folio.test.extensions.WireMockStub;
import org.folio.test.types.IntegrationTest;
import org.folio.uk.base.BaseIntegrationTest;
import org.folio.uk.base.KeycloakTestClient;
import org.folio.uk.domain.dto.Identifier;
import org.folio.uk.integration.keycloak.TokenService;
import org.folio.uk.service.PasswordResetService;
import org.folio.uk.support.TestConstants;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

@IntegrationTest
public class ForgottenUsernamePasswordIT extends BaseIntegrationTest {

  @MockBean private PasswordResetService passwordResetService;

  @BeforeAll
  static void beforeAll(@Autowired KeycloakTestClient client, @Autowired TokenService tokenService) {
    enableTenant(TestConstants.TENANT_NAME, tokenService, client);
  }

  @AfterAll
  static void afterAll() {
    removeTenant(TestConstants.TENANT_NAME);
  }

  @Test
  @WireMockStub(scripts = "/wiremock/stubs/users/query-test1-user.json")
  @WireMockStub(scripts = "/wiremock/stubs/config/get-forgottenData-configs.json")
  public void postResetForgottenPassword_positive() throws Exception {
    attemptPost("/users-keycloak/forgotten/password", new Identifier().id("test1"))
      .andExpect(status().isNoContent());

    verify(passwordResetService).sendPasswordRestLink(UUID.fromString("d3958402-2f80-421b-a527-9933245a3556"));
  }

  @Test
  @WireMockStub(scripts = "/wiremock/stubs/users/query-users-notfound.json")
  @WireMockStub(scripts = "/wiremock/stubs/config/get-forgottenData-configs.json")
  public void postResetForgottenPassword_negative_notFoundUser() throws Exception {
    attemptPost("/users-keycloak/forgotten/password", new Identifier().id("unknownuser"))
      .andExpect(status().isBadRequest())
      .andExpect(jsonPath("$.total_records", is(1)))
      .andExpect(jsonPath("$.errors[0].message", is("User is not found: unknownuser")));

    verifyNoInteractions(passwordResetService);
  }

  @Test
  @WireMockStub(scripts = "/wiremock/stubs/users/query-user-multiplefound.json")
  @WireMockStub(scripts = "/wiremock/stubs/config/get-forgottenData-configs.json")
  public void postResetForgottenPassword_negative_multipleUsersFound() throws Exception {
    attemptPost("/users-keycloak/forgotten/password", new Identifier().id("twin1"))
      .andExpect(status().isUnprocessableEntity())
      .andExpect(jsonPath("$.total_records", is(1)))
      .andExpect(jsonPath("$.errors[0].message", is("Multiple users associated with 'twin1'")))
      .andExpect(jsonPath("$.errors[0].code", is("forgotten.password.found.multiple.users")));

    verifyNoInteractions(passwordResetService);
  }

  @Test
  @WireMockStub(scripts = "/wiremock/stubs/users/query-inactive-user.json")
  @WireMockStub(scripts = "/wiremock/stubs/config/get-forgottenData-configs.json")
  public void postResetForgottenPassword_negative_inactiveUser() throws Exception {
    attemptPost("/users-keycloak/forgotten/password", new Identifier().id("userinactive"))
      .andExpect(status().isUnprocessableEntity())
      .andExpect(jsonPath("$.total_records", is(1)))
      .andExpect(jsonPath("$.errors[0].message", is("Users associated with 'userinactive' is not active")))
      .andExpect(jsonPath("$.errors[0].code", is("forgotten.password.found.inactive")));
  }

  @Test
  @WireMockStub(scripts = "/wiremock/stubs/notify/send-forgotten-username-notification.json")
  @WireMockStub(scripts = "/wiremock/stubs/users/query-test1-user.json")
  @WireMockStub(scripts = "/wiremock/stubs/config/get-forgottenData-configs.json")
  public void postRecoverForgottenUsername_positive() throws Exception {
    attemptPost("/users-keycloak/forgotten/username", new Identifier().id("test1"))
      .andExpect(status().isNoContent());
  }

  @Test
  @WireMockStub(scripts = "/wiremock/stubs/users/query-user-multiplefound.json")
  @WireMockStub(scripts = "/wiremock/stubs/config/get-forgottenData-configs.json")
  public void postRecoverForgottenUsername_negative_multipleUsersFound() throws Exception {
    attemptPost("/users-keycloak/forgotten/username", new Identifier().id("twin1"))
      .andExpect(status().isUnprocessableEntity())
      .andExpect(jsonPath("$.total_records", is(1)))
      .andExpect(jsonPath("$.errors[0].message", is("Multiple users associated with 'twin1'")))
      .andExpect(jsonPath("$.errors[0].code", is("forgotten.username.found.multiple.users")));
  }

  @Test
  @WireMockStub(scripts = "/wiremock/stubs/users/query-inactive-user.json")
  @WireMockStub(scripts = "/wiremock/stubs/config/get-forgottenData-configs.json")
  public void postRecoverForgottenUsername_negative_inactiveUser() throws Exception {
    attemptPost("/users-keycloak/forgotten/username", new Identifier().id("userinactive"))
      .andExpect(status().isUnprocessableEntity())
      .andExpect(jsonPath("$.total_records", is(1)))
      .andExpect(jsonPath("$.errors[0].message", is("Users associated with 'userinactive' is not active")))
      .andExpect(jsonPath("$.errors[0].code", is("forgotten.password.found.inactive")));
  }
}
