package org.folio.uk.it;

import static org.assertj.core.api.Assertions.assertThat;
import static org.folio.test.TestUtils.asJsonString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.net.URI;
import lombok.SneakyThrows;
import org.folio.spring.integration.XOkapiHeaders;
import org.folio.test.extensions.WireMockStub;
import org.folio.test.types.IntegrationTest;
import org.folio.uk.base.BaseIntegrationTest;
import org.folio.uk.domain.dto.Identifier;
import org.folio.uk.integration.keycloak.KeycloakClient;
import org.folio.uk.integration.keycloak.RealmConfigurationProvider;
import org.folio.uk.integration.keycloak.model.KeycloakRealmConfiguration;
import org.folio.uk.integration.keycloak.model.TokenResponse;
import org.folio.uk.integration.notify.NotificationClient;
import org.folio.uk.integration.notify.model.Notification;
import org.folio.uk.support.TestConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.util.LinkedMultiValueMap;

/**
 * Verifies the whole forgotten-password flow (locate user, build reset link, send notification) and checks that the
 * notification carries a valid "request a new link" (forgot-password) URL.
 */
@IntegrationTest
class ForgottenPasswordEmailLinkIT extends BaseIntegrationTest {

  private static final String FORGOTTEN_PASSWORD_PATH = "/users-keycloak/forgotten/password";
  private static final String TEST_TENANT = "diku";
  private static final String CLIENT_ID = "password-reset-client";
  private static final String RESET_PASSWORD_TOKEN = "ZHVtbXlKd3Q=.eyJzdWIiOiJyZXNldC1wYXNzd29yZC1jbGllbnQiLCJwYXN"
    + "zd29yZFJlc2V0QWN0aW9uSWQiOiI1YWMzYjgyZC1hN2Q0LTQzYTAtODI4NS0xMDRlODRlMDEyNzQifQ==.c2ln";
  private static final String DEFAULT_FORGOT_PASSWORD_LINK = "http://localhost:3000/forgot-password";
  private static final String CONFIGURED_FORGOT_PASSWORD_LINK = "https://diku.folio.example.org/custom/forgot-password";
  private static final String RESET_PASSWORD_EVENT = "RESET_PASSWORD_EVENT";
  private static final String CREATE_PASSWORD_EVENT = "CREATE_PASSWORD_EVENT";
  private static final KeycloakRealmConfiguration CLIENT_CONFIG =
    new KeycloakRealmConfiguration().clientId(CLIENT_ID).clientSecret("secret");

  @MockitoBean private KeycloakClient keycloakClient;
  @MockitoBean private RealmConfigurationProvider realmConfigurationProvider;
  @MockitoSpyBean private NotificationClient notificationClient;

  @BeforeEach
  void setup() {
    var tokenResponse = new TokenResponse();
    tokenResponse.setAccessToken(RESET_PASSWORD_TOKEN);
    when(keycloakClient.login(any(LinkedMultiValueMap.class), eq(TEST_TENANT))).thenReturn(tokenResponse);
    when(realmConfigurationProvider.getClientConfiguration(TEST_TENANT, CLIENT_ID)).thenReturn(CLIENT_CONFIG);
  }

  @Test
  @WireMockStub(scripts = "/wiremock/stubs/user-tenants/query-diku-users-notfound.json")
  @WireMockStub(scripts = "/wiremock/stubs/users/query-diku-user.json")
  @WireMockStub(scripts = "/wiremock/stubs/users/get-diku-user.json")
  @WireMockStub(scripts = "/wiremock/stubs/config/get-diku-forgottenData-configs.json")
  @WireMockStub(scripts = "/wiremock/stubs/config/get-configs-empty-response.json")
  @WireMockStub(scripts = "/wiremock/stubs/settings/get-base-url.json")
  @WireMockStub(scripts = "/wiremock/stubs/login/reset-existing-password.json")
  @WireMockStub(scripts = "/wiremock/stubs/notify/create-password-reset-notification.json")
  void postResetForgottenPassword_positive_emailContainsDefaultForgotPasswordLink() throws Exception {
    callResetForgottenPassword("diku_admin").andExpect(status().isNoContent());

    var notification = captureSentNotification();
    assertThat(notification.getEventConfigName()).isEqualTo(RESET_PASSWORD_EVENT);
    assertValidForgotPasswordLink(notification, DEFAULT_FORGOT_PASSWORD_LINK);
  }

