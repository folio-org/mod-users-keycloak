package org.folio.uk.integration.keycloak;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.folio.integration.kafka.model.ResourceEventType.CREATE;
import static org.folio.integration.kafka.model.ResourceEventType.DELETE;
import static org.folio.integration.kafka.model.ResourceEventType.UPDATE;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.folio.integration.kafka.consumer.confirmation.ResourceResultEventPublisher;
import org.folio.spring.FolioExecutionContext;
import org.folio.test.types.UnitTest;
import org.folio.uk.configuration.SystemUserConfigurationProperties;
import org.folio.uk.domain.dto.Personal;
import org.folio.uk.domain.dto.User;
import org.folio.uk.domain.dto.Users;
import org.folio.uk.integration.kafka.model.SystemUserEvent;
import org.folio.uk.integration.keycloak.model.KeycloakUser;
import org.folio.uk.integration.roles.dafaultrole.DefaultSystemUserRoleService;
import org.folio.uk.service.UserService;
import org.folio.uk.support.TestConstants;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;

@UnitTest
@ExtendWith(MockitoExtension.class)
class SystemUserServiceTest {

  private static final String TENANT = "test";
  private static final String USERNAME = "test-system-user";
  private static final String MODULE_SYSTEM_USERNAME = "mod-foo";
  private static final String MODULE_ID = "mod-foo-1.0.0";
  private static final String SYSTEM_USER_PASSWORD = "system-user-password";
  private static final String KEYCLOAK_USER_ID = UUID.randomUUID().toString();
  private static final String SYSTEM_ROLE = "System";
  private static final UUID CAPABILITY_ID = UUID.randomUUID();
  private static final String PERMISSION = "foo.bar";

  @InjectMocks private SystemUserService systemUserService;

  @Mock private UserService userService;
  @Mock private DefaultSystemUserRoleService defaultSystemUserRoleService;
  @Mock private KeycloakService keycloakService;
  @Mock private FolioExecutionContext folioExecutionContext;
  @Mock private SystemUserPasswordService systemUserPasswordService;
  @Mock private ResourceResultEventPublisher eventPublisher;

  @Spy private final SystemUserConfigurationProperties userConfiguration = new SystemUserConfigurationProperties();
  @Captor private ArgumentCaptor<User> userCaptor;
  @Captor private ArgumentCaptor<String> passwordCaptor;

  @AfterEach
  void tearDown() {
    verifyNoMoreInteractions(userService, keycloakService, systemUserPasswordService, eventPublisher);
  }

  @Test
  void create_positive_freshSetup() {
    when(folioExecutionContext.getTenantId()).thenReturn(TENANT);
    givenSystemUserPassword(USERNAME);
    when(userService.createUserSafe(userCaptor.capture(), passwordCaptor.capture(), eq(false))).then(firstArg());
    when(keycloakService.findUserByUsername(USERNAME)).thenReturn(Optional.of(keycloakUser()));
    when(keycloakService.hasRole(KEYCLOAK_USER_ID, SYSTEM_ROLE)).thenReturn(false);

    systemUserService.create();

    verify(userConfiguration).getSystemUserRole();
    verify(userConfiguration).getEmailTemplate();
    verify(userConfiguration).getUsernameTemplate();

    assertThat(passwordCaptor.getValue()).isEqualTo(SYSTEM_USER_PASSWORD);
    assertThat(userCaptor.getValue()).usingRecursiveComparison().ignoringFields("id").isEqualTo(systemUser());
    assertThat(userCaptor.getValue().getId()).isNotNull();

    verify(systemUserPasswordService).getOrCreatePassword(TENANT, USERNAME);
    verify(keycloakService).assignRole(KEYCLOAK_USER_ID, SYSTEM_ROLE);
  }

  @Test
  void create_positive_userExistsAndRoleNotFound() {
    when(folioExecutionContext.getTenantId()).thenReturn(TENANT);
    givenSystemUserPassword(USERNAME);
    when(userService.createUserSafe(userCaptor.capture(), passwordCaptor.capture(), eq(false))).then(firstArg());
    when(keycloakService.findUserByUsername(USERNAME)).thenReturn(Optional.of(keycloakUser()));
    when(keycloakService.hasRole(KEYCLOAK_USER_ID, SYSTEM_ROLE)).thenReturn(false);

    systemUserService.create();

    assertThat(passwordCaptor.getValue()).isEqualTo(SYSTEM_USER_PASSWORD);
    verify(systemUserPasswordService).getOrCreatePassword(TENANT, USERNAME);
    verify(keycloakService).assignRole(KEYCLOAK_USER_ID, SYSTEM_ROLE);
  }

