package org.folio.uk.integration.roles;

import java.util.Optional;
import java.util.UUID;
import org.folio.uk.integration.roles.model.CollectionResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "user-capabilities-set-client", url = "users", dismiss404 = true)
public interface UserCapabilitySetClient {

  @DeleteMapping("/{id}/capability-sets")
  void deleteUserCapabilitySet(@PathVariable("id") UUID id);

  @GetMapping("/{id}/capability-sets")
  Optional<CollectionResponse> findUserCapabilitySet(@PathVariable("id") UUID id);
}
