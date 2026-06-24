package org.folio.uk.integration.keycloak;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.folio.test.types.UnitTest;
import org.folio.tools.store.SecureStore;
import org.folio.tools.store.properties.SecureStoreProperties;
import org.folio.uk.configuration.SystemUserConfigurationProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@UnitTest
@ExtendWith(MockitoExtension.class)
class SystemUserPasswordServiceTest {

  private static final String TENANT = "test";
  private static final String USERNAME = "mod-foo";
  private static final String SECURE_STORE_ENV = "secure-test";
  private static final String SYSTEM_USER_STORE_KEY = "secure-test_test_mod-foo";
  private static final String LEGACY_SYSTEM_USER_STORE_KEY = "folio_test_mod-foo";

  @InjectMocks private SystemUserPasswordService systemUserPasswordService;

  @Mock private SecureStore secureStore;
  @Mock private SecureStoreProperties secureStoreProperties;

  @Spy private final SystemUserConfigurationProperties userConfiguration = new SystemUserConfigurationProperties();
  @Captor private ArgumentCaptor<String> passwordCaptor;

  @Test
  void getOrCreatePassword_positive_existingSecureStorePassword_passwordIsReused() {
    var storedPassword = "system-user-password";
    when(secureStoreProperties.getEnvironment()).thenReturn(SECURE_STORE_ENV);
    when(secureStore.lookup(SYSTEM_USER_STORE_KEY)).thenReturn(Optional.of(storedPassword));

    var result = systemUserPasswordService.getOrCreatePassword(TENANT, USERNAME);

    assertThat(result).isEqualTo(storedPassword);
    verify(secureStore, never()).set(any(), any());
  }

  @Test
  void getOrCreatePassword_positive_existingLegacyPassword_passwordIsMigrated() {
    var storedPassword = "legacy-system-user-password";
    when(secureStoreProperties.getEnvironment()).thenReturn(SECURE_STORE_ENV);
    when(secureStore.lookup(SYSTEM_USER_STORE_KEY)).thenReturn(Optional.empty());
    when(secureStore.lookup(LEGACY_SYSTEM_USER_STORE_KEY)).thenReturn(Optional.of(storedPassword));

    var result = systemUserPasswordService.getOrCreatePassword(TENANT, USERNAME);

    assertThat(result).isEqualTo(storedPassword);
    verify(secureStore).set(SYSTEM_USER_STORE_KEY, storedPassword);
  }

  @Test
  void getOrCreatePassword_positive_passwordIsMissing_passwordIsGenerated() {
    when(secureStoreProperties.getEnvironment()).thenReturn(SECURE_STORE_ENV);
    when(secureStore.lookup(SYSTEM_USER_STORE_KEY)).thenReturn(Optional.empty());
    when(secureStore.lookup(LEGACY_SYSTEM_USER_STORE_KEY)).thenReturn(Optional.empty());

    var result = systemUserPasswordService.getOrCreatePassword(TENANT, USERNAME);

    assertThat(result).hasSize(userConfiguration.getPasswordLength());
    verify(secureStore).set(eq(SYSTEM_USER_STORE_KEY), passwordCaptor.capture());
    assertThat(passwordCaptor.getValue()).isEqualTo(result);
  }

  @Test
  void migrateLegacyPasswordIfNeeded_positive_secureStorePasswordExists_passwordIsNotMigrated() {
    when(secureStoreProperties.getEnvironment()).thenReturn(SECURE_STORE_ENV);
    when(secureStore.lookup(SYSTEM_USER_STORE_KEY)).thenReturn(Optional.of("system-user-password"));

    systemUserPasswordService.migrateLegacyPasswordIfNeeded(TENANT, USERNAME);

    verify(secureStore, never()).lookup(LEGACY_SYSTEM_USER_STORE_KEY);
    verify(secureStore, never()).set(any(), any());
  }

  @Test
  void migrateLegacyPasswordIfNeeded_positive_legacyPasswordExists_passwordIsMigrated() {
    var storedPassword = "legacy-system-user-password";
    when(secureStoreProperties.getEnvironment()).thenReturn(SECURE_STORE_ENV);
    when(secureStore.lookup(SYSTEM_USER_STORE_KEY)).thenReturn(Optional.empty());
    when(secureStore.lookup(LEGACY_SYSTEM_USER_STORE_KEY)).thenReturn(Optional.of(storedPassword));

    systemUserPasswordService.migrateLegacyPasswordIfNeeded(TENANT, USERNAME);

    verify(secureStore).set(SYSTEM_USER_STORE_KEY, storedPassword);
  }

  @Test
  void migrateLegacyPasswordIfNeeded_positive_passwordIsMissing_passwordIsNotGenerated() {
    when(secureStoreProperties.getEnvironment()).thenReturn(SECURE_STORE_ENV);
    when(secureStore.lookup(SYSTEM_USER_STORE_KEY)).thenReturn(Optional.empty());
    when(secureStore.lookup(LEGACY_SYSTEM_USER_STORE_KEY)).thenReturn(Optional.empty());

    systemUserPasswordService.migrateLegacyPasswordIfNeeded(TENANT, USERNAME);

    verify(userConfiguration, never()).getPasswordLength();
    verify(secureStore, never()).set(any(), any());
  }

  @Test
  void migrateLegacyPasswordIfNeeded_positive_legacyAndSecureStoreKeysMatch_passwordIsNotGenerated() {
    when(secureStoreProperties.getEnvironment()).thenReturn("folio");
    when(secureStore.lookup(LEGACY_SYSTEM_USER_STORE_KEY)).thenReturn(Optional.empty());

    systemUserPasswordService.migrateLegacyPasswordIfNeeded(TENANT, USERNAME);

    verify(userConfiguration, never()).getPasswordLength();
    verify(secureStore, never()).set(any(), any());
  }
}
