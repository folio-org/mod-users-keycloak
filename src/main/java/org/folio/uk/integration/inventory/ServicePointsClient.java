package org.folio.uk.integration.inventory;

import java.util.Optional;
import java.util.UUID;
import org.folio.uk.domain.dto.ServicePoint;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

/**
 * Client for service points API in mod-inventory.
 */
@HttpExchange(url = "service-points")
public interface ServicePointsClient {

  @GetExchange("/{servicePointId}")
  Optional<ServicePoint> getServicePoint(@PathVariable UUID servicePointId);
}
