package org.folio.uk.integration.kafka.configuration;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.Duration;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

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
