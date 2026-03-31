package org.folio.uk.integration.keycloak.config;

import org.folio.common.utils.tls.HttpClientTlsUtils;
import org.folio.uk.integration.keycloak.KeycloakClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class KeycloakClientConfig {

  @Bean
  public KeycloakClient keycloakClient(KeycloakProperties properties) {
    return HttpClientTlsUtils.buildHttpServiceClient(
      RestClient.builder(), properties.getTls(), properties.getUrl(), KeycloakClient.class);
  }
}
