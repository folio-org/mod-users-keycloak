package org.folio.uk.it;

import static org.folio.test.TestUtils.asJsonString;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import lombok.SneakyThrows;
import org.apache.commons.lang3.StringUtils;
import org.folio.spring.integration.XOkapiHeaders;
import org.folio.test.extensions.WireMockStub;
import org.folio.test.types.IntegrationTest;
import org.folio.uk.base.BaseIntegrationTest;
import org.folio.uk.domain.dto.GenerateLinkRequest;
import org.folio.uk.domain.dto.User;
import org.folio.uk.integration.keycloak.KeycloakClient;
import org.folio.uk.integration.keycloak.RealmConfigurationProvider;
import org.folio.uk.integration.keycloak.model.KeycloakRealmConfiguration;
import org.folio.uk.integration.keycloak.model.TokenResponse;
import org.folio.uk.integration.notify.NotificationClient;
import org.folio.uk.integration.notify.model.Context;
import org.folio.uk.integration.notify.model.Notification;
import org.folio.uk.support.TestConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

@IntegrationTest
class GeneratePasswordResetLinkIT extends BaseIntegrationTest {

  private static final String GENERATE_PASSWORD_RESET_LINK_PATH = "/users-keycloak/password-reset/link";
  private static final String MOCK_FOLIO_UI_HOST = "http://localhost:3000";
  private static final String DEFAULT_UI_URL = "/reset-password";
  private static final String CLIENT_ID = "password-reset-client";
  private static final String RESET_PASSWORD_TOKEN = "ZHVtbXlKd3Q=.eyJzdWIiOiJyZXNldC1wYXNzd29yZC1jbGllbnQiLCJwYXN"
    + "zd29yZFJlc2V0QWN0aW9uSWQiOiI1YWMzYjgyZC1hN2Q0LTQzYTAtODI4NS0xMDRlODRlMDEyNzQifQ==.c2ln";
  private static final UUID TEST_USER_ID = UUID.fromString("d3958402-2f80-421b-a527-9933245a3556");
  public static final User TEST_USER = new User().id(TEST_USER_ID).username("diku_admin").active(true);
  private static final GenerateLinkRequest GENERATE_LINK_REQUEST = new GenerateLinkRequest().userId(TEST_USER_ID);
  private static final String TEST_TENANT = "diku";
  private static final String EXPIRATION_TIME_MINUTES = "15";
  private static final String EXPIRATION_TIME_HOURS = "24";
  private static final String EXPIRATION_TIME_DAYS = "2";
  private static final String EXPIRATION_TIME_WEEKS = "4";
  private static final String EXPIRATION_UNIT_OF_TIME_HOURS = "hours";
  private static final String EXPIRATION_UNIT_OF_TIME_MINUTES = "minutes";
  private static final String EXPIRATION_UNIT_OF_TIME_DAYS = "days";
  private static final String EXPIRATION_UNIT_OF_TIME_WEEKS = "weeks";
  private static final String NOTIFICATION_LANG = "en";
  private static final String RESET_PASSWORD_EVENT = "RESET_PASSWORD_EVENT";
  private static final KeycloakRealmConfiguration CLIENT_CONFIG =
    new KeycloakRealmConfiguration().clientId(CLIENT_ID).clientSecret("secret");

  @MockBean private KeycloakClient keycloakClient;
  @MockBean private RealmConfigurationProvider realmConfigurationProvider;
  @SpyBean private NotificationClient notificationClient;

  @BeforeEach
  void setup() {
    var tokenResponse = new TokenResponse();
    tokenResponse.setAccessToken(RESET_PASSWORD_TOKEN);
    when(keycloakClient.login(anyMap(), eq(TEST_TENANT))).thenReturn(tokenResponse);
    when(realmConfigurationProvider.getClientConfiguration(TEST_TENANT, CLIENT_ID)).thenReturn(CLIENT_CONFIG);
  }

