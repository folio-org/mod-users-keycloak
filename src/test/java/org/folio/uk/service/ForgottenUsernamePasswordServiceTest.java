package org.folio.uk.service;

import static org.folio.uk.service.ForgottenUsernamePasswordService.FORGOTTEN_PASSWORD_ALIASES;
import static org.folio.uk.service.ForgottenUsernamePasswordService.FORGOTTEN_USERNAME_ALIASES;
import static org.folio.uk.service.ForgottenUsernamePasswordService.MODULE_NAME_CONFIG;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.FolioModuleMetadata;
import org.folio.test.types.UnitTest;
import org.folio.uk.domain.dto.Identifier;
import org.folio.uk.domain.dto.User;
import org.folio.uk.domain.dto.UserTenant;
import org.folio.uk.domain.dto.UserTenantCollection;
import org.folio.uk.domain.dto.Users;
import org.folio.uk.integration.configuration.ConfigurationService;
import org.folio.uk.integration.notify.NotificationService;
import org.folio.uk.integration.users.UserTenantsClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@UnitTest
@ExtendWith(MockitoExtension.class)
class ForgottenUsernamePasswordServiceTest {

  private static final User TEST_USER = new User().username("test").id(UUID.randomUUID()).active(true);
  private static final UUID TEST_USER_ID = TEST_USER.getId();
  private static final String CENTRAL_TENANT = "centralTenant";
  private static final String MEMBER_TENANT = "memberTenant";
  private static final String TEST_EMAIL = "test@mail.com";
  private static final String TEST_PHONE = "1234567890";
  private static final String TEST_MOBILE = "0987654321";
  private static final Map<String, Collection<String>> OKAPI_HEADERS = Map.of(
    "x-okapi-tenant", List.of(CENTRAL_TENANT),
    "x-okapi-url", List.of("http://localhost")
  );

  @InjectMocks private ForgottenUsernamePasswordService service;
  @Mock private ConfigurationService configurationService;
  @Mock private PasswordResetService passwordResetService;
  @Mock private NotificationService notificationService;
  @Mock private UserService userService;
  @Mock private FolioExecutionContext folioExecutionContext;
  @Mock private FolioModuleMetadata folioModuleMetadata;
  @Mock private UserTenantsClient userTenantsClient;

  @Test
  void resetForgottenPassword_positive_locateUserByAvailableConfigs() {
    when(configurationService.queryModuleConfigsByCodes(MODULE_NAME_CONFIG, FORGOTTEN_PASSWORD_ALIASES))
      .thenReturn(Map.of("phoneNumber", "personal.phone"));
    when(userTenantsClient.getOne()).thenReturn(new UserTenantCollection().totalRecords(0));
    when(userService.findUsers(anyString(), anyInt()))
      .thenReturn(new Users().totalRecords(1).addUsersItem(TEST_USER));

    service.resetForgottenPassword(new Identifier().id("te*st"));

    verify(userService).findUsers(eq("personal.phone==\"te\\*st\""), eq(2));
    verify(passwordResetService).sendPasswordRestLink(eq(TEST_USER_ID));
  }

  @Test
  void resetForgottenPassword_positive_locateUserByDefaultConfigs() {
    mockSingleTenantMode(FORGOTTEN_PASSWORD_ALIASES);
    when(userService.findUsers(anyString(), anyInt()))
      .thenReturn(new Users().totalRecords(1).addUsersItem(TEST_USER));

    service.resetForgottenPassword(new Identifier().id("test"));

    verify(userService).findUsers(eq(
        "personal.email==\"test\" or personal.phone==\"test\" or personal.mobilePhone==\"test\" or username==\"test\""),
      eq(2));
    verify(passwordResetService).sendPasswordRestLink(eq(TEST_USER_ID));
  }

  @Test
  void resetForgottenPassword_negative_multipleUsersFound() {
    mockSingleTenantMode(FORGOTTEN_PASSWORD_ALIASES);
    when(userService.findUsers(anyString(), anyInt()))
      .thenReturn(new Users().totalRecords(2).addUsersItem(TEST_USER).addUsersItem(new User().username("another")));

    service.resetForgottenPassword(new Identifier().id("test"));

    verifyNoInteractions(passwordResetService);
  }

