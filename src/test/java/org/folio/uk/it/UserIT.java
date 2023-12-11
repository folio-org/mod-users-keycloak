package org.folio.uk.it;

import static org.assertj.core.api.Assertions.assertThat;
import static org.folio.spring.integration.XOkapiHeaders.TENANT;
import static org.folio.test.TestConstants.TENANT_ID;
import static org.folio.test.TestUtils.parseResponse;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;
import org.folio.spring.integration.XOkapiHeaders;
import org.folio.test.extensions.WireMockStub;
import org.folio.test.types.IntegrationTest;
import org.folio.uk.base.BaseIntegrationTest;
import org.folio.uk.domain.dto.CompositeUser;
import org.folio.uk.domain.dto.User;
import org.folio.uk.exception.RequestValidationException;
import org.folio.uk.support.TestConstants;
import org.junit.jupiter.api.Test;

@IntegrationTest
@SuppressWarnings("squid:S2699")
class UserIT extends BaseIntegrationTest {
  private static final String TOKEN_WITH_USER_ID =
    "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJkaWt1X2FkbWluIiwidHlwZSI6ImxlZ2FjeS1hY2Nlc3MiLCJ1c2VyX"
      + "2lkIjoiYWM2NzY3YzMtMTMyNS01MjA1LTg5ZDUtYzU3OGM5OTFhNDVhIiwiaWF0IjoxNjg4NzAzNTM3LCJ0ZW5hbnQiOiJkaWt1In0"
      + ".9MDZuCHjKcG4YTHtGsnaYNej5ZqBRm7Wo-OcvPfTGU4";
  private static final UUID USER_ID_FROM_TOKEN = UUID.fromString("ac6767c3-1325-5205-89d5-c578c991a45a");

  @Test
  @WireMockStub(scripts = "/wiremock/stubs/users/get-user.json")
  void get_positive() throws Exception {
    doGet("/users-keycloak/users/{id}", TestConstants.USER_ID)
      .andExpect(json("user/get-user-response.json"));
  }

  @Test
  @WireMockStub(scripts = "/wiremock/stubs/users/get-user-notfound.json")
  void get_negative_notFoundById() throws Exception {
    UUID userId = UUID.randomUUID();

    attemptGet("/users-keycloak/users/{id}", userId)
      .andExpectAll(notFoundWithMsg("Not Found"));
  }

  @Test
  @WireMockStub(scripts = {
    "/wiremock/stubs/users/get-user-by-self.json",
    "/wiremock/stubs/users/get-perms-by-self.json",
    "/wiremock/stubs/inventory/get-service-points-user.json",
    "/wiremock/stubs/inventory/get-service-point.json"
  })
  void getBySelf_positive() throws Exception {
    var response = mockMvc.perform(get("/users-keycloak/_self?include=groups")
        .header(XOkapiHeaders.URL, wmAdminClient.getWireMockUrl())
        .header(TENANT, TENANT_ID)
        .header(XOkapiHeaders.TOKEN, TOKEN_WITH_USER_ID)
        .header(XOkapiHeaders.USER_ID, USER_ID_FROM_TOKEN)
        .contentType(APPLICATION_JSON))
      .andExpect(status().isOk())
      .andReturn();

    var user = parseResponse(response, CompositeUser.class);
    assertThat(user.getUser().getId()).isEqualTo(USER_ID_FROM_TOKEN);
    assertThat(user.getPermissions().getPermissions()).isEqualTo(List.of("ui.all"));
    assertThat(user.getServicePointsUser().getId()).isEqualTo("e66e30fd-0473-4a3f-910b-7921817eb3ea");
    assertThat(user.getServicePointsUser().getServicePoints().get(0).getId()).isEqualTo(
      "7c5abc9f-f3d7-4856-b8d7-6712462ca007");
  }

  @Test
  @WireMockStub(scripts = {
    "/wiremock/stubs/users/get-user-by-self.json",
    "/wiremock/stubs/users/get-perms-by-self.json",
    "/wiremock/stubs/inventory/get-service-points-user-not-found.json"
  })
  void getBySelf_positive_no_servicePointsUsers() throws Exception {
    var response = mockMvc.perform(get("/users-keycloak/_self?include=groups")
        .header(XOkapiHeaders.URL, wmAdminClient.getWireMockUrl())
        .header(TENANT, TENANT_ID)
        .header(XOkapiHeaders.TOKEN, TOKEN_WITH_USER_ID)
        .header(XOkapiHeaders.USER_ID, USER_ID_FROM_TOKEN)
        .contentType(APPLICATION_JSON))
      .andExpect(status().isOk())
      .andReturn();

    var user = parseResponse(response, CompositeUser.class);
    assertThat(user.getUser().getId()).isEqualTo(USER_ID_FROM_TOKEN);
    assertThat(user.getPermissions().getPermissions()).isEqualTo(List.of("ui.all"));
    assertThat(user.getServicePointsUser()).isNull();
  }

