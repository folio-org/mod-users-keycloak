package org.folio.uk.integration.keycloak;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.folio.uk.support.TestConstants.systemUserEvent;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.folio.spring.FolioExecutionContext;
import org.folio.test.types.UnitTest;
import org.folio.tools.store.SecureStore;
import org.folio.uk.configuration.SystemUserConfigurationProperties;
import org.folio.uk.domain.dto.Personal;
import org.folio.uk.domain.dto.User;
import org.folio.uk.domain.dto.Users;
import org.folio.uk.exception.UnresolvedPermissionsException;
import org.folio.uk.integration.keycloak.model.KeycloakUser;
import org.folio.uk.service.CapabilitiesService;
import org.folio.uk.service.UserService;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;

@UnitTest
@ExtendWith(MockitoExtension.class)
class SystemUserServiceTest {

  private static final String TENANT = "test";
  private static final String USERNAME = "test-system-user";
  private static final String SYSTEM_USER_STORE_KEY = "folio_test_test-system-user";
  private static final String MODULE_SYSTEM_USERNAME = "mod-foo";
  private static final String MODULE_SYSTEM_USER_STORE_KEY = "folio_test_mod-foo";
  private static final String KEYCLOAK_USER_ID = UUID.randomUUID().toString();
  private static final String SYSTEM_ROLE = "System";
  private static final UUID CAPABILITY_ID = UUID.randomUUID();
  private static final String PERMISSION = "foo.bar";

  @InjectMocks private SystemUserService systemUserService;

  @Mock private SecureStore secureStore;
  @Mock private UserService userService;
  @Mock private CapabilitiesService capabilitiesService;
  @Mock private KeycloakService keycloakService;
  @Mock private FolioExecutionContext folioExecutionContext;

  @Spy private final SystemUserConfigurationProperties userConfiguration = new SystemUserConfigurationProperties();
  @Captor private ArgumentCaptor<User> userCaptor;
  @Captor private ArgumentCaptor<String> passwordCaptor;

  @AfterEach
  void tearDown() {
    verifyNoMoreInteractions(secureStore, userService, keycloakService);
  }

  @Test
  void create_positive_freshSetup() {
    when(folioExecutionContext.getTenantId()).thenReturn(TENANT);
    when(secureStore.lookup(SYSTEM_USER_STORE_KEY)).thenReturn(Optional.empty());
    when(userService.createUserSafe(userCaptor.capture(), passwordCaptor.capture(), eq(false))).then(firstArg());
    when(keycloakService.findUserByUsername(USERNAME)).thenReturn(Optional.of(keycloakUser()));
    when(keycloakService.hasRole(KEYCLOAK_USER_ID, SYSTEM_ROLE)).thenReturn(false);

    systemUserService.create();

    verify(userConfiguration).getSystemUserRole();
    verify(userConfiguration).getEmailTemplate();
    verify(userConfiguration).getUsernameTemplate();
    verify(userConfiguration).getPasswordLength();

    var capturedPassword = passwordCaptor.getValue();
    assertThat(passwordCaptor.getValue()).hasSize(userConfiguration.getPasswordLength());
    assertThat(userCaptor.getValue()).usingRecursiveComparison().ignoringFields("id").isEqualTo(systemUser());
    assertThat(userCaptor.getValue().getId()).isNotNull();

    verify(secureStore).set(SYSTEM_USER_STORE_KEY, capturedPassword);
    verify(keycloakService).assignRole(KEYCLOAK_USER_ID, SYSTEM_ROLE);
  }

