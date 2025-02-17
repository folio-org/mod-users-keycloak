package org.folio.uk.integration.keycloak.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Data
@Component
@Validated
@ConfigurationProperties(prefix = "application.federated-auth")
public class KeycloakFederatedAuthProperties {
  private boolean isEnabled;
}
