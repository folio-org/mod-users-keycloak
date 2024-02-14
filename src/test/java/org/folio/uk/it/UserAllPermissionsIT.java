package org.folio.uk.it;

import static org.folio.spring.integration.XOkapiHeaders.TENANT;
import static org.folio.test.TestConstants.TENANT_ID;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;
import org.folio.spring.integration.XOkapiHeaders;
import org.folio.test.extensions.WireMockStub;
import org.folio.test.types.IntegrationTest;
import org.folio.uk.base.BaseIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;

@IntegrationTest
@TestPropertySource(properties = "application.mod-roles-keycloak.include-only-visible-permissions=false")
class UserAllPermissionsIT extends BaseIntegrationTest {
  private static final String TOKEN_WITH_USER_ID =
    "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJkaWt1X2FkbWluIiwidHlwZSI6ImxlZ2FjeS1hY2Nlc3MiLCJ1c2VyX"
      + "2lkIjoiYWM2NzY3YzMtMTMyNS01MjA1LTg5ZDUtYzU3OGM5OTFhNDVhIiwiaWF0IjoxNjg4NzAzNTM3LCJ0ZW5hbnQiOiJkaWt1In0"
      + ".9MDZuCHjKcG4YTHtGsnaYNej5ZqBRm7Wo-OcvPfTGU4";
  private static final UUID USER_ID_FROM_TOKEN = UUID.fromString("ac6767c3-1325-5205-89d5-c578c991a45a");

  @Test
  @WireMockStub(scripts = {
    "/wiremock/stubs/users/get-user-by-self.json",
    "/wiremock/stubs/users/get-all-perms-by-self.json",
    "/wiremock/stubs/inventory/get-service-points-user.json",
    "/wiremock/stubs/inventory/get-service-point.json"
  })
  void getBySelf_positive() throws Exception {
    mockMvc.perform(get("/users-keycloak/_self?include=groups")
        .header(XOkapiHeaders.URL, wmAdminClient.getWireMockUrl())
        .header(TENANT, TENANT_ID)
        .header(XOkapiHeaders.TOKEN, TOKEN_WITH_USER_ID)
        .header(XOkapiHeaders.USER_ID, USER_ID_FROM_TOKEN)
        .contentType(APPLICATION_JSON))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.user.id", is(USER_ID_FROM_TOKEN.toString())))
      .andExpect(jsonPath("$.permissions.permissions", is(List.of("ui.all", "be.all"))))
      .andExpect(jsonPath("$.servicePointsUser.id", is("e66e30fd-0473-4a3f-910b-7921817eb3ea")))
      .andExpect(jsonPath("$.servicePointsUser.servicePoints[*].id", contains("7c5abc9f-f3d7-4856-b8d7-6712462ca007")));
  }
}