  @Test
  void create_positive_userExistsAndRoleNotFound() {
    when(folioExecutionContext.getTenantId()).thenReturn(TENANT);
    when(secureStore.lookup(SYSTEM_USER_STORE_KEY)).thenReturn(Optional.empty());
    when(userService.createUserSafe(userCaptor.capture(), passwordCaptor.capture(), eq(false))).then(firstArg());
    when(keycloakService.findUserByUsername(USERNAME)).thenReturn(Optional.of(keycloakUser()));
    when(keycloakService.hasRole(KEYCLOAK_USER_ID, SYSTEM_ROLE)).thenReturn(false);

    systemUserService.create();

    var capturedPassword = passwordCaptor.getValue();
    assertThat(new HashSet<>(passwordCaptor.getAllValues())).hasSize(1);
    assertThat(capturedPassword).hasSize(userConfiguration.getPasswordLength());
    verify(secureStore).set(SYSTEM_USER_STORE_KEY, capturedPassword);
    verify(keycloakService).assignRole(KEYCLOAK_USER_ID, SYSTEM_ROLE);
  }

  @Test
  void create_positive_userExistsAndRoleIsFound() {
    when(folioExecutionContext.getTenantId()).thenReturn(TENANT);
    when(secureStore.lookup(SYSTEM_USER_STORE_KEY)).thenReturn(Optional.empty());
    when(userService.createUserSafe(userCaptor.capture(), passwordCaptor.capture(), eq(false))).then(firstArg());
    when(keycloakService.findUserByUsername(USERNAME)).thenReturn(Optional.of(keycloakUser()));
    when(keycloakService.hasRole(KEYCLOAK_USER_ID, SYSTEM_ROLE)).thenReturn(true);

    systemUserService.create();

    var capturedPassword = passwordCaptor.getValue();
    assertThat(passwordCaptor.getValue()).hasSize(userConfiguration.getPasswordLength());
    verify(secureStore).set(SYSTEM_USER_STORE_KEY, capturedPassword);
  }

  @Test
  void create_negative_userByUsernameIsNotFound() {
    when(folioExecutionContext.getTenantId()).thenReturn(TENANT);
    when(secureStore.lookup(SYSTEM_USER_STORE_KEY)).thenReturn(Optional.empty());
    when(userService.createUserSafe(userCaptor.capture(), passwordCaptor.capture(), eq(false))).then(firstArg());
    when(keycloakService.findUserByUsername(USERNAME)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> systemUserService.create())
      .isInstanceOf(KeycloakException.class)
      .hasMessage("Failed to find a system user by username: " + USERNAME);

    var capturedPassword = passwordCaptor.getValue();
    assertThat(passwordCaptor.getValue()).hasSize(userConfiguration.getPasswordLength());
    verify(secureStore).set(SYSTEM_USER_STORE_KEY, capturedPassword);
  }

  @Test
  void createOnEvent_positive_freshSetup() {
    when(folioExecutionContext.getTenantId()).thenReturn(TENANT);
    when(secureStore.lookup(MODULE_SYSTEM_USER_STORE_KEY)).thenReturn(Optional.empty());
    when(userService.createUserSafe(userCaptor.capture(), passwordCaptor.capture(), eq(false))).then(firstArg());

    systemUserService.createOnEvent(systemUserEvent(Set.of(PERMISSION)));

    verify(userConfiguration).getPasswordLength();

    var capturedPassword = passwordCaptor.getValue();
    assertThat(passwordCaptor.getValue()).hasSize(userConfiguration.getPasswordLength());
    assertThat(userCaptor.getValue()).usingRecursiveComparison().ignoringFields("id").isEqualTo(moduleUser());
    assertThat(userCaptor.getValue().getId()).isNotNull();

    verify(capabilitiesService).assignCapabilitiesByPermissions(any(User.class), eq(Set.of(PERMISSION)));
    verify(secureStore).set(MODULE_SYSTEM_USER_STORE_KEY, capturedPassword);
  }

