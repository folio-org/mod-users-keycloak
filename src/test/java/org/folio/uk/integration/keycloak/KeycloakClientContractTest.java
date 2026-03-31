package org.folio.uk.integration.keycloak;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.util.List;
import java.util.UUID;
import org.folio.test.types.UnitTest;
import org.folio.uk.integration.keycloak.model.Client;
import org.folio.uk.integration.keycloak.model.KeycloakUser;
import org.folio.uk.integration.keycloak.model.ScopePermission;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

@UnitTest
class KeycloakClientContractTest {

  private static final String BASE_URL = "https://keycloak.example";
  private static final String REALM = "testtenant";
  private static final String CLIENT_ID = "testtenant-login-application";
  private static final String TOKEN = "Bearer token";

  private MockRestServiceServer mockServer;
  private KeycloakClient keycloakClient;

  @BeforeEach
  void setUp() {
    var restClientBuilder = RestClient.builder().baseUrl(BASE_URL);
    mockServer = MockRestServiceServer.bindTo(restClientBuilder).build();

    var restClient = restClientBuilder.build();
    var adapter = RestClientAdapter.create(restClient);
    keycloakClient = HttpServiceProxyFactory.builderFor(adapter).build().createClient(KeycloakClient.class);
  }

  @Test
  void findClientsByClientId_positive_sendsClientIdAsQueryParam() {
    var id = UUID.randomUUID();
    var response = """
      [{"id":"%s","clientId":"%s"}]
      """.formatted(id, CLIENT_ID);

    mockServer.expect(requestTo(BASE_URL + "/admin/realms/" + REALM + "/clients?clientId=" + CLIENT_ID))
      .andExpect(method(GET))
      .andExpect(header(AUTHORIZATION, TOKEN))
      .andRespond(withSuccess(response, MediaType.APPLICATION_JSON));

    var result = keycloakClient.findClientsByClientId(REALM, CLIENT_ID, TOKEN);

    assertThat(result)
      .singleElement()
      .extracting(Client::getClientId)
      .isEqualTo(CLIENT_ID);
    mockServer.verify();
  }

  @Test
  void httpExchange_positive_expandsQueryTemplateVariablesFromPathVariable() {
    var id = UUID.randomUUID();
    var response = """
      [{"id":"%s","clientId":"%s"}]
      """.formatted(id, CLIENT_ID);

    var restClientBuilder = RestClient.builder().baseUrl(BASE_URL);
    var queryTemplateServer = MockRestServiceServer.bindTo(restClientBuilder).build();
    var queryTemplateClient = HttpServiceProxyFactory.builderFor(RestClientAdapter.create(restClientBuilder.build()))
      .build()
      .createClient(QueryTemplateKeycloakClient.class);

    queryTemplateServer.expect(requestTo(BASE_URL + "/admin/realms/" + REALM + "/clients?clientId=" + CLIENT_ID))
      .andExpect(method(GET))
      .andExpect(header(AUTHORIZATION, TOKEN))
      .andRespond(withSuccess(response, MediaType.APPLICATION_JSON));

    var result = queryTemplateClient.findClientsByClientId(REALM, CLIENT_ID, TOKEN);

    assertThat(result)
      .singleElement()
      .extracting(Client::getClientId)
      .isEqualTo(CLIENT_ID);
    queryTemplateServer.verify();
  }

  @Test
  void httpExchange_positive_supportsLegacyUserSearchQueryTemplate() {
    var response = """
      [{"id":"1","username":"alice"}]
      """;

    var restClientBuilder = RestClient.builder().baseUrl(BASE_URL);
    var queryTemplateServer = MockRestServiceServer.bindTo(restClientBuilder).build();
    var queryTemplateClient = HttpServiceProxyFactory.builderFor(RestClientAdapter.create(restClientBuilder.build()))
      .build()
      .createClient(QueryTemplateUsersClient.class);

    queryTemplateServer.expect(requestTo(BASE_URL + "/admin/realms/" + REALM
      + "/users?exact=true&first=0&max=1&username=alice&briefRepresentation=true"))
      .andExpect(method(GET))
      .andExpect(header(AUTHORIZATION, TOKEN))
      .andRespond(withSuccess(response, MediaType.APPLICATION_JSON));

    var result = queryTemplateClient.findUsersByUsername(REALM, "alice", true, TOKEN);

    assertThat(result)
      .singleElement()
      .extracting(KeycloakUser::getUserName)
      .isEqualTo("alice");
    queryTemplateServer.verify();
  }

  @Test
  void httpExchange_positive_supportsLegacyScopePermissionQueryTemplate() {
    var id = UUID.randomUUID();
    var response = """
      [{"id":"1","name":"Password Reset policy","scopes":[],"policies":[],"resources":[]}]
      """;

    var restClientBuilder = RestClient.builder().baseUrl(BASE_URL);
    var queryTemplateServer = MockRestServiceServer.bindTo(restClientBuilder).build();
    var queryTemplateClient = HttpServiceProxyFactory.builderFor(RestClientAdapter.create(restClientBuilder.build()))
      .build()
      .createClient(QueryTemplateScopePermissionClient.class);

    queryTemplateServer.expect(requestTo(BASE_URL + "/admin/realms/" + REALM + "/clients/" + id
      + "/authz/resource-server/permission/scope?first=0&max=100&name=Password%20Reset%20policy"))
      .andExpect(method(GET))
      .andExpect(header(AUTHORIZATION, TOKEN))
      .andRespond(withSuccess(response, MediaType.APPLICATION_JSON));

    var result = queryTemplateClient.findScopePermission(REALM, id, "Password Reset policy", TOKEN);

    assertThat(result)
      .singleElement()
      .extracting(ScopePermission::getName)
      .isEqualTo("Password Reset policy");
    queryTemplateServer.verify();
  }

  @HttpExchange
  interface QueryTemplateKeycloakClient {

    @GetExchange("/admin/realms/{realmId}/clients?clientId={clientId}")
    List<Client> findClientsByClientId(@PathVariable("realmId") String realmId,
      @PathVariable("clientId") String clientId,
      @RequestHeader(AUTHORIZATION) String token);
  }

  @HttpExchange
  interface QueryTemplateUsersClient {

    @GetExchange("/admin/realms/{realm}/users?exact=true&first=0&max=1")
    List<KeycloakUser> findUsersByUsername(@PathVariable("realm") String realm,
      @RequestParam("username") String username,
      @RequestParam("briefRepresentation") boolean briefRepresentation,
      @RequestHeader(AUTHORIZATION) String token);
  }

  @HttpExchange
  interface QueryTemplateScopePermissionClient {

    @GetExchange("/admin/realms/{realmId}/clients/{clientId}/authz/resource-server/permission/scope?first=0&max=100")
    List<ScopePermission> findScopePermission(@PathVariable("realmId") String realmId,
      @PathVariable("clientId") UUID clientId,
      @RequestParam("name") String name,
      @RequestHeader(AUTHORIZATION) String token);
  }
}
