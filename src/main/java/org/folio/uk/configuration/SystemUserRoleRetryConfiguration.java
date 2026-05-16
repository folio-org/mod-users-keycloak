package org.folio.uk.configuration;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.Duration;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties("application.retry.system-user-role")
public class SystemUserRoleRetryConfiguration implements RetryProperties {

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
  @Max(Integer.MAX_VALUE)
  private long retryAttempts = 60;
}
