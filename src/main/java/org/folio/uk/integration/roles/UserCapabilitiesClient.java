package org.folio.uk.integration.roles;

import java.util.Optional;
import java.util.UUID;
import org.folio.uk.domain.dto.UserCapabilitiesRequest;
import org.folio.uk.integration.roles.model.CollectionResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "user-capabilities-client", url = "users", dismiss404 = true)
public interface UserCapabilitiesClient {

  @PutMapping("/{userId}/capabilities")
  void assignUserCapabilities(@PathVariable("userId") UUID userId, @RequestBody UserCapabilitiesRequest request);

  @DeleteMapping("/{userId}/capabilities")
  void deleteUserCapabilities(@PathVariable("userId") UUID userId);

  @GetMapping("/{userId}/capabilities")
  Optional<CollectionResponse> findUserCapabilities(@PathVariable("userId") UUID userId);
}
