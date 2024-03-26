package org.folio.uk.integration.keycloak.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "application.keycloak.tls")
public class KeycloakTlsProperties {

  private boolean enabled;
  private String trustStorePath;
  private String trustStorePassword;
}
