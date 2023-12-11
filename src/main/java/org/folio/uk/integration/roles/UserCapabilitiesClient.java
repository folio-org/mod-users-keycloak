package org.folio.uk.integration.roles;

import java.util.UUID;
import org.folio.uk.domain.dto.UserCapabilitiesRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "user-capabilities-client", url = "users")
public interface UserCapabilitiesClient {

  @PutMapping("/{userId}/capabilities")
  void assignUserCapabilities(@PathVariable("userId") UUID userId, @RequestBody UserCapabilitiesRequest request);

  @DeleteMapping("/{userId}/capabilities")
  void deleteUserCapabilities(@PathVariable("userId") UUID userId);
}
