package org.folio.uk.integration.inventory;

import java.util.Optional;
import java.util.UUID;
import org.folio.uk.domain.dto.ServicePoint;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Client for service points API in mod-inventory.
 */
@FeignClient(value = "service-points", dismiss404 = true)
public interface ServicePointsClient {

  @GetMapping("/{servicePointId}")
  Optional<ServicePoint> getServicePoint(@PathVariable UUID servicePointId);
}
