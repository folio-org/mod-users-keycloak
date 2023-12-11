package org.folio.uk.controller;

import static org.folio.test.TestUtils.OBJECT_MAPPER;
import static org.folio.test.TestUtils.asJsonString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.SneakyThrows;
import org.apache.commons.lang3.time.DateUtils;
import org.folio.spring.FolioExecutionContext;
import org.folio.test.types.UnitTest;
import org.folio.uk.domain.dto.PasswordReset;
import org.folio.uk.domain.dto.User;
import org.folio.uk.integration.configuration.ConfigurationClient;
import org.folio.uk.integration.configuration.ConfigurationService;
import org.folio.uk.integration.keycloak.KeycloakClient;
import org.folio.uk.integration.keycloak.PasswordResetTokenService;
import org.folio.uk.integration.keycloak.RealmConfigurationProvider;
import org.folio.uk.integration.keycloak.config.KeycloakPasswordResetClientProperties;
import org.folio.uk.integration.login.LoginClient;
import org.folio.uk.integration.login.LoginService;
import org.folio.uk.integration.login.model.PasswordResetAction;
import org.folio.uk.integration.login.model.PasswordResetResponse;
import org.folio.uk.integration.notify.NotificationClient;
import org.folio.uk.integration.notify.NotificationService;
import org.folio.uk.integration.password.PasswordValidationService;
import org.folio.uk.integration.password.PasswordValidatorClient;
import org.folio.uk.integration.password.model.PasswordValidationResult;
import org.folio.uk.integration.users.UsersClient;
import org.folio.uk.service.PasswordResetService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@UnitTest
@WebMvcTest(PasswordResetController.class)
@Import({ControllerTestConfiguration.class, PasswordResetController.class, PasswordResetService.class,
  PasswordResetTokenService.class, NotificationService.class, PasswordValidationService.class,
  ConfigurationService.class, LoginService.class})
class PasswordResetControllerTest {

  private static final String PASSWORD_RESET_ACTION_ID = "5ac3b82d-a7d4-43a0-8285-104e84e01274";
  private static final String JWT_TOKEN_PATTERN = "%s.%s.%s";
  private static final String JWT_TOKEN_HEADER = "ZHVtbXlKd3Q=";
  private static final String JWT_TOKEN_SIGNATURE = "c2ln";
  private static final User TEST_USER = new User().id(UUID.randomUUID()).username("test");
  private static final UUID TEST_USER_ID = TEST_USER.getId();

  @Autowired private MockMvc mockMvc;
  @MockBean private UsersClient usersClient;
  @MockBean private LoginClient loginClient;
  @MockBean private ConfigurationClient configurationClient;
  @MockBean private KeycloakClient keycloakClient;
  @MockBean private NotificationClient notificationClient;
  @MockBean private PasswordValidatorClient passwordValidatorClient;
  @MockBean private FolioExecutionContext folioExecutionContext;
  @MockBean private KeycloakPasswordResetClientProperties resetPasswordClientProperties;
  @MockBean private RealmConfigurationProvider realmConfigurationProvider;

  @Test
  public void postPasswordResetValidate() throws Exception {
    var passwordResetAction = new PasswordResetAction().withId(UUID.randomUUID().toString())
      .withUserId(TEST_USER_ID)
      .withExpirationTime(DateUtils.addDays(new Date(), 2));

    when(folioExecutionContext.getToken()).thenReturn(buildToken(PASSWORD_RESET_ACTION_ID));
    when(usersClient.lookupUserById(any())).thenReturn(Optional.of(TEST_USER));
    when(loginClient.getPasswordResetAction(eq(PASSWORD_RESET_ACTION_ID))).thenReturn(Optional.of(passwordResetAction));

    mockMvc.perform(post("/users-keycloak/password-reset/validate"))
      .andExpect(status().isNoContent());
  }

  @Test
  public void postPasswordResetValidateExpiredAction() throws Exception {
    var passwordResetAction = new PasswordResetAction().withId(UUID.randomUUID().toString())
      .withUserId(TEST_USER_ID)
      .withExpirationTime(DateUtils.addDays(new Date(), -2));

    when(folioExecutionContext.getToken()).thenReturn(buildToken(PASSWORD_RESET_ACTION_ID));
    when(usersClient.lookupUserById(any())).thenReturn(Optional.of(TEST_USER));
    when(loginClient.getPasswordResetAction(eq(PASSWORD_RESET_ACTION_ID))).thenReturn(Optional.of(passwordResetAction));

    mockMvc.perform(post("/users-keycloak/password-reset/validate"))
      .andExpect(status().isUnprocessableEntity());
  }