  @Test
  @WireMockStub(scripts = "/wiremock/stubs/config/get-configs-without-reset.json")
  @WireMockStub(scripts = "/wiremock/stubs/users/get-diku-user.json")
  @WireMockStub(scripts = "/wiremock/stubs/login/reset-existing-password.json")
  @WireMockStub(scripts = "/wiremock/stubs/notify/create-password-reset-notification.json")
  void generatePasswordResetLink_positive_resetPasswordWithDefaultExpirationTime() throws Exception {
    var expectedLink = MOCK_FOLIO_UI_HOST + DEFAULT_UI_URL + '/' + RESET_PASSWORD_TOKEN;

    callGeneratePasswordResetLink()
      .andExpectAll(status().isOk(),
        jsonPath("$.link", is(expectedLink)));

    var expectedNotification = new Notification()
      .withEventConfigName(RESET_PASSWORD_EVENT)
      .withContext(
        new Context()
          .withAdditionalProperty("user", TEST_USER)
          .withAdditionalProperty("link", expectedLink)
          .withAdditionalProperty("expirationTime", EXPIRATION_TIME_HOURS)
          .withAdditionalProperty("expirationUnitOfTime", EXPIRATION_UNIT_OF_TIME_HOURS))
      .withRecipientId(TEST_USER_ID)
      .withLang(NOTIFICATION_LANG)
      .withText(StringUtils.EMPTY);

    verify(notificationClient).sendNotification(eq(expectedNotification));
  }

  @Test
  @WireMockStub(scripts = "/wiremock/stubs/config/get-configs-without-reset.json")
  @WireMockStub(scripts = "/wiremock/stubs/users/get-diku-user.json")
  @WireMockStub(scripts = "/wiremock/stubs/login/reset-non-existing-password.json")
  @WireMockStub(scripts = "/wiremock/stubs/notify/create-password-reset-notification.json")
  void generatePasswordResetLink_positive_createPasswordWithDefaultExpirationTime() throws Exception {
    var expectedLink = MOCK_FOLIO_UI_HOST + DEFAULT_UI_URL + '/' + RESET_PASSWORD_TOKEN;

    callGeneratePasswordResetLink()
      .andExpectAll(status().isOk(),
        jsonPath("$.link", is(expectedLink)));

    verify(notificationClient).sendNotification(any());
  }

  @Test
  @WireMockStub(scripts = "/wiremock/stubs/config/get-reset-configs-expiration-minutes.json")
  @WireMockStub(scripts = "/wiremock/stubs/users/get-diku-user.json")
  @WireMockStub(scripts = "/wiremock/stubs/login/reset-existing-password.json")
  @WireMockStub(scripts = "/wiremock/stubs/notify/create-password-reset-notification.json")
  public void shouldGenerateAndSendPasswordNotificationWhenPasswordWithMinutesOfExpirationTime() {
    generateAndSendResetPasswordNotificationWhenPasswordExistsWith(EXPIRATION_TIME_MINUTES,
      EXPIRATION_UNIT_OF_TIME_MINUTES);
  }

  @Test
  @WireMockStub(scripts = "/wiremock/stubs/config/get-reset-configs-expiration-days.json")
  @WireMockStub(scripts = "/wiremock/stubs/users/get-diku-user.json")
  @WireMockStub(scripts = "/wiremock/stubs/login/reset-existing-password.json")
  @WireMockStub(scripts = "/wiremock/stubs/notify/create-password-reset-notification.json")
  public void shouldGenerateAndSendPasswordNotificationWhenPasswordWithDaysOfExpirationTime() {
    generateAndSendResetPasswordNotificationWhenPasswordExistsWith(EXPIRATION_TIME_DAYS, EXPIRATION_UNIT_OF_TIME_DAYS);
  }

  @Test
  @WireMockStub(scripts = "/wiremock/stubs/config/get-reset-configs-expiration-weeks.json")
  @WireMockStub(scripts = "/wiremock/stubs/users/get-diku-user.json")
  @WireMockStub(scripts = "/wiremock/stubs/login/reset-existing-password.json")
  @WireMockStub(scripts = "/wiremock/stubs/notify/create-password-reset-notification.json")
  public void shouldGenerateAndSendPasswordNotificationWhenPasswordWithWeeksOfExpirationTime() {
    generateAndSendResetPasswordNotificationWhenPasswordExistsWith(EXPIRATION_TIME_WEEKS,
      EXPIRATION_UNIT_OF_TIME_WEEKS);
  }

