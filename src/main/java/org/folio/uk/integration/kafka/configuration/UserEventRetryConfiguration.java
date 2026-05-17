package org.folio.uk.integration.kafka.configuration;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.Duration;
import lombok.Data;
import org.folio.uk.configuration.RetryProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

/**
 * Retry configuration for the user domain event Kafka listener, bound to
 * {@code application.retry.user-event.*} properties.
 *
 * <p>Controls the delay and maximum number of retry attempts when a transient error occurs
 * while processing a user event (e.g. tenant not yet initialised).
 */
@Data
@Component
@Validated
@ConfigurationProperties("application.retry.user-event")
public class UserEventRetryConfiguration implements RetryProperties {

  @NotNull
  private Duration retryDelay = Duration.ofMillis(250);

  @NotNull
  @Positive
  private long retryAttempts = Long.MAX_VALUE;
}