  @Test
  public void postPasswordResetValidateNonexistentAction() throws Exception {
    when(folioExecutionContext.getToken()).thenReturn(buildToken(PASSWORD_RESET_ACTION_ID));
    when(usersClient.lookupUserById(any())).thenReturn(Optional.of(TEST_USER));
    when(loginClient.getPasswordResetAction(any())).thenReturn(Optional.empty());

    mockMvc.perform(post("/users-keycloak/password-reset/validate"))
      .andExpect(status().isUnprocessableEntity());
  }

  @Test
  public void postPasswordResetValidateUserNotFound() throws Exception {
    when(folioExecutionContext.getToken()).thenReturn(buildToken(PASSWORD_RESET_ACTION_ID));
    when(usersClient.lookupUserById(any())).thenReturn(Optional.empty());

    mockMvc.perform(post("/users-keycloak/password-reset/validate"))
      .andExpect(status().isUnprocessableEntity());
  }

  @Test
  public void postPasswordReset() throws Exception {
    var passwordResetAction = new PasswordResetAction().withId(PASSWORD_RESET_ACTION_ID)
      .withUserId(TEST_USER_ID)
      .withExpirationTime(DateUtils.addDays(new Date(), 2));

    when(folioExecutionContext.getToken()).thenReturn(buildToken(PASSWORD_RESET_ACTION_ID));
    when(usersClient.lookupUserById(any())).thenReturn(Optional.of(TEST_USER));
    when(loginClient.getPasswordResetAction(eq(PASSWORD_RESET_ACTION_ID))).thenReturn(Optional.of(passwordResetAction));
    var resetActionResponse = new PasswordResetResponse();
    resetActionResponse.setIsNewPassword(false);
    when(loginClient.resetPassword(any())).thenReturn(resetActionResponse);
    when(passwordValidatorClient.validateNewPassword(any())).thenReturn(new PasswordValidationResult());

    mockMvc.perform(post("/users-keycloak/password-reset/reset")
        .content(asJsonString(new PasswordReset().newPassword("1q2w3E!190")))
        .contentType(MediaType.APPLICATION_JSON))
      .andExpect(status().isNoContent());
  }

  @Test
  public void postPasswordResetIncorrectPassword() throws Exception {
    var passwordResetAction = new PasswordResetAction().withId(PASSWORD_RESET_ACTION_ID)
      .withUserId(TEST_USER_ID)
      .withExpirationTime(DateUtils.addDays(new Date(), 2));

    when(folioExecutionContext.getToken()).thenReturn(buildToken(PASSWORD_RESET_ACTION_ID));
    when(usersClient.lookupUserById(any())).thenReturn(Optional.of(TEST_USER));
    when(loginClient.getPasswordResetAction(eq(PASSWORD_RESET_ACTION_ID))).thenReturn(Optional.of(passwordResetAction));
    var resetActionResponse = new PasswordResetResponse();
    resetActionResponse.setIsNewPassword(false);
    when(loginClient.resetPassword(any())).thenReturn(resetActionResponse);
    var validationResult = new PasswordValidationResult();
    validationResult.setMessages(List.of("Invalid password"));
    when(passwordValidatorClient.validateNewPassword(any())).thenReturn(validationResult);

    mockMvc.perform(post("/users-keycloak/password-reset/reset")
        .content(asJsonString(new PasswordReset().newPassword("1q2w3E!190ggggg")))
        .contentType(MediaType.APPLICATION_JSON))
      .andExpect(status().isUnprocessableEntity());
  }

  @Test
  public void postPasswordResetWithIncorrectUser() throws Exception {
    var passwordResetAction = new PasswordResetAction().withId(PASSWORD_RESET_ACTION_ID)
      .withUserId(TEST_USER_ID)
      .withExpirationTime(DateUtils.addDays(new Date(), 2));

    when(folioExecutionContext.getToken()).thenReturn(buildToken(PASSWORD_RESET_ACTION_ID));
    when(loginClient.getPasswordResetAction(eq(PASSWORD_RESET_ACTION_ID))).thenReturn(Optional.of(passwordResetAction));
    when(usersClient.lookupUserById(any())).thenReturn(Optional.empty());
    var validationResult = new PasswordValidationResult();
    validationResult.setMessages(List.of("Invalid password"));

    mockMvc.perform(post("/users-keycloak/password-reset/reset")
        .content(asJsonString(new PasswordReset().newPassword("1q2w3E!190ggggg")))
        .contentType(MediaType.APPLICATION_JSON))
      .andExpect(status().isUnprocessableEntity());
  }