  @Test
  @WireMockStub(scripts = {
    "/wiremock/stubs/users/get-user-by-self.json",
    "/wiremock/stubs/users/get-perms-by-self.json",
    "/wiremock/stubs/inventory/get-service-points-user-error.json"
  })
  void getBySelf_positive_error_servicePointsUsers() throws Exception {
    var response = mockMvc.perform(get("/users-keycloak/_self?include=groups")
        .header(XOkapiHeaders.URL, wmAdminClient.getWireMockUrl())
        .header(TENANT, TENANT_ID)
        .header(XOkapiHeaders.TOKEN, TOKEN_WITH_USER_ID)
        .header(XOkapiHeaders.USER_ID, USER_ID_FROM_TOKEN)
        .contentType(APPLICATION_JSON))
      .andExpect(status().isOk())
      .andReturn();

    var user = parseResponse(response, CompositeUser.class);
    assertThat(user.getUser().getId()).isEqualTo(USER_ID_FROM_TOKEN);
    assertThat(user.getPermissions().getPermissions()).isEqualTo(List.of("ui.all"));
    assertThat(user.getServicePointsUser()).isNull();
  }

  @Test
  @WireMockStub(scripts = {
    "/wiremock/stubs/users/get-user-by-self.json",
    "/wiremock/stubs/users/get-perms-by-self.json",
    "/wiremock/stubs/inventory/get-service-points-user.json",
    "/wiremock/stubs/inventory/get-service-point-notfound.json"
  })
  void getBySelf_positive_no_servicePoint() throws Exception {
    var response = mockMvc.perform(get("/users-keycloak/_self?include=groups")
        .header(XOkapiHeaders.URL, wmAdminClient.getWireMockUrl())
        .header(TENANT, TENANT_ID)
        .header(XOkapiHeaders.TOKEN, TOKEN_WITH_USER_ID)
        .header(XOkapiHeaders.USER_ID, USER_ID_FROM_TOKEN)
        .contentType(APPLICATION_JSON))
      .andExpect(status().isOk())
      .andReturn();

    var user = parseResponse(response, CompositeUser.class);
    assertThat(user.getUser().getId()).isEqualTo(USER_ID_FROM_TOKEN);
    assertThat(user.getPermissions().getPermissions()).isEqualTo(List.of("ui.all"));
    assertThat(user.getServicePointsUser().getId()).isEqualTo("e66e30fd-0473-4a3f-910b-7921817eb3ea");
    assertThat(user.getServicePointsUser().getServicePoints()).isEmpty();
  }

  @Test
  @WireMockStub(scripts = {
    "/wiremock/stubs/users/get-user-by-self.json",
    "/wiremock/stubs/users/get-perms-by-self.json",
    "/wiremock/stubs/inventory/get-service-points-user.json",
    "/wiremock/stubs/inventory/get-service-point.json"
  })
  void getBySelf_positive_onlyHeader() throws Exception {
    var response = mockMvc.perform(get("/users-keycloak/_self?include=groups")
        .header(XOkapiHeaders.URL, wmAdminClient.getWireMockUrl())
        .header(TENANT, TENANT_ID)
        .header(XOkapiHeaders.TOKEN, "TOKEN WITH NO USER ID")
        .header(XOkapiHeaders.USER_ID, USER_ID_FROM_TOKEN)
        .contentType(APPLICATION_JSON))
      .andExpect(status().isOk())
      .andReturn();

    var user = parseResponse(response, CompositeUser.class);
    assertThat(user.getUser().getId()).isEqualTo(USER_ID_FROM_TOKEN);
    assertThat(user.getPermissions().getPermissions()).isEqualTo(List.of("ui.all"));
  }

  @Test
  @WireMockStub(scripts = {
    "/wiremock/stubs/users/get-user-by-self.json",
    "/wiremock/stubs/users/get-perms-by-self.json",
    "/wiremock/stubs/inventory/get-service-points-user.json",
    "/wiremock/stubs/inventory/get-service-point.json"
  })
  void getBySelf_positive_onlyToken() throws Exception {
    var response = mockMvc.perform(get("/users-keycloak/_self?include=groups")
        .header(XOkapiHeaders.URL, wmAdminClient.getWireMockUrl())
        .header(TENANT, TENANT_ID)
        .header(XOkapiHeaders.TOKEN, TOKEN_WITH_USER_ID)
        .contentType(APPLICATION_JSON))
      .andExpect(status().isOk())
      .andReturn();

    var user = parseResponse(response, CompositeUser.class);
    assertThat(user.getUser().getId()).isEqualTo(USER_ID_FROM_TOKEN);
    assertThat(user.getPermissions().getPermissions()).isEqualTo(List.of("ui.all"));
  }

