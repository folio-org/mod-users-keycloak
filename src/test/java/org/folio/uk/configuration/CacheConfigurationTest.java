package org.folio.uk.configuration;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.Set;
import org.folio.test.types.UnitTest;
import org.junit.jupiter.api.Test;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.cache.CacheManager;

@UnitTest
class CacheConfigurationTest {

  @Test
  void cacheManager_positive_containsExpectedCaches() {
    var cacheManager = (SimpleCacheManager) new CacheConfiguration().cacheManager(cacheProperties());

    assertThat(cacheManager.getCacheNames())
      .containsAll(Set.of("keycloak-configuration", "keycloak-client-configuration", "token"));
  }

  @Test
  void cacheManager_positive_createsCachesWithConfiguredNames() {
    CacheManager cacheManager = new CacheConfiguration().cacheManager(cacheProperties());

    assertThat(cacheManager.getCache("keycloak-configuration")).isNotNull();
    assertThat(cacheManager.getCache("keycloak-client-configuration")).isNotNull();
    assertThat(cacheManager.getCache("token")).isNotNull();
  }

  private static CacheProperties cacheProperties() {
    return new CacheProperties(
      new CacheProperties.CacheSpec(Duration.ofHours(1), 500),
      new CacheProperties.CacheSpec(Duration.ofHours(1), 500),
      new CacheProperties.CacheSpec(Duration.ofSeconds(60), 10));
  }
}