  @Test
  @WireMockStub(scripts = "/wiremock/stubs/user-tenants/query-diku-users-notfound.json")
  @WireMockStub(scripts = "/wiremock/stubs/users/query-diku-user.json")
  @WireMockStub(scripts = "/wiremock/stubs/users/get-diku-user.json")
  @WireMockStub(scripts = "/wiremock/stubs/config/get-diku-forgottenData-configs.json")
  @WireMockStub(scripts = "/wiremock/stubs/config/get-forgot-password-configs-custom.json")
  @WireMockStub(scripts = "/wiremock/stubs/settings/get-base-url.json")
  @WireMockStub(scripts = "/wiremock/stubs/login/reset-existing-password.json")
  @WireMockStub(scripts = "/wiremock/stubs/notify/create-password-reset-notification.json")
  void postResetForgottenPassword_positive_emailContainsConfiguredForgotPasswordLink() throws Exception {
    callResetForgottenPassword("diku_admin").andExpect(status().isNoContent());

    var notification = captureSentNotification();
    assertThat(notification.getEventConfigName()).isEqualTo(RESET_PASSWORD_EVENT);
    assertValidForgotPasswordLink(notification, CONFIGURED_FORGOT_PASSWORD_LINK);
  }

  @Test
  @WireMockStub(scripts = "/wiremock/stubs/user-tenants/query-diku-users-notfound.json")
  @WireMockStub(scripts = "/wiremock/stubs/users/query-diku-user.json")
  @WireMockStub(scripts = "/wiremock/stubs/users/get-diku-user.json")
  @WireMockStub(scripts = "/wiremock/stubs/config/get-diku-forgottenData-configs.json")
  @WireMockStub(scripts = "/wiremock/stubs/config/get-configs-empty-response.json")
  @WireMockStub(scripts = "/wiremock/stubs/settings/get-base-url.json")
  @WireMockStub(scripts = "/wiremock/stubs/login/reset-non-existing-password.json")
  @WireMockStub(scripts = "/wiremock/stubs/notify/create-password-reset-notification.json")
  void postResetForgottenPassword_positive_createPasswordEmailContainsForgotPasswordLink() throws Exception {
    callResetForgottenPassword("diku_admin").andExpect(status().isNoContent());

    var notification = captureSentNotification();
    assertThat(notification.getEventConfigName()).isEqualTo(CREATE_PASSWORD_EVENT);
    assertValidForgotPasswordLink(notification, DEFAULT_FORGOT_PASSWORD_LINK);
  }

  private Notification captureSentNotification() {
    var captor = ArgumentCaptor.forClass(Notification.class);
    verify(notificationClient).sendNotification(captor.capture());
    return captor.getValue();
  }

  private static void assertValidForgotPasswordLink(Notification notification, String expectedLink) {
    var forgotPasswordLink = (String) notification.getContext().getAdditionalProperties().get("forgotPasswordLink");
    assertThat(forgotPasswordLink).isNotBlank().isEqualTo(expectedLink);
    var uri = URI.create(forgotPasswordLink);
    assertThat(uri.getScheme()).isIn("http", "https");
    assertThat(uri.getHost()).isNotBlank();
  }

  @SneakyThrows
  private static ResultActions callResetForgottenPassword(String identifier) {
    return mockMvc.perform(post(FORGOTTEN_PASSWORD_PATH)
      .header(XOkapiHeaders.TENANT, TEST_TENANT)
      .header(XOkapiHeaders.URL, wmAdminClient.getWireMockUrl())
      .header(XOkapiHeaders.TOKEN, TestConstants.OKAPI_AUTH_TOKEN)
      .content(asJsonString(new Identifier().id(identifier)))
      .contentType(MediaType.APPLICATION_JSON));
  }
}