  @Test
  @WireMockStub(scripts = "/wiremock/stubs/config/get-reset-configs-expiration-weeks-exceeded-max.json")
  @WireMockStub(scripts = "/wiremock/stubs/users/get-diku-user.json")
  @WireMockStub(scripts = "/wiremock/stubs/login/reset-existing-password.json")
  @WireMockStub(scripts = "/wiremock/stubs/notify/create-password-reset-notification.json")
  public void shouldGenerateAndSendPasswordNotificationWhenExpirationTimeIsBiggerThanMax() {
    generateAndSendResetPasswordNotificationWhenPasswordExistsWith(EXPIRATION_TIME_WEEKS,
      EXPIRATION_UNIT_OF_TIME_WEEKS);
  }

  @Test
  @WireMockStub(scripts = "/wiremock/stubs/config/get-configs-invalid-exp.json")
  @WireMockStub(scripts = "/wiremock/stubs/users/get-diku-user.json")
  @WireMockStub(scripts = "/wiremock/stubs/login/reset-existing-password.json")
  public void shouldGenerateAndSendPasswordNotificationWhenExpirationTimeIsIncorrect() {
    shouldHandleExceptionWhenConvertTime();
  }

  @Test
  @WireMockStub(scripts = "/wiremock/stubs/config/get-configs-invalid-time-unit.json")
  @WireMockStub(scripts = "/wiremock/stubs/users/get-diku-user.json")
  @WireMockStub(scripts = "/wiremock/stubs/login/reset-existing-password.json")
  public void shouldGenerateAndSendPasswordNotificationWhenExpirationOfUnitTimeIsIncorrect() {
    shouldHandleExceptionWhenConvertTime();
  }

  @SneakyThrows
  private void generateAndSendResetPasswordNotificationWhenPasswordExistsWith(
    String expectedExpirationTime, String expectedExpirationTimeOfUnit) {
    var expectedLink = MOCK_FOLIO_UI_HOST + DEFAULT_UI_URL + '/' + RESET_PASSWORD_TOKEN;

    callGeneratePasswordResetLink()
      .andExpectAll(status().isOk(), jsonPath("$.link", is(expectedLink)));

    var expectedNotification = new Notification()
      .withEventConfigName(RESET_PASSWORD_EVENT)
      .withContext(
        new Context()
          .withAdditionalProperty("user", TEST_USER)
          .withAdditionalProperty("link", expectedLink)
          .withAdditionalProperty("expirationTime", expectedExpirationTime)
          .withAdditionalProperty("expirationUnitOfTime", expectedExpirationTimeOfUnit))
      .withRecipientId(TEST_USER_ID)
      .withLang(NOTIFICATION_LANG)
      .withText(StringUtils.EMPTY);

    verify(notificationClient).sendNotification(eq(expectedNotification));
  }

  @SneakyThrows
  private void shouldHandleExceptionWhenConvertTime() {
    callGeneratePasswordResetLink()
      .andExpectAll(status().isUnprocessableEntity(),
        content().string(containsString("Can't convert time period to milliseconds")));

    verifyNoInteractions(notificationClient);
  }

  @SneakyThrows
  private static ResultActions callGeneratePasswordResetLink() {
    return mockMvc.perform(post(GENERATE_PASSWORD_RESET_LINK_PATH)
      .header(XOkapiHeaders.TENANT, TEST_TENANT)
      .header(XOkapiHeaders.URL, wmAdminClient.getWireMockUrl())
      .header(XOkapiHeaders.TOKEN, TestConstants.OKAPI_AUTH_TOKEN)
      .content(asJsonString(GENERATE_LINK_REQUEST))
      .contentType(MediaType.APPLICATION_JSON));
  }
}