  @Test
  void create_positive_userExistsAndRoleIsFound() {
    when(folioExecutionContext.getTenantId()).thenReturn(TENANT);
    givenSystemUserPassword(USERNAME);
    when(userService.createUserSafe(userCaptor.capture(), passwordCaptor.capture(), eq(false))).then(firstArg());
    when(keycloakService.findUserByUsername(USERNAME)).thenReturn(Optional.of(keycloakUser()));
    when(keycloakService.hasRole(KEYCLOAK_USER_ID, SYSTEM_ROLE)).thenReturn(true);

    systemUserService.create();

    assertThat(passwordCaptor.getValue()).isEqualTo(SYSTEM_USER_PASSWORD);
    verify(systemUserPasswordService).getOrCreatePassword(TENANT, USERNAME);
  }

  @Test
  void create_negative_userByUsernameIsNotFound() {
    when(folioExecutionContext.getTenantId()).thenReturn(TENANT);
    givenSystemUserPassword(USERNAME);
    when(userService.createUserSafe(userCaptor.capture(), passwordCaptor.capture(), eq(false))).then(firstArg());
    when(keycloakService.findUserByUsername(USERNAME)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> systemUserService.create())
      .isInstanceOf(KeycloakException.class)
      .hasMessage("Failed to find a system user by username: " + USERNAME);

    assertThat(passwordCaptor.getValue()).isEqualTo(SYSTEM_USER_PASSWORD);
    verify(systemUserPasswordService).getOrCreatePassword(TENANT, USERNAME);
  }

  @Test
  void createOnEvent_positive_freshSetup() {
    when(folioExecutionContext.getTenantId()).thenReturn(TENANT);
    givenSystemUserPassword(MODULE_SYSTEM_USERNAME);
    when(userService.createUserSafe(userCaptor.capture(), passwordCaptor.capture(), eq(false))).then(firstArg());

    var event = SystemUserEvent.builder().type(CREATE).tenant(TENANT)
      .newValue(TestConstants.systemUser(Set.of(PERMISSION))).build();
    systemUserService.createOnEvent(event);

    assertThat(passwordCaptor.getValue()).isEqualTo(SYSTEM_USER_PASSWORD);
    assertThat(userCaptor.getValue()).usingRecursiveComparison().ignoringFields("id").isEqualTo(moduleUser());
    assertThat(userCaptor.getValue().getId()).isNotNull();

    verify(systemUserPasswordService).getOrCreatePassword(TENANT, MODULE_SYSTEM_USERNAME);
    verify(eventPublisher).publishSuccessFor(event, MODULE_ID);
  }

  @Test
  void createOnEvent_positive_usesPasswordServicePassword() {
    var storedPassword = "system-user-password";
    when(folioExecutionContext.getTenantId()).thenReturn(TENANT);
    when(systemUserPasswordService.getOrCreatePassword(TENANT, MODULE_SYSTEM_USERNAME)).thenReturn(storedPassword);
    when(userService.createUserSafe(userCaptor.capture(), passwordCaptor.capture(), eq(false))).then(firstArg());

    var event = SystemUserEvent.builder().type(CREATE).tenant(TENANT)
      .newValue(TestConstants.systemUser(Set.of(PERMISSION))).build();
    systemUserService.createOnEvent(event);

    assertThat(passwordCaptor.getValue()).isEqualTo(storedPassword);
    verify(systemUserPasswordService).getOrCreatePassword(TENANT, MODULE_SYSTEM_USERNAME);
    verify(eventPublisher).publishSuccessFor(event, MODULE_ID);
  }

  @Test
  void updateOnEvent_positive_userFound() {
    var user = systemUser().id(CAPABILITY_ID);
    when(folioExecutionContext.getTenantId()).thenReturn(TENANT);
    when(userService.findUsers("username==\"mod-foo\"", 1)).thenReturn(new Users().addUsersItem(user));

    var event = SystemUserEvent.builder().type(UPDATE).tenant(TENANT)
      .newValue(TestConstants.systemUser(Set.of(PERMISSION))).build();
    systemUserService.updateOnEvent(event);

    verify(systemUserPasswordService).migrateLegacyPasswordIfNeeded(TENANT, MODULE_SYSTEM_USERNAME);
    verify(eventPublisher).publishSuccessFor(event, MODULE_ID);
  }

  @Test
  void updateOnEvent_positive_userNotFoundThenCreate() {
    when(folioExecutionContext.getTenantId()).thenReturn(TENANT);
    givenSystemUserPassword(MODULE_SYSTEM_USERNAME);
    when(userService.findUsers("username==\"mod-foo\"", 1)).thenReturn(new Users());
    when(userService.createUserSafe(userCaptor.capture(), passwordCaptor.capture(), eq(false))).then(firstArg());

    var event = SystemUserEvent.builder().type(UPDATE).tenant(TENANT)
      .newValue(TestConstants.systemUser(Set.of(PERMISSION))).build();
    systemUserService.updateOnEvent(event);

    assertThat(userCaptor.getValue()).usingRecursiveComparison().ignoringFields("id").isEqualTo(moduleUser());
    assertThat(userCaptor.getValue().getId()).isNotNull();
    assertThat(passwordCaptor.getValue()).isEqualTo(SYSTEM_USER_PASSWORD);
    verify(systemUserPasswordService).getOrCreatePassword(TENANT, MODULE_SYSTEM_USERNAME);
    verify(eventPublisher).publishSuccessFor(event, MODULE_ID);
  }

