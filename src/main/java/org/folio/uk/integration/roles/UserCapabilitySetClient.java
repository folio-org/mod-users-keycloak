package org.folio.uk.integration.roles;

import java.util.Optional;
import java.util.UUID;
import org.folio.uk.integration.roles.model.CollectionResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.service.annotation.DeleteExchange;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

@HttpExchange(url = "users")
public interface UserCapabilitySetClient {

  @DeleteExchange("/{id}/capability-sets")
  void deleteUserCapabilitySet(@PathVariable("id") UUID id);

  @GetExchange("/{id}/capability-sets")
  Optional<CollectionResponse> findUserCapabilitySet(@PathVariable("id") UUID id);
}
