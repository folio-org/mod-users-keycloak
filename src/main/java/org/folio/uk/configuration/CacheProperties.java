package org.folio.uk.configuration;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "application.cache")
public record CacheProperties(
  CacheSpec keycloakConfiguration,
  CacheSpec keycloakClientConfiguration,
  CacheSpec token
) {

  public record CacheSpec(Duration ttl, long maxSize) {}
}
