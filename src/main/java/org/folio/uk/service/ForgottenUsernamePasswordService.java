package org.folio.uk.service;

import static java.lang.String.format;
import static org.folio.spring.utils.FolioExecutionContextUtils.prepareContextForTenant;

import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.MapUtils;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.FolioModuleMetadata;
import org.folio.spring.exception.NotFoundException;
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
      // check if we are in consortia mode by checking if user-tenants API returns any records
      var centralTenantId = getCentralTenantIdIfConsortiaMode();

      if (centralTenantId == null) {
        // single mode: use traditional user search in current tenant
        sendPasswordRestLinkByIdentifier(identifier);
        return;
      }

      // consortia mode: perform validation and send email using central tenant data
      handleConsortiaPasswordReset(identifier, centralTenantId);
    } catch (MultipleEntityException e) {
      log.warn("Multiple users found for forgotten password request, returning success without sending reset link: {}",
        e.getMessage());
    }
  }

  public void recoverForgottenUsername(Identifier identifier) {
    try {
      // check if we are in consortia mode by checking if user-tenants API returns any records
      var centralTenantId = getCentralTenantIdIfConsortiaMode();

      if (centralTenantId == null) {
        // single mode: use traditional user search in current tenant
        sendLocateUserNotificationByIdentifier(identifier);
        return;
      }

      // consortia mode: perform validation and send notification using central tenant data
      handleConsortiaUsernameRecovery(identifier, centralTenantId);
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

  /**
   * Checks if we are operating in consortia mode and returns the central tenant ID.
   *
   * <p>
   * In consortia mode, the user-tenants API will return at least one record. The central tenant ID is extracted from
   * the first record.
   *
   * @return central tenant ID if in consortia mode, null if in single mode
   */
  private String getCentralTenantIdIfConsortiaMode() {
    var response = userTenantsClient.getOne();
    if (response.getTotalRecords() == 0) {
      // single mode: user-tenants API returns no records
      return null;
    }

    // defensive check for empty list
    if (response.getUserTenants().isEmpty()) {
      return null;
    }

    // consortia mode: extract central tenant ID from any user-tenant record
    var canaryUser = response.getUserTenants().getFirst();
    return canaryUser.getCentralTenantId();
  }

  /**
   * Handles password reset in consortia mode.
   *
   * <p>
   * Finds and validates user in central tenant, then sends password reset link.
   *
   * @param identifier the identifier provided by the user
   * @param centralTenantId the central tenant ID
   * @throws MultipleEntityException if duplicates are found
   */
  private void handleConsortiaPasswordReset(Identifier identifier, String centralTenantId) {
    var userTenant = findAndValidateUserInConsortia(identifier, centralTenantId, FORGOTTEN_PASSWORD_ERROR_KEY);

    // send password reset link using the userId from UserTenant
    var userId = UUID.fromString(userTenant.getUserId());
    var userHomeTenantId = userTenant.getTenantId();

    // send email in the user's home tenant context
    try (var ignored = new FolioExecutionContextSetter(
      prepareContextForTenant(userHomeTenantId, folioModuleMetadata, folioExecutionContext))) {
      passwordResetService.sendPasswordRestLink(userId);
    }
  }

  /**
   * Handles username recovery in consortia mode.
   *
   * <p>
   * Finds and validates user in central tenant, then sends username notification.
   *
   * @param identifier the identifier provided by the user
   * @param centralTenantId the central tenant ID
   * @throws MultipleEntityException if duplicates are found
   */
  private void handleConsortiaUsernameRecovery(Identifier identifier, String centralTenantId) {
    var userTenant = findAndValidateUserInConsortia(identifier, centralTenantId, FORGOTTEN_USERNAME_ERROR_KEY);

    // send username notification using the userId from UserTenant
    var userId = UUID.fromString(userTenant.getUserId());
    var userHomeTenantId = userTenant.getTenantId();

    // send notification in the user's home tenant context
    try (var ignored = new FolioExecutionContextSetter(
      prepareContextForTenant(userHomeTenantId, folioModuleMetadata, folioExecutionContext))) {

      // look up the full User object to send notification
      var user = userService.getUser(userId)
        .orElseThrow(() -> new NoSuchElementException("User not found: " + userId));
      notificationService.sendLocateUserNotification(user);
    }
  }

  /**
   * Finds and validates a user in consortia mode using a two-step process.
   *
   * <p>
   * Step 1: Search in central tenant by provided identifier - If multiple users found → throw error (duplicate on
   * provided contact) - If no users found → throw NoSuchElementException - If one user found → proceed to Step 2
   *
   * <p>
   * Step 2: Validate all contacts of the found user are unique - If multiple users found → throw error (other contacts
   * are duplicated) - If one user found → return the UserTenant
   *
   * @param identifier the identifier provided by the user
   * @param centralTenantId the central tenant ID
   * @param errorKey the error key for exception handling
   * @return the validated UserTenant
   * @throws MultipleEntityException if duplicates are found
   * @throws NotFoundException if no user is found
   */
  private UserTenant findAndValidateUserInConsortia(Identifier identifier, String centralTenantId, String errorKey) {
    var providedId = identifier.getId();
    try (var ignored = new FolioExecutionContextSetter(
      prepareContextForTenant(centralTenantId, folioModuleMetadata, folioExecutionContext))) {

      // step 1: Search by provided identifier in all fields (OR query)
      var searchResponse = userTenantsClient.getUserTenants(2, providedId, providedId, providedId,
        providedId);

      if (searchResponse.getTotalRecords() > 1) {
        log.warn("Multiple users found with provided identifier: tenant = {}, identifier = {}",
          centralTenantId, providedId);
        throw new MultipleEntityException(format("Multiple users associated with '%s'", providedId), errorKey);
      }

      if (searchResponse.getTotalRecords() == 0) {
        log.warn("No user-tenants (consortia mode) found with provided identifier: tenant = {}, identifier = {}",
          centralTenantId, providedId);
        throw new NotFoundException("User is not found: " + providedId);
      }

      var userTenant = searchResponse.getUserTenants().getFirst();

      // step 2: Validate all contacts of the found user are unique
      validateAllContactsAreUnique(userTenant, providedId, errorKey);
      return userTenant;
    }
  }

  /**
   * Validates that all contact fields of the user are unique in the central tenant.
   *
   * <p>
   * This method must be called within the central tenant context. It searches by ALL contact fields of the user to
   * ensure no other users share any contact.
   *
   * @param userTenant the user tenant with contact information
   * @param providedIdentifier the original identifier provided by the user (for logging)
   * @param errorKey the error key for exception handling
   * @throws MultipleEntityException if any contact field is shared with other users
   */
  private void validateAllContactsAreUnique(UserTenant userTenant, String providedIdentifier, String errorKey) {
    var username = userTenant.getUsername();
    var email = userTenant.getEmail();
    var phoneNumber = userTenant.getPhoneNumber();
    var mobilePhoneNumber = userTenant.getMobilePhoneNumber();

    // search by ALL contact fields (OR query)
    var response = userTenantsClient.getUserTenants(2, username, email, phoneNumber,
      mobilePhoneNumber);

    if (response.getTotalRecords() > 1) {
      // another user shares at least one contact field with this user
      log.warn("Multiple users found with same contact information: tenant = {}, providedIdentifier = {}",
        folioExecutionContext.getTenantId(), providedIdentifier);
      var message = format("Multiple users associated with '%s'", providedIdentifier);
      throw new MultipleEntityException(message, errorKey);
    }
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
      var message = format("Multiple users associated with '%s'", identifier.getId());
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
      var message = format("Users associated with '%s' is not active", entity.getId());
      throw new UnprocessableEntityException(message, FORGOTTEN_PASSWORD_FOUND_INACTIVE);
    }
  }
}
