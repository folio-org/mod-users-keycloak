package org.folio.uk.integration.roles;

import org.folio.uk.domain.dto.Capabilities;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "capabilities")
public interface CapabilitiesClient {

  @GetMapping
  Capabilities queryCapabilities(@RequestParam String query, @RequestParam Integer limit);
}
