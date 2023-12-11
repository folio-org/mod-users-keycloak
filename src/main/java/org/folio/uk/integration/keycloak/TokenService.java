package org.folio.uk.integration.keycloak;

import java.util.HashMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.uk.integration.keycloak.config.KeycloakProperties;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

@Log4j2
@Component
@RequiredArgsConstructor
public class TokenService {

  private static final String CACHE_NAME = "token";
  private static final String CACHE_KEY = "'admin-cli-token'";
  private final KeycloakClient keycloakClient;
  private final KeycloakProperties keycloakProperties;
  private final RealmConfigurationProvider realmConfigurationProvider;

  @Cacheable(cacheNames = CACHE_NAME, key = CACHE_KEY)
  public String issueToken() {
    return requestToken();
  }

  @CachePut(cacheNames = CACHE_NAME, key = CACHE_KEY)
  public String renewToken() {
    return requestToken();
  }

  private String requestToken() {
    var realmConfigProvider = realmConfigurationProvider.getRealmConfiguration();
    var loginRequest = new HashMap<String, String>();
    var clientId = keycloakProperties.getClientId();
    loginRequest.put("client_id", clientId);
    loginRequest.put("client_secret", realmConfigProvider.getClientSecret());
    loginRequest.put("grant_type", keycloakProperties.getGrantType());

    log.info("Issuing access token for Keycloak communication [clientId: {}]", clientId);
    var token = keycloakClient.login(loginRequest);
    return token.getTokenType() + " " + token.getAccessToken();
  }
}