  @Test
  public void postPasswordResetWithIncorrectToken() throws Exception {
    var passwordResetAction = new PasswordResetAction().withId(PASSWORD_RESET_ACTION_ID)
      .withUserId(TEST_USER_ID)
      .withExpirationTime(DateUtils.addDays(new Date(), 2));

    when(folioExecutionContext.getToken()).thenReturn(buildIncorrectToken());
    when(loginClient.getPasswordResetAction(eq(PASSWORD_RESET_ACTION_ID))).thenReturn(Optional.of(passwordResetAction));
    when(usersClient.lookupUserById(any())).thenReturn(Optional.empty());
    var validationResult = new PasswordValidationResult();
    validationResult.setMessages(List.of("Invalid password"));

    mockMvc.perform(post("/users-keycloak/password-reset/reset")
        .content(asJsonString(new PasswordReset().newPassword("1q2w3E!190ggggg")))
        .contentType(MediaType.APPLICATION_JSON))
      .andExpect(status().isUnprocessableEntity());
  }

  @Test
  public void postPasswordResetNonexistentAction() throws Exception {
    when(folioExecutionContext.getToken()).thenReturn(buildToken(PASSWORD_RESET_ACTION_ID));
    when(usersClient.lookupUserById(any())).thenReturn(Optional.of(TEST_USER));
    when(loginClient.getPasswordResetAction(any())).thenReturn(Optional.empty());
    var resetActionResponse = new PasswordResetResponse();
    resetActionResponse.setIsNewPassword(false);
    when(loginClient.resetPassword(any())).thenReturn(resetActionResponse);
    when(passwordValidatorClient.validateNewPassword(any())).thenReturn(new PasswordValidationResult());

    mockMvc.perform(post("/users-keycloak/password-reset/reset")
        .content(asJsonString(new PasswordReset().newPassword("1q2w3E!190")))
        .contentType(MediaType.APPLICATION_JSON))
      .andExpect(status().isUnprocessableEntity());
  }

  @Test
  public void postPasswordResetExpiredAction() throws Exception {
    var passwordResetAction = new PasswordResetAction().withId(PASSWORD_RESET_ACTION_ID)
      .withUserId(TEST_USER_ID)
      .withExpirationTime(DateUtils.addDays(new Date(), -2));

    when(usersClient.lookupUserById(any())).thenReturn(Optional.of(TEST_USER));
    when(loginClient.getPasswordResetAction(eq(PASSWORD_RESET_ACTION_ID))).thenReturn(Optional.of(passwordResetAction));
    var resetActionResponse = new PasswordResetResponse();
    resetActionResponse.setIsNewPassword(false);
    when(loginClient.resetPassword(any())).thenReturn(resetActionResponse);
    when(passwordValidatorClient.validateNewPassword(any())).thenReturn(new PasswordValidationResult());
    when(folioExecutionContext.getToken()).thenReturn(buildToken(PASSWORD_RESET_ACTION_ID));

    mockMvc.perform(post("/users-keycloak/password-reset/reset")
        .content(asJsonString(new PasswordReset().newPassword("1q2w3E!190")))
        .contentType(MediaType.APPLICATION_JSON))
      .andExpect(status().isUnprocessableEntity());
  }

  @SneakyThrows
  private static String buildToken(String passwordResetActionId) {
    var payload = OBJECT_MAPPER.writeValueAsString(
      Map.of("sub", "reset-password-client", "passwordResetActionId", passwordResetActionId));
    byte[] bytes = payload.getBytes(StandardCharsets.UTF_8);
    return String.format(JWT_TOKEN_PATTERN, JWT_TOKEN_HEADER, Base64.getEncoder().encodeToString(bytes),
      JWT_TOKEN_SIGNATURE);
  }

  @SneakyThrows
  private String buildIncorrectToken() {
    var payload = OBJECT_MAPPER.writeValueAsString(
      Map.of("sub", "reset-password-client"));
    byte[] bytes = payload.getBytes(StandardCharsets.UTF_8);
    return String.format(JWT_TOKEN_PATTERN, JWT_TOKEN_HEADER, Base64.getEncoder().encodeToString(bytes),
      JWT_TOKEN_SIGNATURE);
  }
}
