package org.folio.uk.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.FolioModuleMetadata;
import org.folio.test.types.UnitTest;
import org.folio.uk.domain.dto.Identifier;
import org.folio.uk.domain.dto.User;
import org.folio.uk.domain.dto.UserTenant;
import org.folio.uk.domain.dto.UserTenantCollection;
import org.folio.uk.domain.dto.Users;
import org.folio.uk.exception.UnprocessableEntityException;
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
    when(configurationService.queryModuleConfigsByCodes(ForgottenUsernamePasswordService.MODULE_NAME_CONFIG,
      ForgottenUsernamePasswordService.FORGOTTEN_PASSWORD_ALIASES))
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
    when(configurationService.queryModuleConfigsByCodes(ForgottenUsernamePasswordService.MODULE_NAME_CONFIG,
      ForgottenUsernamePasswordService.FORGOTTEN_PASSWORD_ALIASES))
      .thenReturn(Collections.emptyMap());
    when(userTenantsClient.getOne()).thenReturn(new UserTenantCollection().totalRecords(0));
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
    when(configurationService.queryModuleConfigsByCodes(ForgottenUsernamePasswordService.MODULE_NAME_CONFIG,
      ForgottenUsernamePasswordService.FORGOTTEN_PASSWORD_ALIASES))
      .thenReturn(Collections.emptyMap());
    when(userTenantsClient.getOne()).thenReturn(new UserTenantCollection().totalRecords(0));
    when(userService.findUsers(anyString(), anyInt()))
      .thenReturn(new Users().totalRecords(2).addUsersItem(TEST_USER).addUsersItem(new User().username("another")));

    service.resetForgottenPassword(new Identifier().id("test"));

    verifyNoInteractions(passwordResetService);
  }

  @Test
  void resetForgottenPassword_negative_inactiveUser() {
    var inactiveUser = new User().active(false).username("test").id(UUID.randomUUID());

    when(configurationService.queryModuleConfigsByCodes(ForgottenUsernamePasswordService.MODULE_NAME_CONFIG,
      ForgottenUsernamePasswordService.FORGOTTEN_PASSWORD_ALIASES))
      .thenReturn(Collections.emptyMap());
    when(userService.findUsers(anyString(), anyInt()))
      .thenReturn(
        new Users().totalRecords(1).addUsersItem(inactiveUser));
    when(userTenantsClient.getOne())
      .thenReturn(new UserTenantCollection().totalRecords(0));

    assertThatThrownBy(() -> service.resetForgottenPassword(new Identifier().id("test")))
      .isInstanceOf(UnprocessableEntityException.class);

    verifyNoInteractions(passwordResetService);
  }

  @Test
  void recoverForgottenUsername_positive_locateUserByAvailableConfigs() {
    when(configurationService.queryModuleConfigsByCodes(ForgottenUsernamePasswordService.MODULE_NAME_CONFIG,
      ForgottenUsernamePasswordService.FORGOTTEN_USERNAME_ALIASES))
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
    when(configurationService.queryModuleConfigsByCodes(ForgottenUsernamePasswordService.MODULE_NAME_CONFIG,
      ForgottenUsernamePasswordService.FORGOTTEN_USERNAME_ALIASES))
      .thenReturn(Collections.emptyMap());
    when(userService.findUsers(anyString(), anyInt()))
      .thenReturn(new Users().totalRecords(1).addUsersItem(TEST_USER));
    when(userTenantsClient.getOne())
      .thenReturn(new UserTenantCollection().totalRecords(0));

    service.recoverForgottenUsername(new Identifier().id("test"));

    verify(userService).findUsers(eq(
        "personal.email==\"test\" or personal.phone==\"test\" or personal.mobilePhone==\"test\" or username==\"test\""),
      eq(2));
    verify(notificationService).sendLocateUserNotification(eq(TEST_USER));
  }

  @Test
  void recoverForgottenUsername_negative_multipleUsersFound() {
    when(configurationService.queryModuleConfigsByCodes(ForgottenUsernamePasswordService.MODULE_NAME_CONFIG,
      ForgottenUsernamePasswordService.FORGOTTEN_USERNAME_ALIASES))
      .thenReturn(Collections.emptyMap());
    when(userTenantsClient.getOne()).thenReturn(new UserTenantCollection().totalRecords(0));
    when(userService.findUsers(anyString(), anyInt()))
      .thenReturn(new Users().totalRecords(2).addUsersItem(TEST_USER).addUsersItem(new User().username("another")));

    service.recoverForgottenUsername(new Identifier().id("test"));

    verifyNoInteractions(notificationService);
  }

  @Test
  void resetForgottenPassword_crossTenant_positive() {
    var canaryUser = new UserTenant()
      .userId(TEST_USER_ID.toString())
      .email("test@mail.com")
      .tenantId("memberTenant")
      .centralTenantId("centralTenant");
    var canaryCollection = new UserTenantCollection()
      .addUserTenantsItem(canaryUser)
      .totalRecords(1);
    var locatedUserTenant = new UserTenant()
      .userId(TEST_USER_ID.toString())
      .email("test@mail.com")
      .username("testuser")
      .phoneNumber("1234567890")
      .mobilePhoneNumber("0987654321")
      .tenantId("memberTenant")
      .centralTenantId("centralTenant");
    var locatedCollection = new UserTenantCollection()
      .addUserTenantsItem(locatedUserTenant)
      .totalRecords(1);

    when(userTenantsClient.getOne()).thenReturn(canaryCollection);
    lenient().when(folioExecutionContext.getTenantId()).thenReturn("centralTenant");
    when(folioExecutionContext.getOkapiHeaders())
      .thenReturn(Map.of("x-okapi-tenant", List.of("centralTenant"), "x-okapi-url", List.of("http://localhost")));
    when(userTenantsClient.getUserTenants(2, "test@mail.com", "test@mail.com", "test@mail.com", "test@mail.com"))
      .thenReturn(locatedCollection);
    when(userTenantsClient.getUserTenants(2, "testuser", "test@mail.com", "1234567890", "0987654321"))
      .thenReturn(new UserTenantCollection().totalRecords(1));

    service.resetForgottenPassword(new Identifier().id("test@mail.com"));

    verify(passwordResetService).sendPasswordRestLink(TEST_USER_ID);
    verifyNoInteractions(notificationService);
  }

  @Test
  void recoverForgottenUsername_crossTenant_negative_multipleUsersFound() {
    var canaryUser = new UserTenant()
      .userId(TEST_USER_ID.toString())
      .tenantId("memberTenant")
      .centralTenantId("centralTenant");
    var canaryCollection = new UserTenantCollection()
      .addUserTenantsItem(canaryUser)
      .totalRecords(1);

    when(userTenantsClient.getOne()).thenReturn(canaryCollection);
    lenient().when(folioExecutionContext.getTenantId()).thenReturn("centralTenant");
    when(folioExecutionContext.getOkapiHeaders())
      .thenReturn(Map.of("x-okapi-tenant", List.of("centralTenant"), "x-okapi-url", List.of("http://localhost")));
    when(userTenantsClient.getUserTenants(2, "test", "test", "test", "test"))
      .thenReturn(new UserTenantCollection().totalRecords(2));

    service.recoverForgottenUsername(new Identifier().id("test"));

    verifyNoInteractions(notificationService);
  }

  @Test
  void resetForgottenPassword_crossTenant_negative_duplicateContacts() {
    var canaryUser = new UserTenant()
      .userId(TEST_USER_ID.toString())
      .email("test@mail.com")
      .tenantId("memberTenant")
      .centralTenantId("centralTenant");
    var canaryCollection = new UserTenantCollection()
      .addUserTenantsItem(canaryUser)
      .totalRecords(1);
    var locatedUserTenant = new UserTenant()
      .userId(TEST_USER_ID.toString())
      .username("testuser")
      .email("duplicate@mail.com")
      .phoneNumber("1234567890")
      .mobilePhoneNumber("0987654321")
      .tenantId("memberTenant")
      .centralTenantId("centralTenant");
    var locatedCollection = new UserTenantCollection()
      .addUserTenantsItem(locatedUserTenant)
      .totalRecords(1);

    when(userTenantsClient.getOne()).thenReturn(canaryCollection);
    when(folioExecutionContext.getOkapiHeaders())
      .thenReturn(Map.of("x-okapi-tenant", List.of("centralTenant"), "x-okapi-url", List.of("http://localhost")));
    when(userTenantsClient.getUserTenants(2, "test@mail.com", "test@mail.com", "test@mail.com", "test@mail.com"))
      .thenReturn(locatedCollection);
    when(userTenantsClient.getUserTenants(2, "testuser", "duplicate@mail.com", "1234567890", "0987654321"))
      .thenReturn(new UserTenantCollection().totalRecords(2));

    service.resetForgottenPassword(new Identifier().id("test@mail.com"));

    verifyNoInteractions(passwordResetService);
    verifyNoInteractions(notificationService);
  }

  @Test
  void resetForgottenPassword_crossTenant_positive_nullContactFields() {
    var canaryUser = new UserTenant()
      .userId(TEST_USER_ID.toString())
      .email("test@mail.com")
      .tenantId("memberTenant")
      .centralTenantId("centralTenant");
    var canaryCollection = new UserTenantCollection()
      .addUserTenantsItem(canaryUser)
      .totalRecords(1);
    var locatedUserTenant = new UserTenant()
      .userId(TEST_USER_ID.toString())
      .email("test@mail.com")
      .tenantId("memberTenant")
      .centralTenantId("centralTenant");
    var locatedCollection = new UserTenantCollection()
      .addUserTenantsItem(locatedUserTenant)
      .totalRecords(1);

    when(userTenantsClient.getOne()).thenReturn(canaryCollection);
    when(folioExecutionContext.getOkapiHeaders())
      .thenReturn(Map.of("x-okapi-tenant", List.of("centralTenant"), "x-okapi-url", List.of("http://localhost")));
    when(userTenantsClient.getUserTenants(2, "test@mail.com", "test@mail.com", "test@mail.com", "test@mail.com"))
      .thenReturn(locatedCollection);
    when(userTenantsClient.getUserTenants(2, null, "test@mail.com", null, null))
      .thenReturn(new UserTenantCollection().totalRecords(1));

    service.resetForgottenPassword(new Identifier().id("test@mail.com"));

    verify(passwordResetService).sendPasswordRestLink(TEST_USER_ID);
    verifyNoInteractions(notificationService);
  }

  @Test
  void recoverForgottenUsername_crossTenant_negative_duplicateContacts() {
    var canaryUser = new UserTenant()
      .userId(TEST_USER_ID.toString())
      .email("test@mail.com")
      .tenantId("memberTenant")
      .centralTenantId("centralTenant");
    var canaryCollection = new UserTenantCollection()
      .addUserTenantsItem(canaryUser)
      .totalRecords(1);
    var locatedUserTenant = new UserTenant()
      .userId(TEST_USER_ID.toString())
      .email("test@mail.com")
      .phoneNumber("1234567890")
      .tenantId("memberTenant")
      .centralTenantId("centralTenant");
    var locatedCollection = new UserTenantCollection()
      .addUserTenantsItem(locatedUserTenant)
      .totalRecords(1);

    when(userTenantsClient.getOne()).thenReturn(canaryCollection);
    when(folioExecutionContext.getOkapiHeaders())
      .thenReturn(Map.of("x-okapi-tenant", List.of("centralTenant"), "x-okapi-url", List.of("http://localhost")));
    when(userTenantsClient.getUserTenants(2, "test@mail.com", "test@mail.com", "test@mail.com", "test@mail.com"))
      .thenReturn(locatedCollection);
    when(userTenantsClient.getUserTenants(2, null, "test@mail.com", "1234567890", null))
      .thenReturn(new UserTenantCollection().totalRecords(2));

    service.recoverForgottenUsername(new Identifier().id("test@mail.com"));

    verifyNoInteractions(notificationService);
  }
}
