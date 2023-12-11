package org.folio.uk.integration.configuration;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Data
@Component
@Validated
@ConfigurationProperties(prefix = "okapi")
public class OkapiConfigurationProperties {

  /**
   * Okapi URL.
   */
  @NotNull
  private String url;
}
