package org.folio.uk.configuration;

import java.time.Duration;

/**
 * Common retry configuration contract shared by Kafka listener retry configurations.
 */
public interface RetryProperties {

  /**
   * Returns the delay between retry attempts.
   *
   * @return retry delay duration
   */
  Duration getRetryDelay();

  /**
   * Returns the maximum number of retry attempts.
   *
   * @return maximum retry attempts; {@link Long#MAX_VALUE} for effectively infinite retries
   */
  long getRetryAttempts();
}
