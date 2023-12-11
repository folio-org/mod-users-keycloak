package org.folio.uk.integration.kafka.configuration;

import java.util.Map;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Application properties for kafka message consumer.
 */
@Data
@Component
@ConfigurationProperties("application.kafka")
public class FolioKafkaProperties {

  /**
   * Map with settings for application kafka listeners.
   */
  private Map<String, KafkaListenerProperties> listener;

  @Data
  public static class KafkaListenerProperties {

    private String topicPattern;
    private String groupId;
  }
}