  @Test
  void getBySelf_negative() throws Exception {
    mockMvc.perform(get("/users-keycloak/_self?include=groups")
        .header(XOkapiHeaders.URL, wmAdminClient.getWireMockUrl())
        .header(TENANT, TENANT_ID)
        .contentType(APPLICATION_JSON))
      .andExpect(status().isNotFound());
  }

  @Test
  @WireMockStub(scripts = {
    "/wiremock/stubs/users/create-user.json"
  })
  void create_positive() throws Exception {
    var user = TestConstants.user();
    var mvcResult = doPost("/users-keycloak/users", user).andReturn();
    var resp = parseResponse(mvcResult, User.class);

    assertThat(resp.getId()).isEqualTo(user.getId());
    assertThat(resp.getUsername()).isEqualTo(user.getUsername());
    assertThat(resp.getBarcode()).isEqualTo(user.getBarcode());
    assertThat(resp.getPatronGroup()).isEqualTo(user.getPatronGroup());

    assertThat(resp.getPersonal().getFirstName()).isEqualTo(user.getPersonal().getFirstName());
    assertThat(resp.getPersonal().getLastName()).isEqualTo(user.getPersonal().getLastName());
    assertThat(resp.getPersonal().getEmail()).isEqualTo(user.getPersonal().getEmail());

    assertThat(resp.getMetadata()).isNotNull();
  }

  @Test
  void create_positive_keycloakOnly() throws Exception {
    var user = TestConstants.user(UUID.randomUUID().toString(), "keycloakOnlyUser", "kc@mail.com");
    var mvcResult = doPost("/users-keycloak/users?keycloakOnly=true", user).andReturn();
    var resp = parseResponse(mvcResult, User.class);

    assertThat(resp.getId()).isEqualTo(user.getId());
    assertThat(resp.getUsername()).isEqualTo(user.getUsername());
    assertThat(resp.getBarcode()).isEqualTo(user.getBarcode());
    assertThat(resp.getPatronGroup()).isEqualTo(user.getPatronGroup());

    assertThat(resp.getPersonal().getFirstName()).isEqualTo(user.getPersonal().getFirstName());
    assertThat(resp.getPersonal().getLastName()).isEqualTo(user.getPersonal().getLastName());
    assertThat(resp.getPersonal().getEmail()).isEqualTo(user.getPersonal().getEmail());
  }

  @Test
  void create_negative_badRequest() throws Exception {
    mockMvc.perform(post("/users-keycloak/users")
      .headers(okapiHeaders())
      .content(readTemplate("user/create-user-no-lastname-request.json"))
      .contentType(APPLICATION_JSON)
    ).andExpectAll(argumentNotValidErr("must not be null", "personal.lastName", null));
  }

  @Test
  @WireMockStub(scripts = "/wiremock/stubs/users/create-user-patron-group-not-found.json")
  void create_negative_patronGroupNotFound() throws Exception {
    mockMvc.perform(post("/users-keycloak/users")
      .headers(okapiHeaders())
      .content(readTemplate("user/create-user-unknown-group-request.json"))
      .contentType(APPLICATION_JSON)
    ).andExpectAll(status().isBadRequest(),
      jsonPath("$.errors[0].message", containsString("Patron group not found")),
      jsonPath("$.errors[0].code", is("validation_error")),
      jsonPath("$.errors[0].type", is("BadRequest")),
      jsonPath("$.total_records", is(1)));
  }

  @Test
  @WireMockStub(scripts = {
    "/wiremock/stubs/users/create-user-auth-exist.json"
  })
  void create_positive_kc_only_authUserExists() throws Exception {
    var userId = "d24b7b4a-00ed-416e-a810-981b003bd158";
    var user = TestConstants.user(userId, "create-user-auth-exist", "au_exist@mail.com");
    doPost("/users-keycloak/users?keycloakOnly=true", user);

    mockMvc.perform(post("/users-keycloak/users")
      .headers(okapiHeaders())
      .content(readTemplate("user/create-user-request.json"))
      .contentType(APPLICATION_JSON)
    ).andExpectAll(status().isCreated(),
      jsonPath("$.username", is("create-user-auth-exist")));
  }

