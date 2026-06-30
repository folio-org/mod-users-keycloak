package org.folio.uk.integration.keycloak;

import static org.folio.common.configuration.properties.FolioEnvironment.getFolioEnvName;
import static org.folio.tools.store.utils.SecretGenerator.generateSecret;

import java.util.Objects;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.tools.store.SecureStore;
import org.folio.tools.store.properties.SecureStoreProperties;
import org.folio.uk.configuration.SystemUserConfigurationProperties;
import org.springframework.stereotype.Service;

@Log4j2
@Service
@RequiredArgsConstructor
public class SystemUserPasswordService {

  private final SecureStore secureStore;
  private final SecureStoreProperties secureStoreProperties;
  private final SystemUserConfigurationProperties systemUserConfiguration;

  public String getOrCreatePassword(String tenant, String username) {
    var systemUserKey = getSystemUserStoreKey(secureStoreProperties.getEnvironment(), tenant, username);
    return secureStore.lookup(systemUserKey)
      .or(() -> migrateLegacyPassword(systemUserKey, tenant, username))
      .orElseGet(() -> generateAndSavePassword(systemUserKey));
  }

  public void migrateLegacyPasswordIfNeeded(String tenant, String username) {
    // A legacy password is an existing system-user password stored under the old ENV-derived key.
    // Copy it to the SECURE_STORE_ENV-derived key so the sidecar can read the same credential.

    var systemUserKey = getSystemUserStoreKey(secureStoreProperties.getEnvironment(), tenant, username);
    if (secureStore.lookup(systemUserKey).isPresent()) {
      return;
    }

    migrateLegacyPassword(systemUserKey, tenant, username);
  }

  private Optional<String> migrateLegacyPassword(String systemUserKey, String tenant, String username) {
    var legacyKey = getLegacySystemUserStoreKey(tenant, username);
    if (Objects.equals(systemUserKey, legacyKey)) {
      return Optional.empty();
    }

    return secureStore.lookup(legacyKey)
      .map(password -> saveLegacyPassword(systemUserKey, legacyKey, password));
  }

  private static String getSystemUserStoreKey(String env, String tenant, String username) {
    return String.format("%s_%s_%s", env, tenant, username);
  }

  private static String getLegacySystemUserStoreKey(String tenant, String username) {
    return getSystemUserStoreKey(getFolioEnvName(), tenant, username);
  }

  private String saveLegacyPassword(String systemUserKey, String legacyKey, String password) {
    log.info("Found legacy system user password, copying to new key [legacyKey: {}, key: {}]",
      legacyKey, systemUserKey);
    return savePassword(systemUserKey, password);
  }

  private String generateAndSavePassword(String key) {
    log.info("Generating system user password [key: {}]", key);
    var secret = generateSecret(systemUserConfiguration.getPasswordLength());
    return savePassword(key, secret);
  }

  private String savePassword(String key, String secret) {
    secureStore.set(key, secret);
    return secret;
  }
}
