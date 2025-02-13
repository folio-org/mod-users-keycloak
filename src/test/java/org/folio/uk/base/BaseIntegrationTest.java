package org.folio.uk.base;

import static net.minidev.json.JSONValue.parse;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.folio.spring.integration.XOkapiHeaders.TENANT;
import static org.folio.spring.integration.XOkapiHeaders.URL;
import static org.folio.test.TestUtils.asJsonString;
import static org.folio.test.TestUtils.readString;
import static org.folio.test.extensions.impl.KeycloakContainerExtension.getKeycloakAdminClient;
import static org.folio.uk.integration.keycloak.model.KeycloakUser.USER_EXTERNAL_SYSTEM_ID_ATTR;
import static org.folio.uk.integration.keycloak.model.KeycloakUser.USER_ID_ATTR;
import static org.folio.uk.support.TestConstants.CENTRAL_TENANT_NAME;
import static org.folio.uk.support.TestConstants.MODULE_NAME;
import static org.folio.uk.support.TestConstants.OKAPI_AUTH_TOKEN;
import static org.folio.uk.support.TestConstants.TENANT_NAME;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.annotation.DirtiesContext.ClassMode.AFTER_CLASS;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import feign.FeignException;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.apache.kafka.clients.admin.NewTopic;
import org.folio.spring.integration.XOkapiHeaders;
import org.folio.tenant.domain.dto.TenantAttributes;
import org.folio.test.base.BaseBackendIntegrationTest;
import org.folio.test.extensions.EnableKafka;
import org.folio.test.extensions.EnableKeycloakTlsMode;
import org.folio.test.extensions.EnablePostgres;
import org.folio.test.extensions.EnableWireMock;
import org.folio.test.extensions.impl.KafkaTestExecutionListener;
import org.folio.test.extensions.impl.KeycloakExecutionListener;
import org.folio.test.extensions.impl.WireMockAdminClient;
import org.folio.test.extensions.impl.WireMockExecutionListener;
import org.folio.uk.domain.dto.User;
import org.folio.uk.integration.keycloak.KeycloakClient;
import org.folio.uk.integration.keycloak.KeycloakService;
import org.folio.uk.integration.keycloak.TokenService;
import org.folio.uk.integration.keycloak.model.IdentityProvider;
import org.folio.uk.integration.keycloak.model.KeycloakUser;
import org.folio.uk.it.CreateUserVerifyDto;
import org.folio.uk.support.TestValues;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;
import org.keycloak.representations.idm.RealmRepresentation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.web.servlet.ResultActions;

@Log4j2
@EnableKafka
@EnableWireMock
@EnableKeycloakTlsMode
@EnablePostgres
@EnableRetry
@EnableAsync
@SpringBootTest
@ActiveProfiles("it")
@AutoConfigureMockMvc
@DirtiesContext(classMode = AFTER_CLASS)
@Import(BaseIntegrationTest.TopicConfiguration.class)
@TestExecutionListeners(listeners =
  {WireMockExecutionListener.class, KeycloakExecutionListener.class, KafkaTestExecutionListener.class},
  mergeMode = TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS)
public abstract class BaseIntegrationTest extends BaseBackendIntegrationTest {

  public static final String FOLIO_SYSTEM_USER_TOPIC = "it-test.master.mgr-tenant-entitlements.system-user";
  public static final String PROVIDER_ALIAS = String.format("%s-keycloak-oidc", TENANT_NAME);

  protected static WireMockAdminClient wmAdminClient;

  @Autowired protected CacheManager cacheManager;
  @Autowired protected KeycloakClient keycloakClient;
  @Autowired protected TokenService tokenService;
  @Autowired protected KeycloakService keycloakService;

  @BeforeEach
  void setUp(TestInfo testInfo) {
    log.info("Running test: {}", testInfo.getDisplayName());
    evictAllCaches();
  }

  @AfterEach
  void afterEach() {
    evictAllCaches();
  }

  public void evictAllCaches() {
    cacheManager.getCacheNames().forEach(cacheName -> cacheManager.getCache(cacheName).clear());
  }

  public static ResultActions attemptGet(String uri, Object... args) throws Exception {
    return mockMvc.perform(get(uri, args)
      .headers(okapiHeaders())
      .contentType(APPLICATION_JSON));
  }

  protected static ResultActions attemptPost(String uri, Object body, Object... args) throws Exception {
    return mockMvc.perform(post(uri, args)
      .headers(okapiHeaders())
      .content(asJsonString(body))
      .contentType(APPLICATION_JSON));
  }

  protected static ResultActions attemptPostWithTenant(String uri, String tenant,
                                                       Object body, Object... args) throws Exception {
    return mockMvc.perform(post(uri, args)
      .headers(okapiHeadersWithTenant(tenant))
      .content(asJsonString(body))
      .contentType(APPLICATION_JSON));
  }

