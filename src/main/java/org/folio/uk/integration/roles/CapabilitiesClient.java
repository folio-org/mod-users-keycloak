package org.folio.uk.integration.roles;

import org.folio.uk.domain.dto.Capabilities;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

@HttpExchange(url = "capabilities")
public interface CapabilitiesClient {

  @GetExchange
  Capabilities queryCapabilities(@RequestParam String query, @RequestParam Integer limit);
}