  @Test
  void updateOnEvent_positive_emptyPermissions() {
    var event = SystemUserEvent.builder().type(UPDATE).tenant(TENANT)
      .newValue(TestConstants.systemUser(Set.of())).build();
    systemUserService.updateOnEvent(event);

    verify(folioExecutionContext, never()).getTenantId();
    verify(userService, never()).findUsers(any(), anyInt());
    verify(eventPublisher).publishSuccessFor(event, MODULE_ID);
  }

  @Test
  void deleteOnEvent_positive() {
    var userId = CAPABILITY_ID;
    var user = systemUser().id(userId);
    when(userService.findUsers("username==\"mod-foo\"", 1)).thenReturn(new Users().addUsersItem(user));

    var event = SystemUserEvent.builder().type(DELETE).tenant(TENANT)
      .oldValue(TestConstants.systemUser(Set.of())).build();
    systemUserService.deleteOnEvent(event);

    verify(userService).deleteUserById(userId);
    verify(eventPublisher).publishSuccessFor(event, MODULE_ID);
  }

  @Test
  void deleteOnEvent_positive_userNotFound() {
    when(userService.findUsers("username==\"mod-foo\"", 1)).thenReturn(new Users());

    var event = SystemUserEvent.builder().type(DELETE).tenant(TENANT)
      .oldValue(TestConstants.systemUser(Set.of())).build();
    systemUserService.deleteOnEvent(event);

    verify(userService, never()).deleteUserById(any());
    verify(eventPublisher).publishSuccessFor(event, MODULE_ID);
  }

  @Test
  void createOnEvent_negative_nullNewValue_throwsIllegalArgumentException() {
    var event = SystemUserEvent.builder().type(CREATE).tenant(TENANT).build();

    assertThatThrownBy(() -> systemUserService.createOnEvent(event))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("System user event does not contain new value");
  }

  @Test
  void updateOnEvent_negative_nullNewValue_throwsIllegalArgumentException() {
    var event = SystemUserEvent.builder().type(UPDATE).tenant(TENANT).build();

    assertThatThrownBy(() -> systemUserService.updateOnEvent(event))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("System user event does not contain new value");
  }

  @Test
  void deleteOnEvent_negative_nullOldValue_throwsIllegalArgumentException() {
    var event = SystemUserEvent.builder().type(DELETE).tenant(TENANT).build();

    assertThatThrownBy(() -> systemUserService.deleteOnEvent(event))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("System user event does not contain old value");
  }

  @Test
  void delete_positive() {
    var userId = CAPABILITY_ID;
    var user = systemUser().id(userId);

    when(folioExecutionContext.getTenantId()).thenReturn(TENANT);
    when(userService.findUsers("username==\"test-system-user\"", 1)).thenReturn(new Users().addUsersItem(user));

    systemUserService.delete();

    verify(userService).deleteUser(userId);
  }

  @Test
  void delete_positive_wildcard() {
    var userId = CAPABILITY_ID;
    var user = systemUser().id(userId);

    when(folioExecutionContext.getTenantId()).thenReturn("foo*");
    when(userService.findUsers("username==\"foo\\*-system-user\"", 1)).thenReturn(new Users().addUsersItem(user));

    systemUserService.delete();

    verify(userService).deleteUser(userId);
  }

  @Test
  void delete_positive_usersNotFoundByUsername() {
    when(folioExecutionContext.getTenantId()).thenReturn(TENANT);
    when(userService.findUsers("username==\"test-system-user\"", 1)).thenReturn(new Users());

    systemUserService.delete();

    verify(userService, never()).deleteUser(any());
  }

  @NotNull
  private static Answer<Object> firstArg() {
    return i -> i.getArgument(0);
  }

  private void givenSystemUserPassword(String username) {
    when(systemUserPasswordService.getOrCreatePassword(TENANT, username)).thenReturn(SYSTEM_USER_PASSWORD);
  }

  private static KeycloakUser keycloakUser() {
    var keycloakUser = new KeycloakUser();
    keycloakUser.setId(KEYCLOAK_USER_ID);
    keycloakUser.setUserName(USERNAME);
    keycloakUser.setFirstName("Global User");
    keycloakUser.setLastName(SYSTEM_ROLE);
    keycloakUser.setEmail("test-system-user@folio.org");
    keycloakUser.setEnabled(true);
    return keycloakUser;
  }

  private static User systemUser() {
    return new User()
      .username(USERNAME)
      .active(true)
      .type("system")
      .personal(new Personal()
        .firstName("System User")
        .lastName(SYSTEM_ROLE)
        .email("test-system-user@folio.org"));
  }

  private static User moduleUser() {
    return new User()
      .username(MODULE_SYSTEM_USERNAME)
      .active(true)
      .type("module")
      .personal(new Personal()
        .firstName("System user - mod-foo")
        .lastName(SYSTEM_ROLE));
  }
}
