package org.folio.uk.integration.keycloak.config;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Data
@Component
@Validated
@ConfigurationProperties(prefix = "application.keycloak.login")
public class KeycloakLoginClientProperties {
  @NotNull
  private String clientNameSuffix;
}
