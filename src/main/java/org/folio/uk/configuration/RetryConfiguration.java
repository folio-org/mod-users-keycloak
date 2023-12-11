package org.folio.uk.configuration;

import feign.FeignException;
import java.util.List;
import lombok.extern.log4j.Log4j2;
import org.folio.uk.exception.UnresolvedPermissionsException;
import org.folio.uk.integration.kafka.configuration.CapabilitiesRetryConfiguration;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.RetryContext;
import org.springframework.retry.RetryListener;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.retry.listener.RetryListenerSupport;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.retry.support.RetryTemplateBuilder;

@Log4j2
@EnableRetry
@Configuration
public class RetryConfiguration {

  @Bean(name = "methodLoggingRetryListener")
  public RetryListener methodLoggingRetryListener() {
    return new RetryListenerSupport() {

      @Override
      public <T, E extends Throwable> void onError(RetryContext ctx, RetryCallback<T, E> callback, Throwable t) {
        int retryCount = ctx.getRetryCount();
        if (retryCount == 1 || retryCount % 5 == 0) {
          log.warn("Retryable method '{}' threw {}th exception with message: {}",
            ctx.getAttribute(RetryContext.NAME), retryCount, t.toString());
        }
      }
    };
  }

  @Bean(name = "capabilityRetryTemplate")
  public RetryTemplate capabilityRetryTemplate(
    @Qualifier("methodLoggingRetryListener") RetryListener retryListener, CapabilitiesRetryConfiguration config) {
    return new RetryTemplateBuilder().maxAttempts(config.getRetryAttempts())
      .fixedBackoff(config.getRetryDelay().toMillis())
      .withListener(retryListener)
      .retryOn(List.of(FeignException.class, UnresolvedPermissionsException.class))
      .build();
  }
}
