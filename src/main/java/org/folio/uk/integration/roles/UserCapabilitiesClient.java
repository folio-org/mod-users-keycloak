package org.folio.uk.integration.roles;

import java.util.Optional;
import java.util.UUID;
import org.folio.uk.domain.dto.UserCapabilitiesRequest;
import org.folio.uk.integration.roles.model.CollectionResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.DeleteExchange;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PutExchange;

@HttpExchange(url = "users")
public interface UserCapabilitiesClient {

  @PutExchange("/{userId}/capabilities")
  void assignUserCapabilities(@PathVariable("userId") UUID userId, @RequestBody UserCapabilitiesRequest request);

  @DeleteExchange("/{userId}/capabilities")
  void deleteUserCapabilities(@PathVariable("userId") UUID userId);

  @GetExchange("/{userId}/capabilities")
  Optional<CollectionResponse> findUserCapabilities(@PathVariable("userId") UUID userId);
}