  @Test
  void resetForgottenPassword_negative_inactiveUser() {
    var inactiveUser = new User().active(false).username("test").id(UUID.randomUUID());

    mockSingleTenantMode(FORGOTTEN_PASSWORD_ALIASES);
    when(userService.findUsers(anyString(), anyInt()))
      .thenReturn(new Users().totalRecords(1).addUsersItem(inactiveUser));

    service.resetForgottenPassword(new Identifier().id("test"));

    verifyNoInteractions(passwordResetService);
  }

  @Test
  void recoverForgottenUsername_positive_locateUserByAvailableConfigs() {
    when(configurationService.queryModuleConfigsByCodes(MODULE_NAME_CONFIG, FORGOTTEN_USERNAME_ALIASES))
      .thenReturn(Map.of("email", "personal.email"));
    when(userService.findUsers(anyString(), anyInt()))
      .thenReturn(new Users().totalRecords(1).addUsersItem(TEST_USER));
    when(userTenantsClient.getOne())
      .thenReturn(new UserTenantCollection().totalRecords(0));

    service.recoverForgottenUsername(new Identifier().id("test"));

    verify(userService).findUsers(eq("personal.email==\"test\""), eq(2));
    verify(notificationService).sendLocateUserNotification(eq(TEST_USER));
  }

  @Test
  void recoverForgottenUsername_positive_locateUserByDefaultConfigs() {
    mockSingleTenantMode(FORGOTTEN_USERNAME_ALIASES);
    when(userService.findUsers(anyString(), anyInt()))
      .thenReturn(new Users().totalRecords(1).addUsersItem(TEST_USER));

    service.recoverForgottenUsername(new Identifier().id("test"));

    verify(userService).findUsers(eq(
        "personal.email==\"test\" or personal.phone==\"test\" or personal.mobilePhone==\"test\" or username==\"test\""),
      eq(2));
    verify(notificationService).sendLocateUserNotification(eq(TEST_USER));
  }

  @Test
  void recoverForgottenUsername_negative_multipleUsersFound() {
    mockSingleTenantMode(FORGOTTEN_USERNAME_ALIASES);
    when(userService.findUsers(anyString(), anyInt()))
      .thenReturn(new Users().totalRecords(2).addUsersItem(TEST_USER).addUsersItem(new User().username("another")));

    service.recoverForgottenUsername(new Identifier().id("test"));

    verifyNoInteractions(notificationService);
  }

  @Test
  void resetForgottenPassword_crossTenant_positive() {
    var canaryUser = createUserTenant(TEST_USER_ID.toString(), TEST_EMAIL, null, null, null);
    var locatedUser = createUserTenant(TEST_USER_ID.toString(), TEST_EMAIL, "testuser", TEST_PHONE, TEST_MOBILE);

    mockConsortiaMode(createCollection(canaryUser));
    lenient().when(folioExecutionContext.getTenantId()).thenReturn(CENTRAL_TENANT);
    when(userTenantsClient.getUserTenants(2, TEST_EMAIL, TEST_EMAIL, TEST_EMAIL, TEST_EMAIL))
      .thenReturn(createCollection(locatedUser));
    when(userTenantsClient.getUserTenants(2, "testuser", TEST_EMAIL, TEST_PHONE, TEST_MOBILE))
      .thenReturn(new UserTenantCollection().totalRecords(1));

    service.resetForgottenPassword(new Identifier().id(TEST_EMAIL));

    verify(passwordResetService).sendPasswordRestLink(TEST_USER_ID);
    verifyNoInteractions(notificationService);
  }

  @Test
  void recoverForgottenUsername_crossTenant_negative_multipleUsersFound() {
    var canaryUser = createUserTenant(TEST_USER_ID.toString(), null, null, null, null);

    mockConsortiaMode(createCollection(canaryUser));
    lenient().when(folioExecutionContext.getTenantId()).thenReturn(CENTRAL_TENANT);
    when(userTenantsClient.getUserTenants(2, "test", "test", "test", "test"))
      .thenReturn(new UserTenantCollection().totalRecords(2));

    service.recoverForgottenUsername(new Identifier().id("test"));

    verifyNoInteractions(notificationService);
  }

