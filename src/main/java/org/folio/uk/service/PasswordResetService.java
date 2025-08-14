package org.folio.uk.service;

import static java.lang.Boolean.parseBoolean;
import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;
import static org.folio.uk.domain.dto.ErrorCode.LINK_EXPIRED;
import static org.folio.uk.domain.dto.ErrorCode.LINK_INVALID;
import static org.folio.uk.domain.dto.ErrorCode.USER_ABSENT_USERNAME;
import static org.folio.uk.domain.dto.ErrorCode.USER_NOT_FOUND;
import static org.folio.uk.utils.DateConversionUtils.convertDateToMillisecondsOrElseThrow;

import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.folio.spring.FolioExecutionContext;
import org.folio.uk.domain.dto.User;
import org.folio.uk.domain.model.ExpirationTimeUnit;
import org.folio.uk.exception.UnprocessableEntityException;
import org.folio.uk.integration.configuration.ConfigurationService;
import org.folio.uk.integration.keycloak.PasswordResetTokenService;
import org.folio.uk.integration.login.LoginService;
import org.folio.uk.integration.login.model.PasswordResetAction;
import org.folio.uk.integration.notify.NotificationService;
import org.folio.uk.integration.users.UsersClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@Log4j2
@RequiredArgsConstructor
public class PasswordResetService {

  private static final String MODULE_NAME = "USERSBL";
  private static final String FOLIO_HOST_CONFIG_KEY = "FOLIO_HOST";
  private static final String UI_PATH_CONFIG_KEY = "RESET_PASSWORD_UI_PATH";
  private static final String LINK_EXPIRATION_TIME_CONFIG_KEY = "RESET_PASSWORD_LINK_EXPIRATION_TIME";
  private static final String LINK_EXPIRATION_UNIT_OF_TIME_CONFIG_KEY = "RESET_PASSWORD_LINK_EXPIRATION_UNIT_OF_TIME";
  private static final String PUT_TOKEN_IN_QUERY_PARAMS_CONFIG_KEY = "PUT_RESET_TOKEN_IN_QUERY_PARAMS";
  private static final Set<String> GENERATE_LINK_REQUIRED_CONFIGURATION = Collections.emptySet();
  private static final String LINK_EXPIRATION_TIME_DEFAULT = "24";
  private static final String FOLIO_HOST_DEFAULT = "http://localhost:3000";
  private static final String LINK_EXPIRATION_UNIT_OF_TIME_DEFAULT = "hours";

  private static final String CREATE_PASSWORD_EVENT_CONFIG_NAME = "CREATE_PASSWORD_EVENT";
  private static final String RESET_PASSWORD_EVENT_CONFIG_NAME = "RESET_PASSWORD_EVENT";

  private static final int MAXIMUM_EXPIRATION_TIME_IN_WEEKS = 4;
  private static final long MAXIMUM_EXPIRATION_TIME = TimeUnit.DAYS.toMillis(7) * MAXIMUM_EXPIRATION_TIME_IN_WEEKS;

  private final ConfigurationService configurationService;
  private final NotificationService notificationService;
  private final PasswordResetTokenService resetTokenService;
  private final LoginService actionService;
  private final UsersClient usersClient;
  private final LoginService loginService;
  private final FolioExecutionContext folioExecutionContext;

  @Value("${reset-password.ui-path.default:/reset-password}")
  private String resetPasswordUiPathDefault;

  public String sendPasswordRestLink(UUID userId) {
    var configMap =
      configurationService.getAllModuleConfigsValidated(MODULE_NAME, GENERATE_LINK_REQUIRED_CONFIGURATION);
    var user = lookupAndValidateUser(userId);

    ExpirationTimeRecord etr = getExpirationTime(configMap);

    var passwordResetActionId = UUID.randomUUID().toString();
    var actionResponse = actionService.createPasswordResetAction(userId, etr.expirationTime(), passwordResetActionId);
    var passwordExists = defaultIfNull(actionResponse.getPasswordExists(), false);

    var tokenResponse = resetTokenService.generateResetToken(passwordResetActionId);
    var token = tokenResponse.getAccessToken();

    var generatedLink = getGeneratedLink(configMap, token);

    var eventConfigName = passwordExists ? RESET_PASSWORD_EVENT_CONFIG_NAME : CREATE_PASSWORD_EVENT_CONFIG_NAME;

    notificationService.sendResetLinkNotification(user, generatedLink,
      eventConfigName, etr.expirationTimeFromConfig(), etr.expirationUnitOfTimeFromConfig());

    return generatedLink;
  }

