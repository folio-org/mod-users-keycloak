package org.folio.uk.it;

import static java.util.Optional.ofNullable;
import static org.assertj.core.api.Assertions.assertThat;
import static org.folio.uk.support.TestConstants.TENANT_NAME;

import org.folio.test.types.IntegrationTest;
import org.folio.uk.base.BaseIntegrationTest;
import org.folio.uk.integration.keycloak.TokenService;
import org.folio.uk.support.TestConstants;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;

@IntegrationTest
class TokenServiceIT extends BaseIntegrationTest {

  @Autowired private TokenService tokenService;

  @BeforeAll
  static void beforeAll() {
    enableTenant(TENANT_NAME);
  }

  @AfterAll
  static void afterAll() {
    removeTenant(TENANT_NAME);
  }

  @Test
  void issueToken_positive() {
    var issuedToken = tokenService.issueToken();

    var cachedToken = getCachedToken();
    assertThat(cachedToken)
      .isNotNull()
      .isEqualTo(issuedToken);
  }

  @Test
  void issueToken_positive_tokenAlreadyCached() {
    var existingToken = "existing";
    cacheManager.getCache(TestConstants.TOKEN_CACHE).put(TestConstants.TOKEN_CACHE_KEY, existingToken);

    var issuedToken = tokenService.issueToken();

    assertThat(issuedToken)
      .isNotNull()
      .isEqualTo(existingToken);
  }

  @Test
  void renewToken_positive() {
    var renewedToken = tokenService.renewToken();
    var cachedToken = getCachedToken();

    assertThat(cachedToken)
      .isNotNull()
      .isEqualTo(renewedToken);
  }

  @Test
  void renewToken_positive_tokenAlreadyCached() {
    var existingToken = "existing";
    cacheManager.getCache(TestConstants.TOKEN_CACHE).put(TestConstants.TOKEN_CACHE_KEY, existingToken);

    var renewedToken = tokenService.renewToken();
    var cachedToken = getCachedToken();

    assertThat(cachedToken)
      .isNotNull()
      .isEqualTo(renewedToken)
      .isNotEqualTo(existingToken);
  }

  private String getCachedToken() {
    return ofNullable(cacheManager.getCache(TestConstants.TOKEN_CACHE)).map(cache -> cache.get(
        TestConstants.TOKEN_CACHE_KEY))
      .map(Cache.ValueWrapper::get)
      .map(Object::toString)
      .orElse(null);
  }
}