  protected static ResultActions attemptPut(String uri, Object body, Object... args) throws Exception {
    return mockMvc.perform(put(uri, args)
      .headers(okapiHeaders())
      .content(asJsonString(body))
      .contentType(APPLICATION_JSON));
  }

  protected static ResultActions attemptDelete(String uri, Object... args) throws Exception {
    return mockMvc.perform(delete(uri, args)
      .headers(okapiHeaders())
      .contentType(APPLICATION_JSON));
  }

  public static ResultActions doGet(String uri, Object... args) throws Exception {
    return attemptGet(uri, args).andExpect(status().isOk());
  }

  protected static ResultActions doPost(String uri, Object body, Object... args) throws Exception {
    return attemptPost(uri, body, args).andExpect(status().isCreated());
  }

  protected static ResultActions doPostWithTenant(String uri, String tenant,
                                                  Object body, Object... args) throws Exception {
    return attemptPostWithTenant(uri, tenant, body, args).andExpect(status().isCreated());
  }

  protected static ResultActions doPut(String uri, Object body, Object... args) throws Exception {
    return attemptPut(uri, body, args).andExpect(status().isNoContent());
  }

  protected static ResultActions doDelete(String uri, Object... args) throws Exception {
    return attemptDelete(uri, args).andExpect(status().isNoContent());
  }

  protected static HttpHeaders okapiHeaders() {
    var headers = new HttpHeaders();
    headers.add(URL, wmAdminClient.getWireMockUrl());
    headers.add(XOkapiHeaders.TOKEN, OKAPI_AUTH_TOKEN);
    headers.add(TENANT, TENANT_NAME);
    return headers;
  }

  protected static HttpHeaders okapiHeadersWithTenant(String tenant) {
    var headers = new HttpHeaders();
    headers.add(URL, wmAdminClient.getWireMockUrl());
    headers.add(XOkapiHeaders.TOKEN, OKAPI_AUTH_TOKEN);
    headers.add(TENANT, tenant);
    return headers;
  }

  @SneakyThrows
  @SuppressWarnings("SameParameterValue")
  protected static void enableTenant(String tenantId) {
    wmAdminClient.addStubMapping(readString("wiremock/stubs/users/create-system-user.json"));
    wmAdminClient.addStubMapping(readString("wiremock/stubs/users/create-system-user-central.json"));

    var realmFile = "json/keycloak/" + tenantId + "-realm.json";
    var tenantRealmRepresentation = TestValues.readValue(realmFile, RealmRepresentation.class);
    getKeycloakAdminClient().realms().create(tenantRealmRepresentation);

    var tenantAttributes = new TenantAttributes().moduleTo(MODULE_NAME);
    mockMvc.perform(post("/_/tenant")
        .content(asJsonString(tenantAttributes))
        .contentType(APPLICATION_JSON)
        .header(TENANT, tenantId)
        .header(URL, wmAdminClient.getWireMockUrl())
        .header(XOkapiHeaders.TOKEN, OKAPI_AUTH_TOKEN))
      .andExpect(status().isNoContent());

    assertThat(wmAdminClient.unmatchedRequests().getRequests()).isEmpty();
    wmAdminClient.resetAll();
  }

  @SneakyThrows
  protected static void removeTenant(String tenantId) {
    removeTenant(tenantId, true);
  }

  @SneakyThrows
  protected static void removeTenant(String tenantId, boolean removeRealm) {
    wmAdminClient.addStubMapping(readString("wiremock/stubs/users/find-system-user-by-query.json"));
    wmAdminClient.addStubMapping(readString("wiremock/stubs/users/delete-system-user.json"));
    wmAdminClient.addStubMapping(readString("wiremock/stubs/users/find-system-user-by-id.json"));
    wmAdminClient.addStubMapping(readString("wiremock/stubs/policy/find-policy-by-system-username.json"));
    wmAdminClient.addStubMapping(readString("wiremock/stubs/users/get-system-user-capability.json"));
    wmAdminClient.addStubMapping(readString("wiremock/stubs/users/get-system-user-capability-set.json"));
    wmAdminClient.addStubMapping(readString("wiremock/stubs/users/get-system-user-roles.json"));

    var tenantAttributes = new TenantAttributes().moduleFrom(MODULE_NAME).purge(true);
    mockMvc.perform(post("/_/tenant")
        .content(asJsonString(tenantAttributes))
        .contentType(APPLICATION_JSON)
        .header(TENANT, tenantId)
        .header(URL, wmAdminClient.getWireMockUrl())
        .header(XOkapiHeaders.TOKEN, OKAPI_AUTH_TOKEN))
      .andExpect(status().isNoContent());

    assertThat(wmAdminClient.unmatchedRequests().getRequests()).isEmpty();
    wmAdminClient.resetAll();

    if (removeRealm) {
      getKeycloakAdminClient().realm(tenantId).remove();
    }
  }

