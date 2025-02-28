package org.folio.uk.migration.properties;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Data
@Component
@Validated
@ConfigurationProperties(prefix = "application.migration")
public class IdpMigrationProperties {

  @NotNull
  @Max(value = 50)
  private Integer batchSize;
}
