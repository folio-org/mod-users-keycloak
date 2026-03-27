package org.folio.uk.integration.kafka.configuration;

import static org.assertj.core.api.Assertions.assertThat;

import org.apache.kafka.common.serialization.StringDeserializer;
import org.folio.test.types.UnitTest;
import org.folio.uk.integration.kafka.model.ResourceEvent;
import org.junit.jupiter.api.Test;
import org.springframework.boot.kafka.autoconfigure.KafkaProperties;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JacksonJsonDeserializer;

@UnitTest
class KafkaConfigurationTest {

  @Test
  void jsonNodeConsumerFactory_positive_usesJacksonJsonDeserializerForValues() {
    var config = new KafkaConfiguration(new KafkaProperties(), retryConfiguration());

    var factory = (DefaultKafkaConsumerFactory<String, ResourceEvent>) config.jsonNodeConsumerFactory();

    assertThat(factory.getKeyDeserializer()).isInstanceOf(StringDeserializer.class);
    assertThat(factory.getValueDeserializer()).isInstanceOf(JacksonJsonDeserializer.class);
  }

  private static SystemUserEventRetryConfiguration retryConfiguration() {
    return new SystemUserEventRetryConfiguration();
  }
}
