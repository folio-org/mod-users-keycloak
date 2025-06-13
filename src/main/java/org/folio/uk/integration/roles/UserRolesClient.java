package org.folio.uk.integration.roles;

import java.util.Optional;
import java.util.UUID;
import org.folio.uk.integration.roles.model.CollectionResponse;
import org.folio.uk.integration.roles.model.UserRoles;
import org.folio.uk.integration.roles.model.UserRolesResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "roles", dismiss404 = true)
public interface UserRolesClient {

  @DeleteMapping("/users/{id}")
  void deleteUserRoles(@PathVariable("id") UUID id);

  @GetMapping("/users/{id}")
  Optional<CollectionResponse> findUserRoles(@PathVariable("id") UUID id);

  @PostMapping("/users")
  UserRolesResponse assignRoleToUser(@RequestBody UserRoles userRoles);
}