  @Test
  void createOnEvent_negative_capabilityNotFound() {
    Mockito.doThrow(new UnresolvedPermissionsException(null, List.of(PERMISSION))).when(capabilitiesService)
      .assignCapabilitiesByPermissions(any(), any());
    when(folioExecutionContext.getTenantId()).thenReturn(TENANT);
    when(secureStore.lookup(MODULE_SYSTEM_USER_STORE_KEY)).thenReturn(Optional.empty());
    when(userService.createUserSafe(userCaptor.capture(), passwordCaptor.capture(), eq(false))).then(firstArg());

    assertThatThrownBy(() -> systemUserService.createOnEvent(systemUserEvent(Set.of(PERMISSION))))
      .isInstanceOf(UnresolvedPermissionsException.class);

    verify(userConfiguration).getPasswordLength();

    var capturedPassword = passwordCaptor.getValue();
    assertThat(passwordCaptor.getValue()).hasSize(userConfiguration.getPasswordLength());
    assertThat(userCaptor.getValue()).usingRecursiveComparison().ignoringFields("id").isEqualTo(moduleUser());
    assertThat(userCaptor.getValue().getId()).isNotNull();

    verify(secureStore).set(MODULE_SYSTEM_USER_STORE_KEY, capturedPassword);
  }

  @Test
  void updateOnEvent_positive_userFound() {
    var user = systemUser().id(CAPABILITY_ID);
    when(userService.findUsers("username==mod-foo", 1)).thenReturn(new Users().addUsersItem(user));

    systemUserService.updateOnEvent(systemUserEvent(Set.of(PERMISSION)));

    verify(capabilitiesService).assignCapabilitiesByPermissions(user, Set.of(PERMISSION));
    verify(folioExecutionContext, never()).getTenantId();
  }

  @Test
  void updateOnEvent_positive_userNotFoundThenCreate() {
    when(folioExecutionContext.getTenantId()).thenReturn(TENANT);
    when(secureStore.lookup(MODULE_SYSTEM_USER_STORE_KEY)).thenReturn(Optional.empty());
    when(userService.findUsers("username==mod-foo", 1)).thenReturn(new Users());
    when(userService.createUserSafe(userCaptor.capture(), any(), eq(false))).then(firstArg());
    doNothing().when(secureStore).set(any(), any());

    systemUserService.updateOnEvent(systemUserEvent(Set.of(PERMISSION)));

    assertThat(userCaptor.getValue()).usingRecursiveComparison().ignoringFields("id").isEqualTo(moduleUser());
    assertThat(userCaptor.getValue().getId()).isNotNull();
    verify(capabilitiesService).assignCapabilitiesByPermissions(any(User.class), eq(Set.of(PERMISSION)));
  }

  @Test
  void updateOnEvent_positive_emptyPermissions() {
    systemUserService.updateOnEvent(systemUserEvent(Set.of()));

    verify(folioExecutionContext, never()).getTenantId();
    verify(userService, never()).findUsers(any(), anyInt());
    verify(capabilitiesService, never()).assignCapabilitiesByPermissions(any(), any());
  }

  @Test
  void deleteOnEvent_positive() {
    var userId = CAPABILITY_ID;
    var user = systemUser().id(userId);
    when(userService.findUsers("username==mod-foo", 1)).thenReturn(new Users().addUsersItem(user));

    systemUserService.deleteOnEvent(systemUserEvent(Set.of()));

    verify(userService).deleteUserById(userId);
  }

  @Test
  void deleteOnEvent_positive_userNotFound() {
    when(userService.findUsers("username==mod-foo", 1)).thenReturn(new Users());

    systemUserService.deleteOnEvent(systemUserEvent(Set.of()));

    verify(userService, never()).deleteUserById(any());
  }

  @Test
  void delete_positive() {
    var userId = CAPABILITY_ID;
    var user = systemUser().id(userId);

    when(folioExecutionContext.getTenantId()).thenReturn(TENANT);
    when(userService.findUsers("username==test-system-user", 1)).thenReturn(new Users().addUsersItem(user));

    systemUserService.delete();

    verify(userService).deleteUser(userId);
  }

  @Test
  void delete_positive_usersNotFoundByUsername() {
    when(folioExecutionContext.getTenantId()).thenReturn(TENANT);
    when(userService.findUsers("username==test-system-user", 1)).thenReturn(new Users());

    systemUserService.delete();

    verify(userService, never()).deleteUser(any());
  }

  @NotNull
  private static Answer<Object> firstArg() {
    return i -> i.getArgument(0);
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