  protected void createIdentityProviderInCentralTenant() {
    var identityProvider = parse(readTemplate("user/create-identity-provider-request.json"), IdentityProvider.class);
    keycloakClient.createIdentityProvider(CENTRAL_TENANT_NAME, identityProvider, tokenService.issueToken());
  }

  protected void removeIdentityProviderInCentralTenant() {
    keycloakClient.removeIdentityProvider(CENTRAL_TENANT_NAME, PROVIDER_ALIAS, tokenService.issueToken());
  }

  protected KeycloakUser createShadowKeycloakUserInCentralTenant(User user) {
    var shadowKeycloakUser = keycloakService.toKeycloakUser(user);
    keycloakClient.createUser(CENTRAL_TENANT_NAME, shadowKeycloakUser, tokenService.issueToken());
    return keycloakService.findKeycloakUserWithUserIdAttr(CENTRAL_TENANT_NAME, user.getId())
      .orElseThrow();
  }

  protected void removeShadowKeycloakUserInCentralTenant(String userId) {
    keycloakClient.deleteUser(CENTRAL_TENANT_NAME, userId, tokenService.issueToken());
  }

  protected void verifyKeycloakUser(User user) {
    var authToken = tokenService.issueToken();
    var keycloakUsers = keycloakClient.findUsersByUsername(TENANT_NAME, user.getUsername(), false, authToken);
    var keycloakUser = keycloakUsers.stream().findFirst()
      .orElseThrow();

    assertThat(user.getPersonal()).isNotNull();
    assertThat(keycloakUser.getEmail()).isEqualTo(user.getPersonal().getEmail());

    var attributes = keycloakUser.getAttributes();
    assertThat(user.getId()).isNotNull();
    assertThat(attributes.get(USER_ID_ATTR)).contains(user.getId().toString());
    assertThat(attributes.get(USER_EXTERNAL_SYSTEM_ID_ATTR)).contains(user.getExternalSystemId());
  }

  protected CreateUserVerifyDto verifyKeycloakUser(String tenant, User user) {
    var authToken = tokenService.issueToken();
    var keycloakUsers = keycloakClient.findUsersByUsername(tenant, user.getUsername(), false, authToken);
    var keycloakUser = keycloakUsers.stream().findFirst()
      .orElseThrow();

    assertThat(user.getPersonal()).isNotNull();
    assertThat(keycloakUser.getEmail()).isEqualTo(user.getPersonal().getEmail());

    var attributes = keycloakUser.getAttributes();
    assertThat(user.getId()).isNotNull();
    assertThat(attributes.get(USER_ID_ATTR)).contains(user.getId().toString());
    assertThat(attributes.get(USER_EXTERNAL_SYSTEM_ID_ATTR)).contains(user.getExternalSystemId());

    return new CreateUserVerifyDto(authToken, keycloakUser);
  }

  protected void verifyKeycloakUserAndIdentityProvider(String tenant, User user, String kcUserId) {
    var dto = verifyKeycloakUser(tenant, user);

    var federatedIdentities = keycloakClient.getUserIdentityProvider(CENTRAL_TENANT_NAME, kcUserId, dto.authToken());
    assertThat(federatedIdentities).isNotEmpty();
    assertThat(federatedIdentities).hasSize(1);

    var federatedIdentity = federatedIdentities.stream().findFirst();
    assertThat(federatedIdentity).isNotNull();
    assertThat(federatedIdentity.get().getProviderAlias()).isNotNull();
    assertThat(federatedIdentity.get().getProviderAlias()).isEqualTo(PROVIDER_ALIAS);
  }

  protected void verifyKeycloakUserAndWithNoIdentityProvider(String tenant, User user) {
    var dto = verifyKeycloakUser(tenant, user);

    var kcUserId = keycloakService.findKeycloakUserWithUserIdAttr(CENTRAL_TENANT_NAME, user.getId())
      .orElseThrow().getId();

    assertThatThrownBy(() -> keycloakClient.getUserIdentityProvider(TENANT_NAME, kcUserId, dto.authToken()))
      .isInstanceOf(FeignException.NotFound.class)
      .hasMessageContaining("User not found");
  }

  @TestConfiguration
  public static class TopicConfiguration {

    @Bean
    public NewTopic systemUserTopic() {
      return new NewTopic(FOLIO_SYSTEM_USER_TOPIC, 1, (short) 1);
    }
  }
}
