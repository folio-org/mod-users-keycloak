package org.folio.uk.integration.roles;

import java.util.UUID;
import org.folio.uk.integration.roles.model.CollectionResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "roles")
public interface UserRolesClient {

  @DeleteMapping("/users/{id}")
  void deleteUserRoles(@PathVariable("id") UUID id);
  
  @GetMapping("/users/{id}")
  CollectionResponse findUserRoles(@PathVariable("id") UUID id);
}
