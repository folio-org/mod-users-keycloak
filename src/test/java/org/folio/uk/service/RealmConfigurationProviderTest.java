package org.folio.uk.service;

import static java.util.Collections.singletonList;
import static java.util.Objects.requireNonNull;
import static java.util.Optional.ofNullable;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.folio.spring.integration.XOkapiHeaders.TENANT;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.folio.common.configuration.properties.FolioEnvironment;
import org.folio.spring.DefaultFolioExecutionContext;
import org.folio.spring.FolioExecutionContext;
import org.folio.test.types.UnitTest;
import org.folio.tools.store.SecureStore;
import org.folio.tools.store.exception.NotFoundException;
import org.folio.uk.integration.keycloak.RealmConfigurationProvider;
import org.folio.uk.integration.keycloak.config.KeycloakProperties;
import org.folio.uk.integration.keycloak.model.KeycloakRealmConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cache.Cache.ValueWrapper;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;

@UnitTest
@SpringBootTest(classes = {RealmConfigurationProvider.class,
  RealmConfigurationProviderTest.TestContextConfiguration.class},
  webEnvironment = WebEnvironment.NONE)
class RealmConfigurationProviderTest {

  private static final String CLIENT_ID = "folio-backend-admin";
  private static final String TENANT_ID = "master";
  private static final String CACHE_NAME = "keycloak-configuration";
  private static final String CLIENT_CACHE_NAME = "keycloak-client-configuration";
  private static final String SECRET = "kc-client-secret";
  private static final String KEY = String.format("test_%s_%s", TENANT_ID, CLIENT_ID);
  private static final String PASSWORD_RESET_ID = "password-reset-client";
  private static final String PASSWORD_RESET_KEY = String.format("test_%s_%s", TENANT_ID, PASSWORD_RESET_ID);

  @Autowired private CacheManager cacheManager;
  @Autowired private RealmConfigurationProvider realmConfigurationProvider;
  @MockBean private SecureStore secureStore;
  @MockBean private FolioEnvironment folioEnvironment;
  @MockBean private KeycloakProperties keycloakConfigurationProperties;

  @AfterEach
  void tearDown() {
    cacheManager.getCacheNames().forEach(cacheName -> requireNonNull(cacheManager.getCache(cacheName)).clear());
  }

  @Test
  void getRealmConfiguration_positive() {
    when(keycloakConfigurationProperties.getClientId()).thenReturn(CLIENT_ID);
    when(folioEnvironment.getEnvironment()).thenReturn("test");
    when(secureStore.get(KEY)).thenReturn(SECRET);

    var actual = realmConfigurationProvider.getRealmConfiguration();

    var expectedValue = new KeycloakRealmConfiguration()
      .clientId(CLIENT_ID)
      .clientSecret(SECRET);

    assertThat(actual).isEqualTo(expectedValue);
    assertThat(getCachedValue()).isPresent().get().isEqualTo(expectedValue);
  }

  @Test
  void getRealmConfiguration_clientSecretNotFound() {
    when(keycloakConfigurationProperties.getClientId()).thenReturn(CLIENT_ID);
    when(folioEnvironment.getEnvironment()).thenReturn("test");
    when(secureStore.get(KEY)).thenThrow(new NotFoundException("not found"));

    assertThatThrownBy(() -> realmConfigurationProvider.getRealmConfiguration())
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("Failed to get value from secure store [clientId: " + CLIENT_ID + "]");

    assertThat(getCachedValue()).isEmpty();
  }

  @Test
  void getClientConfiguration_positive() {
    when(folioEnvironment.getEnvironment()).thenReturn("test");
    when(secureStore.get(PASSWORD_RESET_KEY)).thenReturn(SECRET);

    var actual = realmConfigurationProvider.getClientConfiguration(TENANT_ID, PASSWORD_RESET_ID);

    var expectedValue = new KeycloakRealmConfiguration()
      .clientId(PASSWORD_RESET_ID)
      .clientSecret(SECRET);

    assertThat(actual).isEqualTo(expectedValue);
    assertThat(getCachedClientValue(TENANT_ID, PASSWORD_RESET_ID)).isPresent().get().isEqualTo(expectedValue);
  }

  @Test
  void getClientConfiguration_clientSecretNotFound() {
    when(folioEnvironment.getEnvironment()).thenReturn("test");
    when(secureStore.get(PASSWORD_RESET_KEY)).thenThrow(new NotFoundException("not found"));

    assertThatThrownBy(() -> realmConfigurationProvider.getClientConfiguration(TENANT_ID, PASSWORD_RESET_ID))
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("Failed to get value from secure store [clientId: " + PASSWORD_RESET_ID + "]");

    assertThat(getCachedClientValue(TENANT_ID, PASSWORD_RESET_ID)).isEmpty();
  }

  @Test
  void evictAllClientConfigurations_positive() {
    when(folioEnvironment.getEnvironment()).thenReturn("test");
    when(secureStore.get(PASSWORD_RESET_KEY)).thenReturn(SECRET);

    realmConfigurationProvider.getClientConfiguration(TENANT_ID, PASSWORD_RESET_ID);
    assertThat(getCachedClientValue(TENANT_ID, PASSWORD_RESET_ID)).isPresent();

    realmConfigurationProvider.evictAllClientConfigurations();
    assertThat(getCachedClientValue(TENANT_ID, PASSWORD_RESET_ID)).isEmpty();
  }

  private Optional<Object> getCachedValue() {
    return ofNullable(cacheManager.getCache(CACHE_NAME))
      .map(cache -> cache.get("keycloak-config"))
      .map(ValueWrapper::get);
  }

  private Optional<Object> getCachedClientValue(String realm, String clientId) {
    return ofNullable(cacheManager.getCache(CLIENT_CACHE_NAME))
      .map(cache -> cache.get(List.of(realm, clientId)))
      .map(ValueWrapper::get);
  }

  @EnableCaching
  @TestConfiguration
  static class TestContextConfiguration {

    @Bean
    CacheManager cacheManager() {
      return new ConcurrentMapCacheManager(CACHE_NAME, CLIENT_CACHE_NAME);
    }

    @Bean
    FolioExecutionContext folioExecutionContext() {
      return new DefaultFolioExecutionContext(null, Map.of(TENANT, singletonList(TENANT_ID)));
    }
  }
}
