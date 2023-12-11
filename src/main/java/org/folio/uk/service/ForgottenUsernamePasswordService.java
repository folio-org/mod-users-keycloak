package org.folio.uk.service;

import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.MapUtils;
import org.folio.uk.domain.dto.Identifier;
import org.folio.uk.domain.dto.User;
import org.folio.uk.exception.UnprocessableEntityException;
import org.folio.uk.integration.configuration.ConfigurationService;
import org.folio.uk.integration.notify.NotificationService;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ForgottenUsernamePasswordService {
  public static final String MODULE_NAME_CONFIG = "USERSBL";
  private static final String LOCATE_USER_USERNAME = "userName";
  private static final String LOCATE_USER_PHONE_NUMBER = "phoneNumber";
  private static final String LOCATE_USER_EMAIL = "email";
  public static final List<String> FORGOTTEN_USERNAME_ALIASES =
    Arrays.asList(LOCATE_USER_PHONE_NUMBER, LOCATE_USER_EMAIL);
  public static final List<String> FORGOTTEN_PASSWORD_ALIASES =
    Arrays.asList(LOCATE_USER_USERNAME, LOCATE_USER_PHONE_NUMBER, LOCATE_USER_EMAIL);

  private static final String FORGOTTEN_USERNAME_ERROR_KEY = "forgotten.username.found.multiple.users";
  private static final String FORGOTTEN_PASSWORD_ERROR_KEY = "forgotten.password.found.multiple.users";
  private static final String FORGOTTEN_PASSWORD_FOUND_INACTIVE = "forgotten.password.found.inactive";

  private static final List<String> DEFAULT_FIELDS_TO_LOCATE_USER =
    Arrays.asList("personal.email", "personal.phone", "personal.mobilePhone", "username");

  private final ConfigurationService configurationService;
  private final PasswordResetService passwordResetService;
  private final NotificationService notificationService;
  private final UserService userService;

  public void resetForgottenPassword(Identifier identifier) {
    var user = locateUserByAlias(FORGOTTEN_PASSWORD_ALIASES, identifier, FORGOTTEN_PASSWORD_ERROR_KEY);
    passwordResetService.sendPasswordRestLink(user.getId());
  }

  public void recoverForgottenUsername(Identifier identifier) {
    var user = locateUserByAlias(FORGOTTEN_USERNAME_ALIASES, identifier, FORGOTTEN_USERNAME_ERROR_KEY);
    notificationService.sendLocateUserNotification(user);
  }

  /**
   * Maps aliases to configuration parameters.
   *
   * @param fieldAliases - a list of aliases
   * @return a list of user fields to use for search
   */
  private List<String> getLocateUserFields(List<String> fieldAliases) {
    var configsMap = configurationService.queryModuleConfigsByCodes(MODULE_NAME_CONFIG, fieldAliases);
    if (MapUtils.isEmpty(configsMap)) {
      return DEFAULT_FIELDS_TO_LOCATE_USER;
    }

    return configsMap.values().stream()
      .flatMap(s -> Stream.of(s.split("[^\\w.]+")))
      .collect(Collectors.toList());
  }

  /**
   * Locates user by the given alias.
   *
   * @param fieldAliases list of aliases to use
   * @param identifier   an identity with a value
   * @return Located user
   */
  private User locateUserByAlias(List<String> fieldAliases, Identifier identifier, String errorKey) {
    var locateUserFields = getLocateUserFields(fieldAliases);

    var query = buildQuery(locateUserFields, identifier.getId());

    var userResponse = userService.findUsers(query, 2);

    if (userResponse.getTotalRecords() == 0) {
      // mapped to an error with 400 status code instead of 404 for backward compatibility purposes with bl-users API
      throw new NoSuchElementException("User is not found: " + identifier.getId());
    } else if (userResponse.getTotalRecords() > 1) {
      var message = String.format("Multiple users associated with '%s'", identifier.getId());
      throw new UnprocessableEntityException(message, errorKey);
    }

    var user = userResponse.getUsers().get(0);
    validateActiveUser(identifier, user);

    return user;
  }

  /**
   * Builds CQL query to search a value in the given fields.
   *
   * @param fields a list of fields to be used for search
   * @param value  a value to search
   * @return CQL query value
   */
  private String buildQuery(List<String> fields, String value) {
    return fields.stream()
      .map(field -> field + "==\"" + value + "\"")
      .collect(Collectors.joining(" or "));
  }

  private static void validateActiveUser(Identifier entity, User user) {
    if (user != null && !user.getActive()) {
      var message = String.format("Users associated with '%s' is not active", entity.getId());
      throw new UnprocessableEntityException(message, FORGOTTEN_PASSWORD_FOUND_INACTIVE);
    }
  }
}
