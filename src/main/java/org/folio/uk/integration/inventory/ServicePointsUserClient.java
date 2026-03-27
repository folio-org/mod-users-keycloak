package org.folio.uk.integration.inventory;

import java.util.UUID;
import org.folio.uk.integration.inventory.model.ServicePointUserCollection;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

/**
 * Client for service points user API in mod-inventory.
 */
@HttpExchange(url = "service-points-users")
public interface ServicePointsUserClient {

  default ServicePointUserCollection getServicePointsUser(UUID userId) {
    return queryServicePointUsers("userId==" + userId, 1);
  }

  @GetExchange
  ServicePointUserCollection queryServicePointUsers(@RequestParam("query") String query,
    @RequestParam("limit") Integer limit);
}
