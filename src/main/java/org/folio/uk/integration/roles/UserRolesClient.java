package org.folio.uk.integration.roles;

import java.util.Optional;
import java.util.UUID;
import org.folio.uk.integration.roles.model.CollectionResponse;
import org.folio.uk.integration.roles.model.UserRoles;
import org.folio.uk.integration.roles.model.UserRolesResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.DeleteExchange;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

@HttpExchange(url = "roles")
public interface UserRolesClient {

  @DeleteExchange("/users/{id}")
  void deleteUserRoles(@PathVariable("id") UUID id);

  @GetExchange("/users/{id}")
  Optional<CollectionResponse> findUserRoles(@PathVariable("id") UUID id);

  @PostExchange("/users")
  UserRolesResponse assignRoleToUser(@RequestBody UserRoles userRoles);
}
