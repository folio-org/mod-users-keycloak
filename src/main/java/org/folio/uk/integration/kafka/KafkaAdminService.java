package org.folio.uk.integration.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.stereotype.Service;

@Log4j2
@Service
@RequiredArgsConstructor
public class KafkaAdminService {

  private final KafkaListenerEndpointRegistry kafkaListenerEndpointRegistry;

  /**
   * Restarts kafka event listeners in the application.
   */
  public void restartEventListeners() {
    kafkaListenerEndpointRegistry.getAllListenerContainers().forEach(container -> {
        log.info("Restarting kafka consumer to start listening created topics [ids: {}]", container.getListenerId());

        container.stop();
        container.start();
      }
    );
  }
}
