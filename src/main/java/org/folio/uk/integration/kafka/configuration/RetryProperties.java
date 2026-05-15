package org.folio.uk.integration.kafka.configuration;

import java.time.Duration;

public interface RetryProperties {

  Duration getRetryDelay();

  long getRetryAttempts();
}