  public void resetPassword(String newPassword) {
    var passwordResetActionId = resetTokenService.parsePasswordResetActionId();
    var action = actionService.getPasswordResetAction(passwordResetActionId);
    checkPasswordResetActionExpirationTime(action);
    var user = findUserByPasswordResetActionId(action);

    var resetResponse = loginService.resetPassword(newPassword, user.getId(), passwordResetActionId);
    notificationService.sendPasswordResetNotification(user, resetResponse.getIsNewPassword());
  }

  public void validateLink() {
    var passwordResetActionId = resetTokenService.parsePasswordResetActionId();
    var action = actionService.getPasswordResetAction(passwordResetActionId);
    checkPasswordResetActionExpirationTime(action);
    usersClient.lookupUserById(action.getUserId())
      .orElseThrow(() -> {
        var message = String.format("User with id = %s in not found", action.getUserId());
        return new UnprocessableEntityException(message, LINK_INVALID);
      });
  }

  private String getGeneratedLink(Map<String, String> configMap, String token) {
    var linkHost = configMap.getOrDefault(FOLIO_HOST_CONFIG_KEY, FOLIO_HOST_DEFAULT);
    var linkPath = configMap.getOrDefault(UI_PATH_CONFIG_KEY, resetPasswordUiPathDefault);
    var putTokenInQueryParams = parseBoolean(configMap.getOrDefault(PUT_TOKEN_IN_QUERY_PARAMS_CONFIG_KEY, "false"));
    var tenantId = folioExecutionContext.getTenantId();
    var template = putTokenInQueryParams ? "%s%s?resetToken=%s&tenant=%s" : "%s%s/%s?tenant=%s";
    return String.format(template, linkHost, linkPath, token, tenantId);
  }

  private User lookupAndValidateUser(UUID userId) {
    var user = usersClient.lookupUserById(userId).orElseThrow(() -> {
      var message = String.format("User with id '%s' not found", userId);
      return new UnprocessableEntityException(message, USER_NOT_FOUND);
    });

    if (StringUtils.isBlank(user.getUsername())) {
      throw new UnprocessableEntityException("User without username cannot reset password", USER_ABSENT_USERNAME);
    }
    return user;
  }

  private void checkPasswordResetActionExpirationTime(PasswordResetAction pwdResetAction) {
    var passwordResetActionId = pwdResetAction.getId();
    if (!pwdResetAction.getExpirationTime().toInstant().isAfter(Instant.now())) {
      var message = String.format("PasswordResetAction with id = %s is expired", passwordResetActionId);
      throw new UnprocessableEntityException(message, LINK_EXPIRED);
    }
  }

  private User findUserByPasswordResetActionId(PasswordResetAction passwordResetAction) {
    var userId = passwordResetAction.getUserId();
    return usersClient.lookupUserById(userId).orElseThrow(() -> {
      var message = String.format("User with id '%s' not found", userId);
      return new UnprocessableEntityException(message, LINK_INVALID);
    });
  }

  private static PasswordResetService.ExpirationTimeRecord getExpirationTime(Map<String, String> configMap) {
    var expirationTimeFromConfig =
      configMap.getOrDefault(LINK_EXPIRATION_TIME_CONFIG_KEY, LINK_EXPIRATION_TIME_DEFAULT);
    var expirationUnitOfTimeFromConfig = configMap.getOrDefault(
      LINK_EXPIRATION_UNIT_OF_TIME_CONFIG_KEY, LINK_EXPIRATION_UNIT_OF_TIME_DEFAULT);

    long expirationTime = convertDateToMillisecondsOrElseThrow(expirationTimeFromConfig, expirationUnitOfTimeFromConfig,
      () -> new UnprocessableEntityException("Can't convert time period to milliseconds", LINK_INVALID));

    if (expirationTime > MAXIMUM_EXPIRATION_TIME) {
      expirationTime = MAXIMUM_EXPIRATION_TIME;
      expirationTimeFromConfig = String.valueOf(MAXIMUM_EXPIRATION_TIME_IN_WEEKS);
      expirationUnitOfTimeFromConfig = ExpirationTimeUnit.WEEKS.name().toLowerCase();
    }
    return new ExpirationTimeRecord(expirationTime, expirationTimeFromConfig, expirationUnitOfTimeFromConfig);
  }

  private record ExpirationTimeRecord(long expirationTime, String expirationTimeFromConfig,
    String expirationUnitOfTimeFromConfig) {
  }
}
