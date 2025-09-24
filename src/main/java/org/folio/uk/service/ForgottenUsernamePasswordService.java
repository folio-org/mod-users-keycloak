package org.folio.uk.service;

import static org.folio.spring.utils.FolioExecutionContextUtils.prepareContextForTenant;

import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.MapUtils;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.FolioModuleMetadata;
import org.folio.spring.scope.FolioExecutionContextSetter;
import org.folio.uk.domain.dto.Identifier;
import org.folio.uk.domain.dto.User;
import org.folio.uk.domain.dto.UserTenant;
import org.folio.uk.exception.MultipleEntityException;
import org.folio.uk.exception.UnprocessableEntityException;
import org.folio.uk.integration.configuration.ConfigurationService;
import org.folio.uk.integration.notify.NotificationService;
import org.folio.uk.integration.users.UserTenantsClient;
import org.folio.util.StringUtil;
import org.springframework.stereotype.Service;

@Log4j2
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
  private final UserTenantsClient userTenantsClient;
  private final FolioModuleMetadata folioModuleMetadata;
  private final FolioExecutionContext folioExecutionContext;

  public void resetForgottenPassword(Identifier identifier) {
    try {
      var userTenant = locateUserByAliasCrossTenant(identifier, FORGOTTEN_PASSWORD_ERROR_KEY);
      if (userTenant == null) {
        sendPasswordRestLinkByIdentifier(identifier);
        return;
      }
      try (var ignored = new FolioExecutionContextSetter(
        prepareContextForTenant(userTenant.getTenantId(), folioModuleMetadata, folioExecutionContext))) {
        sendPasswordRestLinkByIdentifier(identifier);
      }
    } catch (MultipleEntityException e) {
      log.warn("Multiple users found for forgotten password request, returning success without sending reset link: {}",
        e.getMessage());
    }
  }

  public void recoverForgottenUsername(Identifier identifier) {
    try {
      var userTenant = locateUserByAliasCrossTenant(identifier, FORGOTTEN_USERNAME_ERROR_KEY);
      if (userTenant == null) {
        sendLocateUserNotificationByIdentifier(identifier);
        return;
      }
      try (var ignored = new FolioExecutionContextSetter(
        prepareContextForTenant(userTenant.getTenantId(), folioModuleMetadata, folioExecutionContext))) {
        sendLocateUserNotificationByIdentifier(identifier);
      }
    } catch (MultipleEntityException e) {
      log.warn(
        "Multiple users found for forgotten username request, returning success without sending notification: {}",
        e.getMessage());
    }
  }

  private void sendLocateUserNotificationByIdentifier(Identifier identifier) {
    var user = locateUserByAlias(FORGOTTEN_USERNAME_ALIASES, identifier, FORGOTTEN_USERNAME_ERROR_KEY);
    notificationService.sendLocateUserNotification(user);
  }

  private void sendPasswordRestLinkByIdentifier(Identifier identifier) {
    var user = locateUserByAlias(FORGOTTEN_PASSWORD_ALIASES, identifier, FORGOTTEN_PASSWORD_ERROR_KEY);
    passwordResetService.sendPasswordRestLink(user.getId());
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

  private UserTenant locateUserByAliasCrossTenant(Identifier identifier, String errorKey) {
    var response = userTenantsClient.getOne();
    if (isConsortiaModeNotActive(response)) { //check if consortia mode is not active
      return null;
    }

    var canaryUser = response.getUserTenants().getFirst();
    return getUserTenant(identifier, errorKey, canaryUser);
  }

  /**
   * Retrieves the appropriate {@link UserTenant} based on the current tenant context.
   *
   *<p>
   * If the current tenant is the central tenant, attempts to find a user tenant by the given identifier. Throws
   * {@link MultipleEntityException} if multiple user tenants are found. Returns {@code null} if no user tenants are
   * found. If the current tenant is a member tenant, returns the provided canary user.
   *
   * @param identifier the identifier used to locate the user tenant
   * @param errorKey the error key for exception handling
   * @param canaryUser a user tenant used to determine tenant type and as a fallback
   * @return the located {@link UserTenant}, or {@code null} if not found
   * @throws MultipleEntityException if multiple user tenants are found for the identifier
   */
  private UserTenant getUserTenant(Identifier identifier, String errorKey, UserTenant canaryUser) {
    // if any user's centralTenant is current tenantId from folioExecutionContext - it is central tenant
    // else - it is member tenant
    if (isCentralTenant(canaryUser)) {
      var id = identifier.getId();
      var userTenantResponse = userTenantsClient.getUserTenants(2, id, id, id);

      if (userTenantResponse.getTotalRecords() > 1) {
        log.warn("Multiple users found: tenant = {}, id = {} ", folioExecutionContext.getTenantId(),
          identifier.getId());
        var message = String.format("Multiple users associated with '%s'", identifier.getId());
        throw new MultipleEntityException(message, errorKey);
      }

      if (userTenantResponse.getTotalRecords() == 0) {
        return null;
      }

      return userTenantResponse.getUserTenants().getFirst();
    } else {
      return canaryUser;
    }
  }

  private static boolean isConsortiaModeNotActive(org.folio.uk.domain.dto.UserTenantCollection tenantCollection) {
    return tenantCollection.getTotalRecords() == 0;
  }

  private boolean isCentralTenant(UserTenant canaryUser) {
    return Objects.equals(folioExecutionContext.getTenantId(), canaryUser.getCentralTenantId());
  }

  /**
   * Locates user by the given alias.
   *
   * @param fieldAliases list of aliases to use
   * @param identifier an identity with a value
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
      throw new MultipleEntityException(message, errorKey);
    }

    var user = userResponse.getUsers().get(0);
    validateActiveUser(identifier, user);

    return user;
  }

  /**
   * Builds CQL query to search a value in the given fields.
   *
   * @param fields a list of fields to be used for search
   * @param value a value to search
   * @return CQL query value
   */
  private String buildQuery(List<String> fields, String value) {
    var equalsValue = "==" + StringUtil.cqlEncode(value);
    return fields.stream()
      .map(field -> field + equalsValue)
      .collect(Collectors.joining(" or "));
  }

  private static void validateActiveUser(Identifier entity, User user) {
    if (user != null && !user.getActive()) {
      var message = String.format("Users associated with '%s' is not active", entity.getId());
      throw new UnprocessableEntityException(message, FORGOTTEN_PASSWORD_FOUND_INACTIVE);
    }
  }
}