  @Test
  void resetForgottenPassword_crossTenant_negative_duplicateContacts() {
    var canaryUser = createUserTenant(TEST_USER_ID.toString(), TEST_EMAIL, null, null, null);
    var locatedUser =
      createUserTenant(TEST_USER_ID.toString(), "duplicate@mail.com", "testuser", TEST_PHONE, TEST_MOBILE);

    mockConsortiaMode(createCollection(canaryUser));
    when(userTenantsClient.getUserTenants(2, TEST_EMAIL, TEST_EMAIL, TEST_EMAIL, TEST_EMAIL))
      .thenReturn(createCollection(locatedUser));
    when(userTenantsClient.getUserTenants(2, "testuser", "duplicate@mail.com", TEST_PHONE, TEST_MOBILE))
      .thenReturn(new UserTenantCollection().totalRecords(2));

    service.resetForgottenPassword(new Identifier().id(TEST_EMAIL));

    verifyNoInteractions(passwordResetService);
    verifyNoInteractions(notificationService);
  }

  @Test
  void resetForgottenPassword_crossTenant_positive_nullContactFields() {
    var canaryUser = createUserTenant(TEST_USER_ID.toString(), TEST_EMAIL, null, null, null);
    var locatedUser = createUserTenant(TEST_USER_ID.toString(), TEST_EMAIL, null, null, null);

    mockConsortiaMode(createCollection(canaryUser));
    when(userTenantsClient.getUserTenants(2, TEST_EMAIL, TEST_EMAIL, TEST_EMAIL, TEST_EMAIL))
      .thenReturn(createCollection(locatedUser));
    when(userTenantsClient.getUserTenants(2, null, TEST_EMAIL, null, null))
      .thenReturn(new UserTenantCollection().totalRecords(1));

    service.resetForgottenPassword(new Identifier().id(TEST_EMAIL));

    verify(passwordResetService).sendPasswordRestLink(TEST_USER_ID);
    verifyNoInteractions(notificationService);
  }

  @Test
  void recoverForgottenUsername_crossTenant_negative_duplicateContacts() {
    var canaryUser = createUserTenant(TEST_USER_ID.toString(), TEST_EMAIL, null, null, null);
    var locatedUser = createUserTenant(TEST_USER_ID.toString(), TEST_EMAIL, null, TEST_PHONE, null);

    mockConsortiaMode(createCollection(canaryUser));
    when(userTenantsClient.getUserTenants(2, TEST_EMAIL, TEST_EMAIL, TEST_EMAIL, TEST_EMAIL))
      .thenReturn(createCollection(locatedUser));
    when(userTenantsClient.getUserTenants(2, null, TEST_EMAIL, TEST_PHONE, null))
      .thenReturn(new UserTenantCollection().totalRecords(2));

    service.recoverForgottenUsername(new Identifier().id(TEST_EMAIL));

    verifyNoInteractions(notificationService);
  }

  @Test
  void recoverForgottenUsername_crossTenant_positive() {
    var canaryUser = createUserTenant(TEST_USER_ID.toString(), TEST_EMAIL, null, null, null);
    var locatedUser = createUserTenant(TEST_USER_ID.toString(), TEST_EMAIL, "testuser", TEST_PHONE, null);

    mockConsortiaMode(createCollection(canaryUser));
    lenient().when(folioExecutionContext.getTenantId()).thenReturn(MEMBER_TENANT);
    when(userTenantsClient.getUserTenants(2, TEST_EMAIL, TEST_EMAIL, TEST_EMAIL, TEST_EMAIL))
      .thenReturn(createCollection(locatedUser));
    when(userTenantsClient.getUserTenants(2, "testuser", TEST_EMAIL, TEST_PHONE, null))
      .thenReturn(new UserTenantCollection().totalRecords(1));
    when(userService.getUser(TEST_USER_ID)).thenReturn(Optional.of(TEST_USER));

    service.recoverForgottenUsername(new Identifier().id(TEST_EMAIL));

    verify(notificationService).sendLocateUserNotification(TEST_USER);
  }

