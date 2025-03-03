package org.folio.uk.base;

import static net.minidev.json.JSONValue.parse;
import static org.assertj.core.api.Assertions.assertThat;
import static org.folio.spring.integration.XOkapiHeaders.TENANT;
import static org.folio.spring.integration.XOkapiHeaders.URL;
import static org.folio.test.TestUtils.asJsonString;
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
import java.util.Objects;
import java.util.Optional;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.apache.kafka.clients.admin.NewTopic;
import org.folio.spring.integration.XOkapiHeaders;
import org.folio.tenant.domain.dto.TenantAttributes;
import org.folio.test.TestUtils;
import org.folio.test.base.BaseBackendIntegrationTest;
import org.folio.test.extensions.EnableKafka;
import org.folio.test.extensions.EnableKeycloakTlsMode;
import org.folio.test.extensions.EnablePostgres;
import org.folio.test.extensions.EnableWireMock;
import org.folio.test.extensions.impl.KafkaTestExecutionListener;
import org.folio.test.extensions.impl.KeycloakExecutionListener;
import org.folio.test.extensions.impl.WireMockAdminClient;
import org.folio.test.extensions.impl.WireMockExecutionListener;
import org.folio.uk.base.model.IdentityProvider;
import org.folio.uk.domain.dto.User;
import org.folio.uk.integration.keycloak.KeycloakClient;
import org.folio.uk.integration.keycloak.KeycloakService;
import org.folio.uk.integration.keycloak.TokenService;
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
  @Autowired protected KeycloakTestClient keycloakTestClient;
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

  protected static ResultActions attemptDeleteWithTenant(String uri, String tenant,
                                                         Object body, Object... args) throws Exception {
    return mockMvc.perform(delete(uri, args)
      .headers(okapiHeadersWithTenant(tenant))
      .content(asJsonString(body))
      .contentType(APPLICATION_JSON));
  }

  public static ResultActions doGet(String uri, Object... args) throws Exception {
    return attemptGet(uri, args).andExpect(status().isOk());
  }

  protected static ResultActions doPost(String uri, Object body, Object... args) throws Exception {
    return attemptPost(uri, body, args).andExpect(status().isCreated());
  }

  protected static ResultActions doPostWithTenant(String uri, String tenant, Object body, Object... args)
    throws Exception {
    return attemptPostWithTenant(uri, tenant, body, args).andExpect(status().isCreated());
  }

  protected static ResultActions doPostWithTenantAndStatusCode(String uri, String tenant, Object body,
                                                               int statusCode, Object... args) throws Exception {
    return attemptPostWithTenant(uri, tenant, body, args).andExpect(status().is(statusCode));
  }

  protected static ResultActions doPut(String uri, Object body, Object... args) throws Exception {
    return attemptPut(uri, body, args).andExpect(status().isNoContent());
  }

  protected static ResultActions doDelete(String uri, Object... args) throws Exception {
    return attemptDelete(uri, args).andExpect(status().isNoContent());
  }

  protected static ResultActions doDeleteWithTenantAndStatusCode(String uri, String tenant, Object body,
                                                                 int statusCode, Object... args) throws Exception {
    return attemptDeleteWithTenant(uri, tenant, body, args).andExpect(status().is(statusCode));
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
    wmAdminClient.addStubMapping(readString(tenantId, "wiremock/stubs/users/%s/create-system-user.json"));

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
    wmAdminClient.addStubMapping(readString(tenantId, "wiremock/stubs/users/%s/find-system-user-by-query.json"));
    wmAdminClient.addStubMapping(readString(tenantId, "wiremock/stubs/users/%s/delete-system-user.json"));
    wmAdminClient.addStubMapping(readString(tenantId, "wiremock/stubs/users/%s/find-system-user-by-id.json"));
    wmAdminClient.addStubMapping(readString(tenantId, "wiremock/stubs/policy/%s/find-policy-by-system-username.json"));
    wmAdminClient.addStubMapping(readString(tenantId, "wiremock/stubs/users/%s/get-system-user-capability.json"));
    wmAdminClient.addStubMapping(readString(tenantId, "wiremock/stubs/users/%s/get-system-user-capability-set.json"));
    wmAdminClient.addStubMapping(readString(tenantId, "wiremock/stubs/users/%s/get-system-user-roles.json"));

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

  protected static String readString(String tenantId, String filename) {
    var tenantFolder = tenantId.equals(CENTRAL_TENANT_NAME) ? "central-tenant" : "";
    return TestUtils.readString(java.nio.file.Path.of(String.format(filename, tenantFolder))
      .normalize().toString());
  }

  protected void createIdentityProviderInCentralTenant() {
    var templateFilePath = "user/create-identity-provider-request.json";
    var identityProvider = parse(readTemplate(templateFilePath), IdentityProvider.class);
    keycloakTestClient.createIdentityProvider(CENTRAL_TENANT_NAME, identityProvider, tokenService.issueToken());
    log.info("Created identity provider in central tenant");
  }

  protected void removeIdentityProviderInCentralTenant() {
    try {
      keycloakTestClient.removeIdentityProvider(CENTRAL_TENANT_NAME, PROVIDER_ALIAS, tokenService.issueToken());
      log.info("Removed identity provider in central tenant");
    } catch (FeignException.NotFound e) {
      log.info("Cannot remove identity provider in central tenant, provider is not found");
    }
  }

  protected void removeShadowKeycloakUserInCentralTenant(String tenant, User user) {
    if (Objects.isNull(user)) {
      return;
    }
    var authToken = tokenService.issueToken();
    getKcUser(tenant, user, authToken)
      .ifPresent(kcUser ->  keycloakClient.deleteUser(tenant, kcUser.getId(), authToken));
    log.info("Removed shadow keycloak user in central tenant");
  }

  protected void assertSuccessfulUserCreation(User resp, User user) {
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

  protected void verifyKeycloakUser(User user) {
    var authToken = tokenService.issueToken();
    var kcUser = getKcUser(TENANT_NAME, user, authToken).orElseThrow();

    assertThat(user.getPersonal()).isNotNull();
    assertThat(kcUser.getEmail()).isEqualTo(user.getPersonal().getEmail());

    var attributes = kcUser.getAttributes();
    assertThat(user.getId()).isNotNull();
    assertThat(attributes.get(USER_ID_ATTR)).contains(user.getId().toString());
    assertThat(attributes.get(USER_EXTERNAL_SYSTEM_ID_ATTR)).contains(user.getExternalSystemId());
  }

  protected CreateUserVerifyDto verifyKeycloakUser(String tenant, User user) {
    var authToken = tokenService.issueToken();
    var kcUser = getKcUser(tenant, user, authToken).orElseThrow();

    assertThat(user.getPersonal()).isNotNull();
    assertThat(kcUser.getEmail()).isEqualTo(user.getPersonal().getEmail());

    var attributes = kcUser.getAttributes();
    assertThat(user.getId()).isNotNull();
    assertThat(attributes.get(USER_ID_ATTR)).contains(user.getId().toString());
    assertThat(attributes.get(USER_EXTERNAL_SYSTEM_ID_ATTR)).contains(user.getExternalSystemId());

    return new CreateUserVerifyDto(authToken, kcUser);
  }

  private Optional<KeycloakUser> getKcUser(String tenant, User user, String authToken) {
    return keycloakClient.findUsersByUsername(tenant, user.getUsername(), false, authToken)
      .stream().findFirst();
  }

  protected void verifyKeycloakUserAndIdentityProvider(String tenant, User user) {
    var verifyDto = verifyKeycloakUser(tenant, user);
    var authToken = verifyDto.authToken();
    var kcUserId = verifyDto.kcUser().getId();

    var federatedIdentities = keycloakTestClient.getUserIdentityProvider(tenant, kcUserId, authToken);
    assertThat(federatedIdentities).isNotEmpty();
    assertThat(federatedIdentities).hasSize(1);

    var federatedIdentity = federatedIdentities.stream().findFirst();
    assertThat(federatedIdentity).isNotNull();
    assertThat(federatedIdentity.get().getProviderAlias()).isNotNull();
    assertThat(federatedIdentity.get().getProviderAlias()).isEqualTo(PROVIDER_ALIAS);
  }

  protected void verifyKeycloakUserAndWithNoIdentityProviderExisting(String tenant, User user) {
    var verifyDto = verifyKeycloakUser(tenant, user);

    var kcUserId = keycloakService.findKeycloakUserWithUserIdAttr(tenant, user.getId())
      .orElseThrow().getId();

    var federatedIdentities = keycloakTestClient.getUserIdentityProvider(tenant, kcUserId, verifyDto.authToken());
    assertThat(federatedIdentities).isEmpty();
  }

  @TestConfiguration
  public static class TopicConfiguration {

    @Bean
    public NewTopic systemUserTopic() {
      return new NewTopic(FOLIO_SYSTEM_USER_TOPIC, 1, (short) 1);
    }
  }
}
