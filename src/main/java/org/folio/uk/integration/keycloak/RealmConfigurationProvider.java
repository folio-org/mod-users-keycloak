package org.folio.uk.integration.keycloak;

import lombok.RequiredArgsConstructor;
import org.folio.tools.store.SecureStore;
import org.folio.tools.store.exception.NotFoundException;
import org.folio.tools.store.properties.SecureStoreProperties;
import org.folio.uk.integration.keycloak.config.KeycloakProperties;
import org.folio.uk.integration.keycloak.model.KeycloakRealmConfiguration;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RealmConfigurationProvider {

  private static final String MASTER_REALM = "master";
  private final SecureStore secureStore;
  private final KeycloakProperties keycloakConfigurationProperties;
  private final SecureStoreProperties secureStoreProperties;

  /**
   * Provides realm configuration using {@link org.folio.spring.FolioExecutionContext} object.
   *
   * @return {@link KeycloakRealmConfiguration} object for user authentication
   */
  @Cacheable(cacheNames = "keycloak-configuration", key = "'keycloak-config'")
  public KeycloakRealmConfiguration getRealmConfiguration() {
    var clientId = keycloakConfigurationProperties.getClientId();
    return new KeycloakRealmConfiguration()
      .clientId(clientId)
      .clientSecret(retrieveKcClientSecret(MASTER_REALM, clientId));
  }

  /**
   * Provides configuration for a client.
   *
   * @return {@link KeycloakRealmConfiguration} object
   */
  @Cacheable(cacheNames = "keycloak-client-configuration", key = "{#realm, #clientId}")
  public KeycloakRealmConfiguration getClientConfiguration(String realm, String clientId) {
    return new KeycloakRealmConfiguration()
      .clientId(clientId)
      .clientSecret(retrieveKcClientSecret(realm, clientId));
  }

  @CacheEvict(cacheNames = "keycloak-client-configuration", allEntries = true)
  public void evictAllClientConfigurations() {
  }

  private String retrieveKcClientSecret(String realm, String clientId) {
    try {
      return secureStore.get(buildKey(secureStoreProperties.getEnvironment(), realm, clientId));
    } catch (NotFoundException e) {
      throw new IllegalStateException(String.format(
        "Failed to get value from secure store [clientId: %s]", clientId), e);
    }
  }

  private String buildKey(String env, String realm, String clientId) {
    return String.format("%s_%s_%s", env, realm, clientId);
  }
}
