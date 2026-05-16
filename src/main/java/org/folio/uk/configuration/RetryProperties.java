package org.folio.uk.configuration;

import java.time.Duration;

public interface RetryProperties {

  Duration getRetryDelay();

  long getRetryAttempts();
}
