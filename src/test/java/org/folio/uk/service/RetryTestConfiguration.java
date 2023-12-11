package org.folio.uk.service;

import static java.util.Collections.singletonList;
import static org.folio.spring.integration.XOkapiHeaders.TENANT;
import static org.folio.test.TestConstants.TENANT_ID;

import java.time.Duration;
import java.util.Map;
import org.folio.spring.DefaultFolioExecutionContext;
import org.folio.spring.FolioExecutionContext;
import org.folio.uk.configuration.RetryConfiguration;
import org.folio.uk.configuration.SystemUserConfigurationProperties;
import org.folio.uk.integration.kafka.configuration.CapabilitiesRetryConfiguration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.retry.annotation.EnableRetry;

@EnableRetry
@TestConfiguration
@Import(RetryConfiguration.class)
public class RetryTestConfiguration {

  @Bean
  public FolioExecutionContext folioExecutionContext() {
    return new DefaultFolioExecutionContext(null, Map.of(TENANT, singletonList(TENANT_ID)));
  }

  @Bean
  public SystemUserConfigurationProperties systemUserConfigurationProperties() {
    var configuration = new SystemUserConfigurationProperties();
    configuration.setRetryDelay(1);
    return configuration;
  }

  @Bean
  public CapabilitiesRetryConfiguration capabilitiesRetryConfiguration() {
    var config = new CapabilitiesRetryConfiguration();
    config.setRetryDelay(Duration.ofMillis(50));
    config.setRetryAttempts(2);
    return config;
  }
}
