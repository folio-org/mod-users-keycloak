package org.folio.uk.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.folio.spring.FolioExecutionContext;
import org.folio.test.types.UnitTest;
import org.folio.uk.domain.dto.User;
import org.folio.uk.integration.configuration.ConfigurationService;
import org.folio.uk.integration.keycloak.PasswordResetTokenService;
import org.folio.uk.integration.keycloak.model.TokenResponse;
import org.folio.uk.integration.login.LoginService;
import org.folio.uk.integration.login.model.PasswordResetActionCreated;
import org.folio.uk.integration.notify.NotificationService;
import org.folio.uk.integration.settings.SettingsService;
import org.folio.uk.integration.users.UsersClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@UnitTest
@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTest {

  private static final UUID USER_ID = UUID.randomUUID();
  private static final User USER = new User().id(USER_ID).username("john").active(true);
  private static final String TOKEN = "reset-token";
  private static final String GENERATED_LINK = "http://localhost:3000/reset-password/reset-token?tenant=diku";
  private static final String CREATE_PASSWORD_EVENT = "CREATE_PASSWORD_EVENT";
  private static final String RESET_PASSWORD_EVENT = "RESET_PASSWORD_EVENT";
  private static final String FOLIO_HOST = "FOLIO_HOST";
  private static final String FORGOT_PASSWORD_UI_PATH = "FORGOT_PASSWORD_UI_PATH";
  private static final String DEFAULT_FORGOT_PASSWORD_LINK = "http://localhost:3000/forgot-password";

  @InjectMocks private PasswordResetService service;
  @Mock private ConfigurationService configurationService;
  @Mock private NotificationService notificationService;
  @Mock private PasswordResetTokenService resetTokenService;
  @Mock private LoginService loginService;
  @Mock private UsersClient usersClient;
  @Mock private FolioExecutionContext folioExecutionContext;
  @Mock private SettingsService settingsService;

  @BeforeEach
  void setUp() {
    ReflectionTestUtils.setField(service, "resetPasswordUiPathDefault", "/reset-password");
    ReflectionTestUtils.setField(service, "forgotPasswordUiPathDefault", "/forgot-password");
    when(usersClient.lookupUserById(USER_ID)).thenReturn(Optional.of(USER));
    when(settingsService.getBaseUrl()).thenReturn(Optional.empty());
    when(folioExecutionContext.getTenantId()).thenReturn("diku");
    var tokenResponse = new TokenResponse();
    tokenResponse.setAccessToken(TOKEN);
    when(resetTokenService.generateResetToken(anyString())).thenReturn(tokenResponse);
  }

  @ParameterizedTest
  @CsvSource({
    "false, CREATE_PASSWORD_EVENT",
    "true, RESET_PASSWORD_EVENT"
  })
  void sendPasswordRestLink_positive_emailContainsNonEmptyValidForgotPasswordLink(boolean passwordExists,
    String expectedEvent) {
    mockPasswordExists(passwordExists);
    mockForgotPasswordConfigs(Map.of());

    var link = service.sendPasswordRestLink(USER_ID);

    assertThat(link).isEqualTo(GENERATED_LINK);
    var forgotPasswordLink = captureForgotPasswordLink(expectedEvent);
    assertThat(forgotPasswordLink).isNotBlank();
    var uri = URI.create(forgotPasswordLink);
    assertThat(uri.getScheme()).isIn("http", "https");
    assertThat(uri.getHost()).isNotBlank();
    assertThat(uri.getPath()).isEqualTo("/forgot-password");
  }

  @Test
  void sendPasswordRestLink_positive_createPasswordEventWhenPasswordExistsIsUnknown() {
    mockPasswordExists(null);
    mockForgotPasswordConfigs(Map.of());

    service.sendPasswordRestLink(USER_ID);

    verify(notificationService).sendResetLinkNotification(USER, GENERATED_LINK, CREATE_PASSWORD_EVENT, "24", "hours",
      DEFAULT_FORGOT_PASSWORD_LINK);
  }

  @Test
  void sendPasswordRestLink_positive_defaultForgotPasswordPathUsedWhenNotConfigured() {
    mockPasswordExists(true);
    mockForgotPasswordConfigs(Map.of());

    service.sendPasswordRestLink(USER_ID);

    verify(notificationService).sendResetLinkNotification(USER, GENERATED_LINK, RESET_PASSWORD_EVENT, "24", "hours",
      DEFAULT_FORGOT_PASSWORD_LINK);
  }

  @Test
  void sendPasswordRestLink_positive_defaultPathUsedWhenOnlyHostConfigured() {
    mockPasswordExists(true);
    mockForgotPasswordConfigs(Map.of(FOLIO_HOST, "https://diku.example.org"));

    service.sendPasswordRestLink(USER_ID);

    verify(notificationService).sendResetLinkNotification(USER, GENERATED_LINK, RESET_PASSWORD_EVENT, "24", "hours",
      "https://diku.example.org/forgot-password");
  }

  @Test
  void sendPasswordRestLink_positive_configuredForgotPasswordPathUsed() {
    mockPasswordExists(false);
    mockForgotPasswordConfigs(Map.of(FORGOT_PASSWORD_UI_PATH, "/custom/forgot"));

    service.sendPasswordRestLink(USER_ID);

    verify(notificationService).sendResetLinkNotification(USER, GENERATED_LINK, CREATE_PASSWORD_EVENT, "24", "hours",
      "http://localhost:3000/custom/forgot");
  }

  @Test
  void sendPasswordRestLink_positive_configuredHostAndPathUsed() {
    mockPasswordExists(true);
    mockForgotPasswordConfigs(
      Map.of(FOLIO_HOST, "https://diku.example.org", FORGOT_PASSWORD_UI_PATH, "/custom/forgot"));

    service.sendPasswordRestLink(USER_ID);

    verify(notificationService).sendResetLinkNotification(USER, GENERATED_LINK, RESET_PASSWORD_EVENT, "24", "hours",
      "https://diku.example.org/custom/forgot");
  }

  @Test
  void sendPasswordRestLink_positive_forgotPasswordPathDefaultComesFromApplicationProperty() {
    ReflectionTestUtils.setField(service, "forgotPasswordUiPathDefault", "/property/forgot");
    mockPasswordExists(true);
    mockForgotPasswordConfigs(Map.of());

    service.sendPasswordRestLink(USER_ID);

    verify(notificationService).sendResetLinkNotification(USER, GENERATED_LINK, RESET_PASSWORD_EVENT, "24", "hours",
      "http://localhost:3000/property/forgot");
  }

  private void mockPasswordExists(Boolean passwordExists) {
    var created = new PasswordResetActionCreated();
    created.setPasswordExists(passwordExists);
    when(loginService.createPasswordResetAction(eq(USER_ID), anyLong(), anyString())).thenReturn(created);
  }

  private void mockForgotPasswordConfigs(Map<String, String> configs) {
    when(configurationService.getAllModuleConfigsValidated(anyString(), anyCollection())).thenReturn(configs);
  }

  private String captureForgotPasswordLink(String expectedEvent) {
    var captor = ArgumentCaptor.forClass(String.class);
    verify(notificationService).sendResetLinkNotification(eq(USER), eq(GENERATED_LINK), eq(expectedEvent), eq("24"),
      eq("hours"), captor.capture());
    return captor.getValue();
  }
}