  @Test
  @WireMockStub(scripts = {
    "/wiremock/stubs/users/create-user-auth-exist.json"
  })
  void create_positive_authUserExists() throws Exception {
    var userId = "d24b7b4a-00ed-416e-a810-981b003bd158";
    var user = TestConstants.user(userId, "create-user-auth-exist", "au_exist@mail.com");
    doPost("/users-keycloak/users?keycloakOnly=false", user);

    mockMvc.perform(post("/users-keycloak/users")
      .headers(okapiHeaders())
      .content(readTemplate("user/create-user-request.json"))
      .contentType(APPLICATION_JSON)
    ).andExpectAll(status().isCreated(),
      jsonPath("$.username", is("create-user-auth-exist")));
  }

  @Test
  void create_negative_keycloakOnlyAndNoUserId() throws Exception {
    mockMvc.perform(post("/users-keycloak/users?keycloakOnly=true")
      .headers(okapiHeaders())
      .content(readTemplate("user/create-user-no-id-request.json"))
      .contentType(APPLICATION_JSON)
    ).andExpectAll(validationErr(RequestValidationException.class.getSimpleName(), "User id is missing", "id", null));
  }

  @Test
  @WireMockStub(scripts = {
    "/wiremock/stubs/users/update-user.json",
  })
  void update_positive() throws Exception {
    var userId = "202a8ef0-d07b-4626-ad41-48c2d50d9099";
    var user = TestConstants.user(userId, "update-user", "uu@mail.com");
    doPost("/users-keycloak/users?keycloakOnly=true", user);
    doPut("/users-keycloak/users/{id}", user, userId);
  }

  @Test
  @WireMockStub(scripts = "/wiremock/stubs/users/update-user-notfound.json")
  void update_negative_userNotFound() throws Exception {
    var user = TestConstants.user("f4b05750-bd6c-427b-a651-7ba2191d24a3", "update-user-notfound", "uunf@mail.com");
    attemptPut("/users-keycloak/users/{id}", user, user.getId())
      .andExpectAll(notFoundWithMsg("Not found"));
  }

  @Test
  @WireMockStub(scripts = "/wiremock/stubs/users/update-user-id-notmatch.json")
  void update_negative_userIdDoesntMatch() throws Exception {
    attemptPut("/users-keycloak/users/{id}", TestConstants.user(), UUID.randomUUID())
      .andExpectAll(status().isBadRequest(),
        jsonPath("$.errors[0].message", containsString("You cannot change the value of the id field")),
        jsonPath("$.errors[0].code", is("validation_error")),
        jsonPath("$.errors[0].type", is("BadRequest")),
        jsonPath("$.total_records", is(1)));
  }

  @Test
  @WireMockStub(scripts = {
    "/wiremock/stubs/users/update-user-no-auth.json"
  })
  void update_negative_authUserNotFound() throws Exception {
    var userId = "95a4a5c0-ca68-4d26-af83-62f5c3586f18";
    var user = TestConstants.user(userId, "update-user-no-auth", "uuna@mail.com");
    attemptPut("/users-keycloak/users/{id}", user, userId)
      .andExpectAll(status().isBadRequest(),
        jsonPath("$.errors[0].message",
          containsString(
            String.format("Failed to update keycloak user: userId = %s, realm = %s", userId,
              TestConstants.TENANT_NAME))),
        jsonPath("$.errors[0].code", is("service_error")),
        jsonPath("$.errors[0].type", is("KeycloakException")),
        jsonPath("$.errors[0].parameters[0].value", containsString("Keycloak user doesn't exist")),
        jsonPath("$.total_records", is(1)));
  }

  @Test
  @WireMockStub(scripts = {
    "/wiremock/stubs/users/delete-user.json",
    "/wiremock/stubs/users/get-user.json"
  })
  void delete_positive() throws Exception {
    var user = TestConstants.user("d3958402-2f80-421b-a527-9933245a3556", "delete-user", "du@mail.com");
    doPost("/users-keycloak/users?keycloakOnly=true", user);
    doDelete("/users-keycloak/users/{id}", user.getId());
  }

  @Test
  @WireMockStub(scripts = {
    "/wiremock/stubs/users/delete-user.json",
    "/wiremock/stubs/users/get-user.json"
  })
  void delete_positive_noAuthUser() throws Exception {
    doDelete("/users-keycloak/users/{id}", TestConstants.USER_ID);
  }

  @Test
  @WireMockStub(scripts = {
    "/wiremock/stubs/users/get-user-notfound.json"
  })
  void delete_positive_noUser() throws Exception {
    var user = TestConstants.user("d3958402-2f80-421b-a527-9933245a3556", "delete-user", "du@mail.com");
    doPost("/users-keycloak/users?keycloakOnly=true", user);
    doDelete("/users-keycloak/users/{id}", user.getId());
  }
}
