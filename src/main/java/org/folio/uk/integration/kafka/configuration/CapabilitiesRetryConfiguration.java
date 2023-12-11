package org.folio.uk.integration.kafka.configuration;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.Duration;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties("application.retry.system-user-capabilities")
public class CapabilitiesRetryConfiguration {

  /**
   * Retry delay for module user capabilities assignment.
   */
  @NotNull
  private Duration retryDelay = Duration.ofSeconds(5);

  /**
   * A number for Retry attempts for module user capabilities assignment.
   */
  @NotNull
  @Positive
  private int retryAttempts = 60;
}