  @Test
  void recoverForgottenUsername_crossTenant_negative_userNotFoundInMemberTenant() {
    var canaryUser = createUserTenant(TEST_USER_ID.toString(), TEST_EMAIL, null, null, null);
    var locatedUser = createUserTenant(TEST_USER_ID.toString(), TEST_EMAIL, null, null, null);

    mockConsortiaMode(createCollection(canaryUser));
    lenient().when(folioExecutionContext.getTenantId()).thenReturn(MEMBER_TENANT);
    when(userTenantsClient.getUserTenants(2, TEST_EMAIL, TEST_EMAIL, TEST_EMAIL, TEST_EMAIL))
      .thenReturn(createCollection(locatedUser));
    when(userTenantsClient.getUserTenants(2, null, TEST_EMAIL, null, null))
      .thenReturn(new UserTenantCollection().totalRecords(1));
    when(userService.getUser(TEST_USER_ID)).thenReturn(Optional.empty());

    service.recoverForgottenUsername(new Identifier().id(TEST_EMAIL));

    verifyNoInteractions(notificationService);
  }

  @Test
  void recoverForgottenUsername_crossTenant_negative_userNotFoundInCentralTenant() {
    var canaryUser = createUserTenant(TEST_USER_ID.toString(), null, null, null, null);

    mockConsortiaMode(createCollection(canaryUser));
    when(userTenantsClient.getUserTenants(2, TEST_EMAIL, TEST_EMAIL, TEST_EMAIL, TEST_EMAIL))
      .thenReturn(new UserTenantCollection().totalRecords(0));

    service.recoverForgottenUsername(new Identifier().id(TEST_EMAIL));

    verifyNoInteractions(notificationService);
  }

  @Test
  void recoverForgottenUsername_singleTenant_negative_noUsersFound() {
    mockSingleTenantMode(FORGOTTEN_USERNAME_ALIASES);
    when(userService.findUsers(anyString(), anyInt()))
      .thenReturn(new Users().totalRecords(0));

    service.recoverForgottenUsername(new Identifier().id("test"));

    verifyNoInteractions(notificationService);
  }

  @Test
  void resetForgottenPassword_singleTenant_negative_noUsersFound() {
    mockSingleTenantMode(FORGOTTEN_PASSWORD_ALIASES);
    when(userService.findUsers(anyString(), anyInt()))
      .thenReturn(new Users().totalRecords(0));

    service.resetForgottenPassword(new Identifier().id("test"));

    verifyNoInteractions(passwordResetService);
  }

  @Test
  void getCentralTenantIdIfConsortiaMode_positive_emptyLis() {
    var emptyCollection = new UserTenantCollection()
      .totalRecords(1)
      .userTenants(Collections.emptyList());

    when(userTenantsClient.getOne()).thenReturn(emptyCollection);
    mockSingleTenantMode(FORGOTTEN_PASSWORD_ALIASES);
    when(userService.findUsers(anyString(), anyInt()))
      .thenReturn(new Users().totalRecords(1).addUsersItem(TEST_USER));

    service.resetForgottenPassword(new Identifier().id("test"));

    verify(userService).findUsers(anyString(), anyInt());
    verify(passwordResetService).sendPasswordRestLink(TEST_USER_ID);
  }

  private static UserTenant createUserTenant(String userId, String email, String username,
    String phone, String mobile) {
    return new UserTenant()
      .userId(userId)
      .email(email)
      .username(username)
      .phoneNumber(phone)
      .mobilePhoneNumber(mobile)
      .tenantId(MEMBER_TENANT)
      .centralTenantId(CENTRAL_TENANT);
  }

  private static UserTenantCollection createCollection(UserTenant... users) {
    var collection = new UserTenantCollection().totalRecords(users.length);
    for (var user : users) {
      collection.addUserTenantsItem(user);
    }
    return collection;
  }

  private void mockConsortiaMode(UserTenantCollection canaryCollection) {
    when(userTenantsClient.getOne()).thenReturn(canaryCollection);
    when(folioExecutionContext.getOkapiHeaders()).thenReturn(OKAPI_HEADERS);
  }

  private void mockSingleTenantMode(List<String> aliases) {
    when(configurationService.queryModuleConfigsByCodes(MODULE_NAME_CONFIG, aliases))
      .thenReturn(Collections.emptyMap());
    when(userTenantsClient.getOne()).thenReturn(new UserTenantCollection().totalRecords(0));
  }
}
