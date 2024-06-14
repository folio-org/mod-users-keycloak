package org.folio.uk.integration.keycloak.config;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.folio.common.configuration.properties.TlsProperties;
import org.hibernate.validator.constraints.URL;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Data
@Component
@Validated
@ConfigurationProperties(prefix = "application.keycloak")
public class KeycloakProperties {

  @NotNull
  private String grantType;
  @NotNull
  private String clientId;
  @URL
  private String url;
  @NotNull
  private TlsProperties tls;
}
