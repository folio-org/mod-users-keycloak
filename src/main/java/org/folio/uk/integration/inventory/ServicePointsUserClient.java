package org.folio.uk.integration.inventory;

import java.util.UUID;
import org.folio.uk.integration.inventory.model.ServicePointUserCollection;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Client for service points user API in mod-inventory.
 */
@FeignClient(value = "service-points-users")
public interface ServicePointsUserClient {

  @GetMapping("?query=userId=={userId}&limit=1")
  ServicePointUserCollection getServicePointsUser(@PathVariable UUID userId);
}
